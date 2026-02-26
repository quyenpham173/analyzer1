package kr.co.weeds.analyzer1.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import kr.co.weeds.analyzer1.dto.LogErrorFileDTO;
import kr.co.weeds.analyzer1.entity.h2.LogError;
import kr.co.weeds.analyzer1.model.DocumentModel;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.repository.h2.LogErrorRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogErrorService {

   private static final Logger LOGGER = LogManager.getLogger(LogErrorService.class);

   private final ErrorBackupService errorBackupService;

   private final LogErrorRepository logErrorRepository;

   @Value("${log-analyze.error-backup-folder:}")
   private String errorBackupFolder;

   @Value("${log-analyze.max-retry:5}")
   private Integer maxRetry;

   public List<LogErrorFileDTO> getLogErrorsInFile(int size, int from) throws IOException {
      if (StringUtils.isBlank(errorBackupFolder)) {
         errorBackupFolder = "error_backup";
      }
      String filePath = Paths.get(errorBackupFolder.trim(), "Analyzer1_failed_logs.csv").toString();
      List<LogErrorFileDTO> results = new ArrayList<>();
      try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
         String line = br.readLine(); // Skip header
         int currentIndex = 0;
         while ((line = br.readLine()) != null) {
            if (currentIndex++ < from) {
               continue;
            }
            String[] values = line.split(",", 3);
            LogErrorFileDTO user = new LogErrorFileDTO(values[0], values[1], values[2]);
            results.add(user);
            if (size > 0 && results.size() >= size) {
               break;
            }
         }
      }
      return results;
   }

   public List<LogErrorFileDTO> getAndRemove(List<String> docIds, int from, int size) throws IOException {
      if (StringUtils.isBlank(errorBackupFolder)) {
         errorBackupFolder = "error_backup";
      }
      String filePath = Paths.get(errorBackupFolder.trim(), "Analyzer1_failed_logs.csv").toString();
      String tempFile = Paths.get(errorBackupFolder.trim(), "Analyzer1_failed_temp.csv").toString();
      List<LogErrorFileDTO> results = new ArrayList<>();
      try (BufferedReader br = new BufferedReader(new FileReader(filePath));
          BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
         int currentIndex = 0;
         String line = br.readLine(); // Skip header
         if (line != null) {
            bw.write(line);
            bw.newLine();
         }
         while ((line = br.readLine()) != null) {
            currentIndex++;
            String[] values = line.split(",", 3);
            if (CollectionUtils.isNotEmpty(docIds)) {
               if (values.length > 2 && docIds.contains(values[0])) {
                  results.add(new LogErrorFileDTO(values[0], values[1], values[2]));
               } else {
                  bw.write(line);
                  bw.newLine();
               }
            } else {
               if (currentIndex < from) {
                  bw.write(line);
                  bw.newLine();
               } else {
                  if (size > 0 && results.size() >= size) {
                     bw.write(line);
                     bw.newLine();
                     continue;
                  }
                  if (values.length > 2) {
                     LogErrorFileDTO user = new LogErrorFileDTO(values[0], values[1], values[2]);
                     results.add(user);
                  }
               }
            }
         }
      }
      Files.move(Paths.get(tempFile), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
      return results;
   }

   public void saveLogError(DocumentModel<?> document, String message) {
      String docId = document.getDocId();
      Optional<LogError> logErrorOpt = logErrorRepository.findById(docId);
      LogError logError;
      if (logErrorOpt.isPresent()) {
         logError = logErrorOpt.get();
         if (logError.getRetryCount() >= maxRetry) {
            LOGGER.error("Log {} exceeds max retry times. Moving log to error backup file.", docId);
            logErrorRepository.delete(logError);
            errorBackupService.moveLogToErrorFile(Collections.singletonList(logError), false);
            return;
         }
         LOGGER.error("Log {} re-analysis failed count: {}", docId, logError.getRetryCount() + 1);
         logError.setRetryCount(logError.getRetryCount() + 1);
      } else {
         LOGGER.error("Error occur when process log {}. Moving to error table. Error message: {}", docId, message);
         logError = new LogError();
         logError.setDocumentId(docId);
         logError.setLastRun(new Date());
         logError.setRetryCount(0);
      }
      logError.setMessage(message);
      logErrorRepository.save(logError);
   }

   public void saveLogErrorList(List<DocumentModel<LogHouseModel>> documents, String message) {
      if (CollectionUtils.isEmpty(documents)) {
         return;
      }
      List<LogError> logErrorSave = new ArrayList<>();
      List<LogError> logErrorRemove = new ArrayList<>();
      Set<String> docIds = documents.stream().map(DocumentModel::getDocId).collect(Collectors.toSet());
      List<LogError> logErrorExistList = logErrorRepository.findByDocumentIdIn(docIds);
      for (DocumentModel<LogHouseModel> document : documents) {
         logErrorExistList.stream().filter(log -> log.getDocumentId().equals(document.getDocId())).findFirst()
             .ifPresentOrElse(
                 logError -> {
                    if (logError.getRetryCount() >= maxRetry) {
                       logErrorRemove.add(logError);
                    } else {
                       logError.setRetryCount(logError.getRetryCount() + 1);
                       logErrorSave.add(logError);
                    }
                 },
                 () -> {
                    LogError logError = new LogError();
                    logError.setDocumentId(document.getDocId());
                    logError.setMessage(message);
                    logError.setLastRun(new Date());
                    logError.setRetryCount(0);
                    logErrorSave.add(logError);
                 }
             );
      }
      logErrorRepository.deleteAll(logErrorRemove);
      logErrorRepository.saveAll(logErrorSave);
      errorBackupService.moveLogToErrorFile(logErrorRemove, false);
   }

}
