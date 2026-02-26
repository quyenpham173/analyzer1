package kr.co.weeds.analyzer1.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "kr.co.weeds.analyzer1.repository.h2",
    entityManagerFactoryRef = "h2EntityManagerFactory",
    transactionManagerRef = "h2TransactionManager"
)
public class H2DataSourceConfig {

   @Bean(name = "h2DataSource")
   @ConfigurationProperties(prefix = "spring.h2.datasource")
   public DataSource h2DataSource() {
      return DataSourceBuilder.create().build();
   }

   @Bean(name = "h2EntityManagerFactory")
   public LocalContainerEntityManagerFactoryBean h2EntityManagerFactory(EntityManagerFactoryBuilder builder,
       @Qualifier("h2DataSource") DataSource dataSource) {
      Map<String, Object> jpaProperties = new HashMap<>();
      jpaProperties.put("hibernate.hbm2ddl.auto", "update");
      jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
      return builder
          .dataSource(dataSource)
          .packages("kr.co.weeds.analyzer1.entity.h2")
          .persistenceUnit("h2")
          .properties(jpaProperties)
          .build();
   }

   @Bean(name = "h2TransactionManager")
   public PlatformTransactionManager h2TransactionManager(
       @Qualifier("h2EntityManagerFactory") EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
   }

}
