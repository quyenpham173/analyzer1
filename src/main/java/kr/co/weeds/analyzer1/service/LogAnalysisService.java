package kr.co.weeds.analyzer1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import kr.co.weeds.analyzer1.constant.StringConstant;
import kr.co.weeds.analyzer1.dto.LogErrorFileDTO;
import kr.co.weeds.analyzer1.dto.ResponseDTO;
import kr.co.weeds.analyzer1.entity.h2.LogError;
import kr.co.weeds.analyzer1.entity.postgres.TraceAnalyzeConfig;
import kr.co.weeds.analyzer1.loader.TraceLoader;
import kr.co.weeds.analyzer1.model.DocumentModel;
import kr.co.weeds.analyzer1.model.LoaderInfo;
import kr.co.weeds.analyzer1.model.ana1.LogRegisterModel;
import kr.co.weeds.analyzer1.model.config.LogAnalysisConfig;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.process.LogPreProcess;
import kr.co.weeds.analyzer1.process.UserInfoProcess;
import kr.co.weeds.analyzer1.repository.h2.LogErrorRepository;
import kr.co.weeds.analyzer1.repository.postgres.TraceConfigRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.ResourceNotFoundException;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import kr.co.weeds.dtos.NameValueObject;

@Service
@RequiredArgsConstructor
public class LogAnalysisService {

   private static final Logger LOGGER = LogManager.getLogger(LogAnalysisService.class);

   private final LogPreProcess logPreProcess;

   private final UserInfoProcess userInfoProcess;

   private final NoSqlService noSqlService;

   private final TraceConfigRepository traceConfigRepository;

   private final LogErrorRepository logErrorRepository;

   private final TraceLoader traceLoader;

   private final LogRegisterService logRegisterService;

   private final LogErrorService logErrorService;

   private final ErrorBackupService errorBackupService;

   private final ObjectMapper mapper;

   private final RestHighLevelClient clientDest;
   private final AtomicLong analyzeCounter = new AtomicLong(0);

   @Value("${log-analyze.max-retry:5}")
   private Integer maxRetry;

   @Value("${os-server.repo.index-prefix:}")
   private String index;

   @Value("${os-server.bulk.timeout:5}")
   private Integer timeout;

   public DocumentModel<LogRegisterModel> testAnalyze(String documentId) {
      LOGGER.info("Start running test analyze for document {}", documentId);
      DocumentModel<LogHouseModel> documentData = noSqlService.getDocumentById(documentId);
      if (documentData != null) {
         LogHouseModel analysisModel = documentData.getSource();
         Optional<TraceAnalyzeConfig> configOptional = traceConfigRepository.findByTraceIdAndAppId(
             analysisModel.getTraceId(), StringConstant.APP_ID);
         if (configOptional.isPresent()) {
            LogAnalysisConfig logAnalysisConfig = LogAnalysisConfig.loadConfig(configOptional.get());
            LoaderInfo loaderInfo = traceLoader.getLoaderInfo();
            return analysisLog(documentData, logAnalysisConfig, loaderInfo);
         }
         LOGGER.error("Trace configuration not found: {}", analysisModel.getTraceId());
      }
      throw new ResourceNotFoundException(
          String.format("Cannot get log data for document %s", documentId));
   }

