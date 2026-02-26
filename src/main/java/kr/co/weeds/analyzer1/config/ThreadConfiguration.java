package kr.co.weeds.analyzer1.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class ThreadConfiguration {

   @Value("${thread.poolSize.core:3}")
   private int coreSize;

   @Value("${thread.poolSize.max:5}")
   private int maxSize;

   @Value("${thread.capacity:50}")
   private int capacity;

   @Value("${thread.prefix:AnalyzeThread-}")
   private String prefix;

   @Bean(name = "taskExecutor")
   public Executor taskExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(coreSize);
      executor.setMaxPoolSize(maxSize);
      executor.setQueueCapacity(capacity);
      executor.setThreadNamePrefix(prefix);
      executor.setRejectedExecutionHandler(new CallerRunsPolicy());
      executor.initialize();
      return executor;
   }

}
