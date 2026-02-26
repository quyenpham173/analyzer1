package kr.co.weeds.analyzer1.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BeanConfiguration {

   @Value("${spring.task.scheduling.pool.size:5}")
   private int schedulePoolSize;

   @Value("${spring.task.scheduling.prefix:AnalyzeJob}")
   private String prefix;

   @Bean
   public ThreadPoolTaskScheduler taskScheduler() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setPoolSize(schedulePoolSize);
      scheduler.setThreadNamePrefix(prefix);
      scheduler.initialize();
      return scheduler;
   }

   @Bean
   public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
      return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), new HashMap<>(), null);
   }

   @Bean
   public RestTemplate restTemplate() {
      RestTemplate restTemplate = new RestTemplate();
      restTemplate.setMessageConverters(getMessageConverters());
      return restTemplate;
   }

   @Bean
   public ObjectMapper mapper() {
      return new ObjectMapper().registerModule(new JavaTimeModule());
   }

   private List<HttpMessageConverter<?>> getMessageConverters() {
      List<HttpMessageConverter<?>> converters = new ArrayList<>();
      MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
      converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
      converters.add(converter);
      return converters;
   }

}
