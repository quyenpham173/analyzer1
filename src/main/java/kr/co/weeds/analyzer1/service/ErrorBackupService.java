package kr.co.weeds.analyzer1.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import kr.co.weeds.analyzer1.entity.h2.LogError;
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
public class ErrorBackupService {

   private static final Logger LOGGER = LogManager.getLogger(ErrorBackupService.class);

   private static final String[] HEADER = {"document ID", "analysis time", "message"};

   @Value("${log-analyze.error-backup-folder:}")
   private String errorBackupFolder;

   private final LogErrorRepository logErrorRepository;

   /**
    * Move logs exceed max retry to back up file
    *
    * @param logErrors List logs failed and exceed max retry
    */
   public void moveLogToErrorFile(List<LogError> logErrors, boolean isReAnalysis) {
      if(CollectionUtils.isEmpty(logErrors)) {
         return;
      }
      if (StringUtils.isBlank(errorBackupFolder)) {
         LOGGER.info("Error backup filepath was not set. Creating file in ./error_backup folder");
         errorBackupFolder = "error_backup";
      }
      String filePath = Paths.get(errorBackupFolder.trim(), "Analyzer1_failed_logs.csv").toString();
      File file = new File(filePath);
      File parentFile = file.getParentFile();
      if (!parentFile.exists()) {
         boolean dirsCreated = parentFile.mkdirs();
         if (dirsCreated) {
            LOGGER.info("Error backup folder created: {}", parentFile.getAbsolutePath());
         } else {
            LOGGER.error("Cannot create error backup folder: {}", parentFile.getAbsolutePath());
            return;
         }
      }
      boolean fileExists = file.exists();
      try (FileWriter fileWriter = new FileWriter(filePath, true);
          PrintWriter printWriter = new PrintWriter(fileWriter)) {
         if (!fileExists) {
            printWriter.println(String.join(",", HEADER));
         }
         String analysisTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
         logErrors.forEach(log -> {
            String errorData = log.getDocumentId() + "," + analysisTime + "," + log.getMessage();
            printWriter.println(errorData);
         });
         if(!isReAnalysis) {
            logErrorRepository.deleteAll(logErrors);
         }
         LOGGER.info("Moved {} logs to error backup file: {}", logErrors.size(), filePath);
      } catch (IOException e) {
         LOGGER.error("Error when move logs failed to error backup file.", e);
      }
   }

   public void backupNewError(List<LogError> logErrors) {
      if(CollectionUtils.isEmpty(logErrors)) {
         return;
      }
      String filePath = errorBackupFolder.trim() + "Analyzer1_failed_logs.csv";
      try (FileWriter fileWriter = new FileWriter(filePath, true);
          PrintWriter printWriter = new PrintWriter(fileWriter)) {
         printWriter.println(String.join(",", HEADER));
         String analysisTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
         logErrors.forEach(log -> {
            String errorData = log.getDocumentId() + "," + analysisTime + "," + log.getMessage();
            printWriter.println(errorData);
         });
         LOGGER.info("Data in error backup file was replace.");
      } catch (IOException e) {
         LOGGER.error("Error when re-new data in error backup file.", e);
      }
   }

}