   public ResponseDTO manualAnalyze(List<String> docIds) {
      LOGGER.info("Start manual analyze for log ids: {}", String.join(", ", docIds));
      ResponseDTO response = new ResponseDTO();
      try {
         List<DocumentModel<LogHouseModel>> lhDocuments = noSqlService.getDocumentByIds(docIds, false);
         if (CollectionUtils.isNotEmpty(lhDocuments)) {
            List<String> listTraceId = lhDocuments.stream()
                .map(DocumentModel::getSource)
                .map(LogHouseModel::getTraceId)
                .distinct()
                .toList();
            List<TraceAnalyzeConfig> traceAnalyzeConfigs = traceConfigRepository.findByTraceIdInAndAppId(listTraceId,
                StringConstant.APP_ID);
            if (CollectionUtils.isNotEmpty(traceAnalyzeConfigs)) {
               LoaderInfo loaderInfo = traceLoader.getLoaderInfo();
               Map<String, LogAnalysisConfig> traceConfigMap = new HashMap<>();
               for (TraceAnalyzeConfig traceAnalyzeConfig : traceAnalyzeConfigs) {
                  LogAnalysisConfig logAnalysisConfig = LogAnalysisConfig.loadConfig(traceAnalyzeConfig);
                  if (logAnalysisConfig != null) {
                     traceConfigMap.put(traceAnalyzeConfig.getTraceId(), logAnalysisConfig);
                  }
               }
               BulkRequest bulkRequest = new BulkRequest();
               for (DocumentModel<LogHouseModel> doc : lhDocuments) {
                  String traceId = doc.getSource().getTraceId();
                  LogAnalysisConfig logAnalysisConfig = traceConfigMap.get(traceId);
                  if (logAnalysisConfig == null) {
                     String message = String.format(
                         "Trace %s does not have configuration. Cannot run analyze for logs of this trace", traceId);
                     LOGGER.error(message);
                  } else {
                     try {
                        DocumentModel<LogRegisterModel> documentModel = analysisLog(doc, logAnalysisConfig, loaderInfo);
                        if (documentModel != null) {
                           LogRegisterModel analyzeResult = documentModel.getSource();
                           try {
                              String source = mapper.writeValueAsString(analyzeResult);
                              bulkRequest.add(new IndexRequest(getIndex(analyzeResult)).id(documentModel.getDocId())
                                  .source(source, XContentType.JSON));
                           } catch (JsonProcessingException e) {
                              LOGGER.error("Analyze by document IDs: Error when convert result to JSON.", e);
                           }
                        }
                     } catch (Exception e) {
                        LOGGER.error("Error when re-analyze log.", e);
                     }
                  }
               }
               if (bulkRequest.numberOfActions() != 0) {
                  BulkResponse bulkResponse = clientDest.bulk(bulkRequest, RequestOptions.DEFAULT);
                  List<String> successList = new ArrayList<>();
                  for (BulkItemResponse itemResponse : bulkResponse) {
                     DocWriteRequest<?> request = bulkRequest.requests().get(itemResponse.getItemId());
                     if (request instanceof IndexRequest indexRequest) {
                        String docId = indexRequest.id();
                        if (itemResponse.isFailed()) {
                           String message = itemResponse.getFailureMessage();
                           LOGGER.error("Manual analyze: Cannot insert document {} to ES by Bulk API: {}", docId,
                               message);
                        } else {
                           successList.add(itemResponse.getId());
                        }
                     }
                  }
                  response.setCode(HttpStatus.OK.value());
                  response.setMessage("Run manual analyze successfully!");
                  response.setData(successList);
               }
            } else {
               String traceIds = String.join(", ", listTraceId);
               response.setCode(HttpStatus.NOT_FOUND.value());
               response.setMessage(
                   "Trace configuration empty. Cannot run manual analyze. Trace list: " + String.join(",", traceIds));
            }
            return response;
         }
         response.setCode(HttpStatus.BAD_REQUEST.value());
         response.setMessage("Cannot get LH documents for list ID params!");
      } catch (Exception e) {
         response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
         response.setMessage(e.getMessage());
      }
      return response;
   }

   public void startAnalysisThread(DocumentModel<LogHouseModel> document, LogAnalysisConfig logAnalysisConfig,
       LoaderInfo loaderInfo) {
      try {
         DocumentModel<LogRegisterModel> documentModel = analysisLog(document, logAnalysisConfig, loaderInfo);
         analyzeCounter.incrementAndGet();
         if (documentModel != null) {
            LOGGER.info("Index {}, document ID {} analyze completed. Moving result to queue.", document.getIndex(),
                document.getDocId());
            logRegisterService.processResult(documentModel);
         }
      } catch (Exception e) {
         LOGGER.error("Error when analysis log {}: ", document.getDocId(), e);
         logErrorService.saveLogError(document, e.getMessage());
      }
   }

