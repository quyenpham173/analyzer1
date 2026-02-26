package kr.co.weeds.analyzer1.config;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import kr.co.weeds.analyzer1.constant.JobKey;
import kr.co.weeds.analyzer1.entity.postgres.AnalyzeJob;
import kr.co.weeds.analyzer1.repository.postgres.AnalyzeJobRepository;
import kr.co.weeds.analyzer1.service.JobService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class JobConfiguration implements SchedulingConfigurer {

   private static final Logger LOGGER = LogManager.getLogger(JobConfiguration.class);

   private final AnalyzeJobRepository analyzeJobRepository;

   private final JobService jobService;

   private final ThreadPoolTaskScheduler taskScheduler;

   @Value("${spring.task.scheduling.analyze-timeout:5}")
   private int timeout;

   private volatile boolean isJobEnabled = false;

   @Override
   public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
      taskRegistrar.setScheduler(taskScheduler);
      taskRegistrar.addTriggerTask(this::analysisLogJob, this::getFixedDelay);
   }

   public void analysisLogJob() {
      if (!isJobEnabled) {
         LOGGER.info("Job is disabled or not ready. Skipping execution.");
         return;
      }

      LOGGER.info("===== User analysis log job is starting =====");
      CompletableFuture<Void> future = CompletableFuture.runAsync(jobService::runAnalysis,
          taskScheduler.getScheduledExecutor()).orTimeout(timeout, TimeUnit.MINUTES).exceptionally(ex -> {
         LOGGER.warn("Job execution timed out or failed: {}", ex.getMessage());
         return null;
      });

      try {
         future.get(timeout, TimeUnit.MINUTES);
      } catch (TimeoutException e) {
         LOGGER.warn("Job execution timed out. Cancelling...");
         future.cancel(true);
      } catch (Exception e) {
         LOGGER.error("Unexpected error in job execution", e);
      } finally {
         LOGGER.info("===== User analysis log job completed =====");
      }
   }

   private Instant getFixedDelay(TriggerContext triggerContext) {
      LOGGER.info("Getting schedule configuration from RDB.");
      Optional<AnalyzeJob> analyzeJobOptional = analyzeJobRepository.findById(JobKey.ANALYZER_1.name());
      Instant lastCompletion = triggerContext.lastCompletion();
      if (analyzeJobOptional.isPresent()) {
         AnalyzeJob analyzeJob = analyzeJobOptional.get();
         isJobEnabled = BooleanUtils.isTrue(analyzeJob.getEnabled());
         if (isJobEnabled) {
            long delay = Long.parseLong(analyzeJob.getJobValue());
            LOGGER.info("Job name {} with fixed delay {}", JobKey.ANALYZER_1, delay);
            if (lastCompletion == null) {
               lastCompletion = Instant.now();
            }
            return lastCompletion.plusSeconds(delay);
         }
      }
      isJobEnabled = false;
      LOGGER.error("Cannot get LOG_ANALYSIS job schedule. Stop schedule, re-run in 30 seconds later.");
      return Instant.now().plusSeconds(30);
   }

}
