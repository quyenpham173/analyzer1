package kr.co.weeds.analyzer1.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import kr.co.weeds.analyzer1.constant.StringConstant;
import kr.co.weeds.analyzer1.entity.h2.LogError;
import kr.co.weeds.analyzer1.entity.postgres.AnalyzeValue;
import kr.co.weeds.analyzer1.entity.postgres.TraceAnalyzeConfig;
import kr.co.weeds.analyzer1.loader.TraceLoader;
import kr.co.weeds.analyzer1.model.DocumentModel;
import kr.co.weeds.analyzer1.model.LoaderInfo;
import kr.co.weeds.analyzer1.model.config.LogAnalysisConfig;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.process.AnalyzeThread;
import kr.co.weeds.analyzer1.repository.h2.LogErrorRepository;
import kr.co.weeds.analyzer1.repository.postgres.AnalyzeValueRepository;
import kr.co.weeds.analyzer1.repository.postgres.TraceConfigRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobService {

   private static final Logger LOGGER = LogManager.getLogger(JobService.class);

   private final LogAnalysisService analysisService;

   private final TraceLoader traceLoader;

   private final Executor taskExecutor;

   private final LogErrorRepository logErrorRepository;

   private final NoSqlService noSqlService;

   private final TraceConfigRepository traceConfigRepository;

   private final LogErrorService logErrorService;

   private final AnalyzeValueRepository analyzeValueRepository;

   private final LogRegisterService logRegisterService;

   @Transactional
   public void runAnalysis() {
      List<DocumentModel<LogHouseModel>> documentList = new ArrayList<>();

      // Get log failed for re-analysis
      List<LogError> logErrors = logErrorRepository.findAll();
      if (CollectionUtils.isNotEmpty(logErrors)) {
         LOGGER.info("Number of error logs in H2DB: {}", logErrors.size());
         List<String> documentIds = logErrors.stream().map(LogError::getDocumentId).toList();
         List<DocumentModel<LogHouseModel>> failedLogs = noSqlService.getDocumentByIds(documentIds, false);
         if (CollectionUtils.isNotEmpty(failedLogs)) {
            documentList.addAll(failedLogs);
         }
      }

      // Get list log data from LH
      AnalyzeValue analyzeValue = new AnalyzeValue();
      List<DocumentModel<LogHouseModel>> analyzeLogs = noSqlService.getAnalyzeLogs(analyzeValue);
      documentList.addAll(analyzeLogs);

      if (CollectionUtils.isNotEmpty(documentList)) {
         LoaderInfo loaderInfo = traceLoader.getLoaderInfo();
         try {
            List<String> listTraceId = documentList.stream()
                .map(DocumentModel::getSource)
                .map(LogHouseModel::getTraceId)
                .distinct()
                .toList();
            List<TraceAnalyzeConfig> traceAnalyzeConfigs = traceConfigRepository.findByTraceIdInAndAppId(listTraceId,
                StringConstant.APP_ID);
            if (CollectionUtils.isNotEmpty(traceAnalyzeConfigs)) {
               CountDownLatch latch = new CountDownLatch(documentList.size());
               Set<String> processedDocs = Collections.synchronizedSet(new HashSet<>());
               Map<String, LogAnalysisConfig> traceConfigMap = new HashMap<>();
               for (TraceAnalyzeConfig traceAnalyzeConfig : traceAnalyzeConfigs) {
                  LogAnalysisConfig logAnalysisConfig = LogAnalysisConfig.loadConfig(traceAnalyzeConfig);
                  if (logAnalysisConfig != null) {
                     traceConfigMap.put(traceAnalyzeConfig.getTraceId(), logAnalysisConfig);
                  }
               }
               for (DocumentModel<LogHouseModel> doc : documentList) {
                  if (processedDocs.add(doc.getDocId())) {
                     String traceId = doc.getSource().getTraceId();
                     LogAnalysisConfig logAnalysisConfig = traceConfigMap.get(traceId);
                     if (logAnalysisConfig == null) {
                        String message = String.format("Trace %s does not have configuration. Moving log to log_error.",
                            traceId);
                        LOGGER.error(message);
                        logErrorService.saveLogError(doc, message);
                        latch.countDown();
                     } else {
                        taskExecutor.execute(
                            new AnalyzeThread(doc, logAnalysisConfig, loaderInfo, analysisService, latch));
                     }
                  } else {
                     latch.countDown();
                  }
               }

               // Wait until all thread finish
               latch.await();
            } else {
               String traceIds = String.join(", ", listTraceId);
               String errMessage = String.format("Trace configuration empty. Trace list: %s", traceIds);
               logErrorService.saveLogErrorList(documentList, errMessage);
               LOGGER.error("Trace configuration empty. Moving all logs to error table. Trace list: {}", traceIds);
            }
         } catch (InterruptedException e) {
            LOGGER.error(e);
            Thread.currentThread().interrupt();
         } finally {
            logRegisterService.bulkRegisterLog();
         }
      } else {
         LOGGER.info("There are no unprocessed logs.");
      }
      if (StringUtils.isNotBlank(analyzeValue.getDocId())) {
         LOGGER.info("Analyze process completed. Update analyze value in RDB.");
         analyzeValueRepository.save(analyzeValue);
      }

   }

}