   public DocumentModel<LogRegisterModel> analysisLog(DocumentModel<LogHouseModel> document,
       LogAnalysisConfig logAnalysisConfig, LoaderInfo loaderInfo) {
      LOGGER.info("Start analysis for log data: Index {}, document ID {}", document.getIndex(), document.getDocId());
      LogHouseModel analysisModel = document.getSource();
      logPreProcess.remodel(analysisModel, logAnalysisConfig);
//      logPreProcess.setSessionId(analysisModel, logAnalysisConfig);
      if (analysisModel.getStartTime() == null) {
         LOGGER.error("Document {} does not have startTime value. Skip log.", analysisModel.getLogNum());
         return null;
      }
      if (hasExceptionParamValue(analysisModel, logAnalysisConfig)) {
         LOGGER.error("Has exception for param value. Skip document {}", document.getDocId());
         return null;
      }
      LogRegisterModel registerModel = userInfoProcess.analyze(analysisModel, logAnalysisConfig, loaderInfo);
      if (registerModel == null) {
         LOGGER.error("There is no analyze result for document {}", analysisModel.getLogNum());
         return null;
      }
      DocumentModel<LogRegisterModel> registerDoc = new DocumentModel<>();
      registerDoc.setDocId(document.getDocId());
      registerDoc.setIndex(document.getIndex());
      registerDoc.setSource(registerModel);
      return registerDoc;
   }

   public ResponseDTO reAnalysis(List<String> errorDocIds, int from, int size) {
      LOGGER.info("Start re-analyze failed logs via API");
      ResponseDTO response = new ResponseDTO();
      try {
         List<LogErrorFileDTO> listErrorLogs = logErrorService.getAndRemove(errorDocIds, from, size);
         if (CollectionUtils.isEmpty(listErrorLogs)) {
            response.setCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage("No document id found in error backup file.");
            return response;
         }
         List<String> docIds = listErrorLogs.stream().map(LogErrorFileDTO::getDocumentID).toList();
         List<DocumentModel<LogHouseModel>> documentList = noSqlService.getDocumentByIds(docIds, true);
         analyzeListLogs(documentList, response, true);
      } catch (Exception e) {
         response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
         response.setMessage(e.getMessage());
      }
      return response;
   }

   public ResponseDTO reAnalysisByDate(String fromDate, String toDate) {
      List<DocumentModel<LogHouseModel>> documentList;
      ResponseDTO response = new ResponseDTO();
      if (StringUtils.isEmpty(toDate)) {
         documentList = noSqlService.getDocumentByDate(fromDate);
      } else {
         documentList = noSqlService.getDocumentByDateRange(fromDate, toDate);
      }
      analyzeListLogs(documentList, response, false);
      return response;
   }

