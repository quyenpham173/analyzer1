package kr.co.weeds.analyzer1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import kr.co.weeds.analyzer1.constant.StringConstant;
import kr.co.weeds.analyzer1.entity.h2.LogError;
import kr.co.weeds.analyzer1.model.DocumentModel;
import kr.co.weeds.analyzer1.model.ana1.LogRegisterModel;
import kr.co.weeds.analyzer1.repository.h2.LogErrorRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.admin.cluster.node.stats.NodeStats;
import org.opensearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.opensearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogRegisterService {

   private static final Logger LOGGER = LogManager.getLogger(LogRegisterService.class);

   @Value("${os-server.bulk.timeout:5}")
   private Integer timeout;

   @Value("${os-server.repo.index-prefix:}")
   private String index;

   @Value("${log-analyze.max-retry:5}")
   private Integer maxRetry;

   @Value("${os-server.bulk.enabled:true}")
   private Boolean bulkEnabled;

   private final Map<String, LogError> logErrorMap = new ConcurrentHashMap<>();

   private BulkRequest bulkRequest = new BulkRequest();

   private LinkedBlockingQueue<DocumentModel<LogRegisterModel>> queue = new LinkedBlockingQueue<>();

   private final RestHighLevelClient clientDest;

   private final LogErrorRepository logErrorRepository;

   private final ErrorBackupService errorBackupService;

   private final LogErrorService logErrorService;

   private final ObjectMapper mapper;

   public void processResult(DocumentModel<LogRegisterModel> data) {
      if (Boolean.TRUE.equals(bulkEnabled)) {
         queue.add(data);
      } else {
         singleRegisterLog(data);
      }
   }

   public void bulkRegisterLog() {
      if(queue.isEmpty()) {
         return;
      }
      LOGGER.info("Start insert analysis result to repo. Queue size: {}", queue.size());
      List<LogError> logErrors = new ArrayList<>(); // Used to store all logs to H2DB for case exception
      long esTime = getNodeTimestamp(clientDest);
      long offset = calculateTimeOffset(esTime);
      try {
         while (!queue.isEmpty()) {
            DocumentModel<LogRegisterModel> data = queue.poll();
            Optional<LogError> logErrorOpt = logErrorRepository.findById(data.getDocId());
            LogError logError;
            if (logErrorOpt.isPresent()) {
               logError = logErrorOpt.get();
               logErrorMap.put(data.getDocId(), logError);
            } else {
               logError = new LogError();
               logError.setDocumentId(data.getDocId());
               logError.setRetryCount(-1);
            }
            logError.setLastRun(new Date());
            logErrors.add(logError);
            LocalDateTime createdTime = getActualEsTime(offset, System.currentTimeMillis(), StringConstant.OS_DATE_FORMAT, ZoneId.of("UTC"));
            data.getSource().setCreatedTime(createdTime);
            IndexRequest indexRequest = getIndexRequest(data);

            if (indexRequest != null) {
               bulkRequest.add(indexRequest);
            }
         }
         bulkRequest.timeout(TimeValue.timeValueMinutes(timeout));
         registerLogData();
         queue = new LinkedBlockingQueue<>();
         LOGGER.info("Analysis result insert process completed.");
      } catch (Exception e) {
         LOGGER.error("Error when insert data to ES by Bulk.", e);
         storeLogErrors(logErrors, e.getMessage());
      }
   }

   /**
    * Store each log analysis result by IndexRequest
    *
    * @param document Analysis result
    */
   public void singleRegisterLog(DocumentModel<LogRegisterModel> document) {
      long esTime = getNodeTimestamp(clientDest);
      long offset = calculateTimeOffset(esTime);
      LocalDateTime createdTime = getActualEsTime(offset, System.currentTimeMillis(), StringConstant.OS_DATE_FORMAT, ZoneId.of("UTC"));
      document.getSource().setCreatedTime(createdTime);
      IndexRequest indexRequest = getIndexRequest(document);
      if (indexRequest == null) {
         return;
      }

      Optional<LogError> logErrorOpt = logErrorRepository.findById(document.getDocId());
      try {
         IndexResponse indexResponse = clientDest.index(indexRequest, RequestOptions.DEFAULT);
         LOGGER.info("Log {} was inserted to ES with status: {} ", document.getDocId(), indexResponse.getResult());
         logErrorOpt.ifPresent(logErrorRepository::delete);
      } catch (Exception e) {
         LOGGER.error("Error when insert log {} to OS.", document.getDocId(), e);
         logErrorService.saveLogError(document, e.getMessage());
      }
   }

   public boolean registerLogOnly(DocumentModel<LogRegisterModel> document) {
      IndexRequest indexRequest = getIndexRequest(document);
      if (indexRequest == null) {
         return false;
      }
      try {
         clientDest.index(indexRequest, RequestOptions.DEFAULT);
         return true;
      } catch (IOException e) {
         LOGGER.error("Error when store log re-analysis.", e);
         return false;
      }
   }

   /**
    * Store log data to OS by Bulk API
    *
    * @throws IOException All logs store failed
    */
   private void registerLogData() throws IOException {
      if (bulkRequest.numberOfActions() == 0) {
         return;
      }
      BulkResponse bulkResponse = clientDest.bulk(bulkRequest, RequestOptions.DEFAULT);
      List<LogError> logErrorsDB = new ArrayList<>();
      List<LogError> logErrorsFile = new ArrayList<>();
      for (BulkItemResponse itemResponse : bulkResponse) {
         DocWriteRequest<?> request = bulkRequest.requests().get(itemResponse.getItemId());
         if (request instanceof IndexRequest indexRequest) {
            String docId = indexRequest.id();
            LogError logError = logErrorMap.get(docId);
            if (itemResponse.isFailed()) {
               String message = itemResponse.getFailureMessage();
               LOGGER.error("Cannot insert document {} to ES by Bulk API: {}", docId, message);
               if (logError != null) {
                  Integer retryCount = logError.getRetryCount();
                  if (retryCount >= maxRetry) {
                     logErrorsFile.add(logError);
                     continue;
                  } else {
                     logError.setRetryCount(retryCount + 1);
                  }
               } else {
                  logError = new LogError();
                  logError.setDocumentId(docId);
                  logError.setRetryCount(0);
               }
               logError.setLastRun(new Date());
               logError.setMessage(message);
               logErrorsDB.add(logError);
            } else {
               if (logError != null) {
                  logErrorRepository.delete(logError);
               }
            }
         }
      }
      errorBackupService.moveLogToErrorFile(logErrorsFile, false);
      logErrorRepository.saveAll(logErrorsDB);
      bulkRequest = new BulkRequest();
      logErrorMap.clear();
   }

   private IndexRequest getIndexRequest(DocumentModel<LogRegisterModel> data) {
      try {
         String source = mapper.writeValueAsString(data.getSource());
         return new IndexRequest(getIndex(data.getSource())).id(data.getDocId())
             .source(source, XContentType.JSON);
      } catch (JsonProcessingException e) {
         LOGGER.error("Error when parse log register to JSON.", e);
         return null;
      }
   }

   private void storeLogErrors(List<LogError> logErrors, String message) {
      List<LogError> logStoreDBList = new ArrayList<>();
      List<LogError> logStoreFileList = new ArrayList<>();
      for (LogError log : logErrors) {
         log.setMessage(message);
         Integer retryCount = log.getRetryCount();
         if (retryCount >= maxRetry) {
            logStoreFileList.add(log);
         } else {
            log.setRetryCount(retryCount + 1);
            logStoreDBList.add(log);
         }
      }
      errorBackupService.moveLogToErrorFile(logStoreFileList, false);
      logErrorRepository.saveAll(logStoreDBList);
   }

   private String getIndex(LogRegisterModel registerModel) {
      if (StringUtils.isBlank(index) || registerModel == null) {
         return StringUtils.EMPTY;
      }
      LocalDateTime startTime = registerModel.getStartTime();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      return index + startTime.format(formatter);
   }

   public long getNodeTimestamp(RestHighLevelClient client) {
      try {
         Request request = new Request("GET", "/_nodes/stats");
         request.addParameter("filter_path", "nodes.*.timestamp");
         Response response = client.getLowLevelClient().performRequest(request);
         try (InputStream is = response.getEntity().getContent()) {
            JsonNode rootNode = mapper.readTree(is);
            JsonNode nodesNode = rootNode.path("nodes");
            if (nodesNode.isObject() && nodesNode.fieldNames().hasNext()) {
               String firstNodeId = nodesNode.fieldNames().next();
               JsonNode firstNode = nodesNode.get(firstNodeId);
               if (firstNode != null && firstNode.has("timestamp")) {
                  return firstNode.get("timestamp").asLong();
               }
            }
         }
      } catch (Exception e) {
         System.err.println("[E] Failed to get system time from NoSQL server: " + e.getMessage());
      }
      return System.currentTimeMillis();
   }

   public static long calculateTimeOffset(long esTimestampMillis) {
      return esTimestampMillis - System.currentTimeMillis();
   }

   public static LocalDateTime getActualEsTime(long offsetMillis, long currentBackendTimeMillis,
       String formatPattern, ZoneId targetZone) {
      long actualEsTimeMillis = currentBackendTimeMillis + offsetMillis;
      ZoneId zoneToApply = (targetZone != null) ? targetZone : ZoneId.systemDefault();
      return Instant.ofEpochMilli(actualEsTimeMillis)
          .atZone(zoneToApply)
          .toLocalDateTime();
   }
}
