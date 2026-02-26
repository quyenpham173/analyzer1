package kr.co.weeds.analyzer1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import kr.co.weeds.analyzer1.constant.StringConstant;
import kr.co.weeds.analyzer1.entity.h2.LogError;
import kr.co.weeds.analyzer1.entity.postgres.AnalyzeValue;
import kr.co.weeds.analyzer1.model.DocumentModel;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.repository.postgres.AnalyzeValueRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoSqlService {

   private static final Logger LOGGER = LogManager.getLogger(NoSqlService.class);

   private static final String DOC_ID = "_id";

   private static final String LOG_NUM = "logNum";

   private static final String CREATED_TIME = "createdTime";

   private static final String START_TIME = "startTime";

//   private static final String[] EXCLUDES = new String[]{"threadId", "endTime", "serverIp", "serverPort", "contextPath",
//       "method", "streamInfo"};

   private static final String[] INCLUDES = new String[]{"logId", LOG_NUM, START_TIME, CREATED_TIME, "clientIp",
       "url", "traceId", "parentTraceId", "additionalData1", "additionalData2", "code", "listHeaders1", "listHeaders2",
       "listCookies1", "listCookies2", "listSessions1", "listSessions2", "listParameters", "sqlLogList.sql",
       "sqlLogList.listParameters"};

   private final RestHighLevelClient clientSource;

   private final ObjectMapper mapper;

   private final AnalyzeValueRepository analyzeValueRepository;

   private final ErrorBackupService errorBackupService;

   @Value("${lh-server.query.size:1000}")
   private Integer limit;

   @Value("${lh-server.repo.index-prefix:}")
   private String index;

   public List<DocumentModel<LogHouseModel>> getDocumentByIds(List<String> documentIds, boolean isReAnalyze) {
      List<DocumentModel<LogHouseModel>> results = new ArrayList<>();
      int batchSize = 500;
      List<List<String>> batches = new ArrayList<>();
      List<LogError> failedLogs = new ArrayList<>();
      Date lastRun = new Date();
      for (int i = 0; i < documentIds.size(); i += batchSize) {
         int end = Math.min(i + batchSize, documentIds.size());
         batches.add(new ArrayList<>(documentIds.subList(i, end)));
      }
      for (List<String> batch : batches) {
         try {
            SearchRequest searchRequest = new SearchRequest(index + "*");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(
                QueryBuilders.idsQuery().addIds(batch.toArray(new String[0]))).size(batch.size());
            sourceBuilder.fetchSource(INCLUDES, null);
            searchRequest.source(sourceBuilder);

            SearchResponse response = clientSource.search(searchRequest, RequestOptions.DEFAULT);
            List<DocumentModel<LogHouseModel>> documentModels = parseSearchResponse(response);
            results.addAll(documentModels);
         } catch (IOException e) {
            String message = e.getMessage();
            LOGGER.error("Error when get document for list id: {}", String.join(", ", batch));
            LOGGER.error("Error occurs when getDocumentByIds.", e);
            batch.forEach(id -> {
               LogError logError = new LogError();
               logError.setDocumentId(id);
               logError.setLastRun(lastRun);
               logError.setMessage(message);
               failedLogs.add(logError);
            });
         }
      }
      if (isReAnalyze) {
         Set<String> foundIds = results.stream().map(DocumentModel::getDocId).collect(Collectors.toSet());
         documentIds.stream().filter(id -> !foundIds.contains(id)).forEach(id -> {
            LogError logError = new LogError();
            logError.setDocumentId(id);
            logError.setLastRun(new Date());
            logError.setMessage("No data in LH repository");
            failedLogs.add(logError);
         });
         errorBackupService.backupNewError(failedLogs);
      }
      return results;
   }

   public List<DocumentModel<LogHouseModel>> getAnalyzeLogs(AnalyzeValue analyzeValue) {
      List<DocumentModel<LogHouseModel>> result;
      List<AnalyzeValue> analyzeValueList = analyzeValueRepository.findByAppId(StringConstant.APP_ID);
      String lastDocId = StringUtils.EMPTY;
      if (CollectionUtils.isEmpty(analyzeValueList)) {
         result = getAnalyzeLogsFirstTime();
         if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
         }
      } else {
         AnalyzeValue previousAnalyze = analyzeValueList.get(0);
         lastDocId = previousAnalyze.getDocId();
         analyzeValue.setCreatedTime(previousAnalyze.getCreatedTime());
         analyzeValue.setId(previousAnalyze.getId());
         result = queryLhDocuments(lastDocId, analyzeValue.getCreatedTime());
      }
      if (CollectionUtils.isNotEmpty(result)) {
         LOGGER.info("Total document results: {}", result.size());
         DocumentModel<LogHouseModel> lastDocument = result.get(result.size() - 1);
         analyzeValue.setDocId(lastDocument.getDocId());
         Date createdDateTime = Date.from(
             lastDocument.getSource().getCreatedTime().atZone(ZoneId.systemDefault()).toInstant());
         String createdTime = new SimpleDateFormat(StringConstant.OS_DATE_FORMAT).format(createdDateTime);
         analyzeValue.setCreatedTime(createdTime);
      } else {
         analyzeValue.setDocId(lastDocId);
      }
      analyzeValue.setAppId(StringConstant.APP_ID);
      analyzeValue.setAnalysisTime(new Date());
      return result;
   }

   public DocumentModel<LogHouseModel> getDocumentById(String documentId) {
      SearchRequest request = new SearchRequest(index + "*");
      SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
      sourceBuilder.query(QueryBuilders.termQuery(DOC_ID, documentId));
      sourceBuilder.fetchSource(INCLUDES, null);
      request.source(sourceBuilder);
      try {
         SearchResponse response = clientSource.search(request, RequestOptions.DEFAULT);
         List<DocumentModel<LogHouseModel>> documents = parseSearchResponse(response);
         if (CollectionUtils.isNotEmpty(documents)) {
            return documents.get(0);
         }
      } catch (IOException e) {
         LOGGER.error("Error when get document with index {} and logNum {}", index, documentId);
      }
      return null;
   }

   public List<DocumentModel<LogHouseModel>> getDocumentByDate(String date) {
      List<DocumentModel<LogHouseModel>> results = new ArrayList<>();
      Object[] searchAfterValues = null;
      try {
         while (true) {
            SearchRequest request = new SearchRequest(index + date);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.sort(START_TIME, SortOrder.ASC);
            sourceBuilder.sort(LOG_NUM, SortOrder.ASC);
            sourceBuilder.size(limit);
            sourceBuilder.trackTotalHits(false);
            if (searchAfterValues != null) {
               sourceBuilder.searchAfter(searchAfterValues);
            }
            request.source(sourceBuilder);
            SearchResponse response = clientSource.search(request, RequestOptions.DEFAULT);
            List<DocumentModel<LogHouseModel>> documentModels = parseSearchResponse(response);
            results.addAll(documentModels);
            if (documentModels.isEmpty() || documentModels.size() < limit) {
               break;
            }

            SearchHit[] hits = response.getHits().getHits();
            SearchHit lastHit = hits[hits.length - 1];
            searchAfterValues = lastHit.getSortValues();
         }
      } catch (IOException e) {
         LOGGER.error("Error when build Discover Index.", e);
      }
      return results;
   }

   public List<DocumentModel<LogHouseModel>> getDocumentByDateRange(String fromDate, String toDate) {
      List<DocumentModel<LogHouseModel>> results = new ArrayList<>();
      Object[] searchAfterValues = null;
      try {
         while (true) {
            SearchRequest request = new SearchRequest(index + "*");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.sort(START_TIME, SortOrder.ASC);
            sourceBuilder.sort(LOG_NUM, SortOrder.ASC);
            sourceBuilder.size(limit);
            sourceBuilder.trackTotalHits(false);
            if (searchAfterValues != null) {
               sourceBuilder.searchAfter(searchAfterValues);
            }
            RangeQueryBuilder rangeQuery = QueryBuilders
                .rangeQuery(START_TIME)
                .gte(fromDate + " 00:00:00.000")
                .lt(toDate + " 00:00:00.000");
            sourceBuilder.query(rangeQuery);
            request.source(sourceBuilder);
            SearchResponse response = clientSource.search(request, RequestOptions.DEFAULT);
            List<DocumentModel<LogHouseModel>> documentModels = parseSearchResponse(response);

            if (documentModels.isEmpty() || documentModels.size() < limit) {
               break;
            }
            results.addAll(documentModels);

            SearchHit[] hits = response.getHits().getHits();
            SearchHit lastHit = hits[hits.length - 1];
            searchAfterValues = lastHit.getSortValues();
         }
      } catch (IOException e) {
         LOGGER.error("Error when build Discover Index.", e);
      }
      return results;
   }

   private List<DocumentModel<LogHouseModel>> queryLhDocuments(String logNum, String createdTime) {
      LOGGER.info("Query filter: logNum {} and createdTime {}", logNum, createdTime);
      SearchRequest request = getSearchRequest(logNum, createdTime);
      try {
         SearchResponse response = clientSource.search(request, RequestOptions.DEFAULT);
         return parseSearchResponse(response);
      } catch (IOException e) {
         LOGGER.error("Error when get list analysis log.", e);
         return null;
      }
   }

   private List<DocumentModel<LogHouseModel>> getAnalyzeLogsFirstTime() {
      List<DocumentModel<LogHouseModel>> results = new ArrayList<>();
      // Query by time range
      LOGGER.info("Getting analysis log for first analyze.");
      SearchRequest request = new SearchRequest(index + "*");
      SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
      sourceBuilder.fetchSource(INCLUDES, null);
      sourceBuilder.sort(CREATED_TIME, SortOrder.ASC);
      sourceBuilder.sort(LOG_NUM, SortOrder.ASC);
      sourceBuilder.size(limit);
      sourceBuilder.trackTotalHits(false);
      request.source(sourceBuilder);
      try {
         SearchResponse response = clientSource.search(request, RequestOptions.DEFAULT);
         return parseSearchResponse(response);
      } catch (IOException e) {
         LOGGER.error("Error when get list analysis log first time.", e);
      }
      return results;
   }

   private SearchRequest getSearchRequest(String logNum, String createdTime) {
      SearchRequest request = new SearchRequest(index + "*");
      SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

      // filter: createdTime <= now - 1s
      String endTime = DateTimeFormatter.ofPattern(StringConstant.OS_DATE_FORMAT)
          .format(LocalDateTime.now().minusSeconds(1));
      RangeQueryBuilder filterCreatedTime = QueryBuilders.rangeQuery(CREATED_TIME).lte(endTime);

      // should part 1: createdTime > startTime
      RangeQueryBuilder shouldCreatedTime = QueryBuilders.rangeQuery(CREATED_TIME).gt(createdTime);

      // should part 2: createdTime = startTime and logNum > lastLogNum
      RangeQueryBuilder createdTimeEquals = QueryBuilders.rangeQuery(CREATED_TIME).gte(createdTime).lte(createdTime);
      RangeQueryBuilder logNumGt = QueryBuilders.rangeQuery(LOG_NUM).gt(logNum);
      BoolQueryBuilder innerMust = QueryBuilders.boolQuery().must(createdTimeEquals).must(logNumGt);

      // should match minimum
      BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
          .filter(filterCreatedTime)
          .should(shouldCreatedTime)
          .should(innerMust)
          .minimumShouldMatch(1);

      sourceBuilder.query(boolQuery);
      sourceBuilder.fetchSource(INCLUDES, null);
      sourceBuilder.sort(CREATED_TIME, SortOrder.ASC);
      sourceBuilder.sort(LOG_NUM, SortOrder.ASC);
      sourceBuilder.size(limit);
      sourceBuilder.trackTotalHits(false);
      request.source(sourceBuilder);
      return request;
   }

   private List<DocumentModel<LogHouseModel>> parseSearchResponse(SearchResponse response) throws IOException {
      SearchHit[] hits = response.getHits().getHits();
      List<DocumentModel<LogHouseModel>> documents = new ArrayList<>(hits.length);
      for (SearchHit hit : hits) {
         LogHouseModel model = mapper.readValue(hit.getSourceAsString(), LogHouseModel.class);
         DocumentModel<LogHouseModel> doc = new DocumentModel<>();
         doc.setDocId(hit.getId());
         doc.setIndex(hit.getIndex());
         doc.setSource(model);
         documents.add(doc);
      }
      return documents;
   }

}