   private void analyzeListLogs(List<DocumentModel<LogHouseModel>> documents, ResponseDTO response, boolean reAnalyze) {
      List<LogError> logErrors = new ArrayList<>();
      try {
         if (CollectionUtils.isNotEmpty(documents)) {
            List<String> listTraceId = documents.stream()
                .map(DocumentModel::getSource)
                .map(LogHouseModel::getTraceId)
                .distinct()
                .toList();
            List<TraceAnalyzeConfig> traceAnalyzeConfigs = traceConfigRepository.findByTraceIdInAndAppId(listTraceId,
                StringConstant.APP_ID);
            if (CollectionUtils.isNotEmpty(traceAnalyzeConfigs)) {
               LoaderInfo loaderInfo = traceLoader.getLoaderInfo();
               Map<String, LogAnalysisConfig> traceConfigMap = new HashMap<>();
               for (TraceAnalyzeConfig traceAnalyzeConfig : traceAnalyzeConfigs) {
                  LogAnalysisConfig logAnalysisConfig = LogAnalysisConfig.loadConfig(traceAnalyzeConfig);
                  if (logAnalysisConfig != null) {
                     traceConfigMap.put(traceAnalyzeConfig.getTraceId(), logAnalysisConfig);
                  }
               }
               BulkRequest bulkRequest = new BulkRequest();
               for (DocumentModel<LogHouseModel> doc : documents) {
                  String traceId = doc.getSource().getTraceId();
                  LogAnalysisConfig logAnalysisConfig = traceConfigMap.get(traceId);
                  if (logAnalysisConfig == null) {
                     String message = String.format("Trace configuration empty. Trace ID: %s", traceId);
                     LOGGER.error(message);
                     logErrors.add(buildLogError(doc.getDocId(), message));
                  } else {
                     try {
                        DocumentModel<LogRegisterModel> documentModel = analysisLog(doc, logAnalysisConfig, loaderInfo);
                        if (documentModel != null) {
                           LOGGER.info("Index {}, document ID {} re-analyze completed. Add to queue.",
                               doc.getIndex(), doc.getDocId());
                           // Add request to bulk
                           String source = mapper.writeValueAsString(documentModel.getSource());
                           bulkRequest.add(
                               new IndexRequest(getIndex(documentModel.getSource())).id(documentModel.getDocId())
                                   .source(source, XContentType.JSON));
                        }
                     } catch (Exception e) {
                        LOGGER.error("Error when re-analyze log.", e);
                        logErrors.add(buildLogError(doc.getDocId(), e.getMessage()));
                     }
                  }
               }
               if (bulkRequest.numberOfActions() != 0) {
                  bulkRequest.timeout(TimeValue.timeValueMinutes(timeout));
                  BulkResponse bulkResponse = clientDest.bulk(bulkRequest, RequestOptions.DEFAULT);
                  List<String> successList = new ArrayList<>();
                  for (BulkItemResponse itemResponse : bulkResponse) {
                     DocWriteRequest<?> request = bulkRequest.requests().get(itemResponse.getItemId());
                     if (request instanceof IndexRequest indexRequest) {
                        String docId = indexRequest.id();
                        if (itemResponse.isFailed()) {
                           String message = itemResponse.getFailureMessage();
                           LOGGER.error("Re analyze: Cannot insert document {} to ES by Bulk API: {}", docId, message);
                        } else {
                           successList.add(itemResponse.getId());
                        }
                     }
                  }
                  response.setCode(HttpStatus.OK.value());
                  response.setMessage("Run manual analyze successfully!");
                  response.setData(successList);
               }

            } else {
               documents.forEach(doc -> logErrors.add(buildLogError(doc.getDocId(),
                   "Trace configuration empty. Trace ID: " + doc.getSource().getTraceId())));
               String traceIds = String.join(", ", listTraceId);
               String errMessage = String.format("Trace configuration empty. Trace list: %s", traceIds);
               response.setCode(HttpStatus.BAD_REQUEST.value());
               response.setMessage(errMessage);
            }
         } else {
            response.setCode(HttpStatus.OK.value());
            response.setMessage("No data collected from LH");
         }
      } catch (Exception e) {
         documents.forEach(doc -> logErrors.add(buildLogError(doc.getDocId(), e.getMessage())));
         response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
         response.setMessage(e.getMessage());
      }
      if (reAnalyze) {
         errorBackupService.backupNewError(logErrors);
      } else {
         logErrorRepository.saveAll(logErrors);
      }
   }

   private boolean hasExceptionParamValue(LogHouseModel log, LogAnalysisConfig logAnalysisConfig) {
      List<String> paramValueWhiteList = logAnalysisConfig.getParamValueWhiteList();
      if (CollectionUtils.isEmpty(paramValueWhiteList)) {
         return false;
      }

      List<NameValueObject> paramList = log.getListParameters();
      if (paramList == null) {
         return true;
      }
      for (String checkParam : paramValueWhiteList) {
         for (NameValueObject nvo : paramList) {
            if (nvo == null || nvo.getValue() == null) {
               continue;
            }
            if (nvo.getValue().contains(checkParam)) {
               return false;
            }
         }
      }
      return true;
   }

   private String getIndex(LogRegisterModel registerModel) {
      if (StringUtils.isBlank(index) || registerModel == null) {
         return StringUtils.EMPTY;
      }
      LocalDateTime startTime = registerModel.getStartTime();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      return index + startTime.format(formatter);
   }

   private LogError buildLogError(String docId, String message) {
      LogError logError = new LogError();
      logError.setDocumentId(docId);
      logError.setMessage(message);
      logError.setRetryCount(0);
      logError.setLastRun(new Date());
      return logError;
   }

   public long getThroughput() { return analyzeCounter.get(); }
   public void resetThroughput() { analyzeCounter.set(0); }


}
