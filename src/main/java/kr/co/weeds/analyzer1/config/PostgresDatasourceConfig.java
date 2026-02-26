package kr.co.weeds.analyzer1.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "kr.co.weeds.analyzer1.repository.postgres",
    entityManagerFactoryRef = "postgresEntityManagerFactory",
    transactionManagerRef = "postgresTransactionManager"
)
@MapperScan(basePackages = "kr.co.weeds.analyzer1.repository.mybatis.mapper", sqlSessionFactoryRef = "postgresSqlSessionFactory")
public class PostgresDatasourceConfig {

   @Primary
   @Bean(name = "postgresDataSource")
   @ConfigurationProperties(prefix = "spring.postgres.datasource")
   public DataSource postgresDataSource() {
      return DataSourceBuilder.create().build();
   }

   @Bean(name = "postgresSqlSessionFactory")
   public SqlSessionFactory postgresSqlSessionFactory(DataSource postgresDataSource) throws Exception {
      SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
      sessionFactory.setDataSource(postgresDataSource);
      sessionFactory.setMapperLocations(
          new PathMatchingResourcePatternResolver().getResources("classpath*:kr/co/weeds/analyzer1/repository/mybatis/sql/*.xml")
      );
      return sessionFactory.getObject();
   }

   @Primary
   @Bean(name = "postgresEntityManagerFactory")
   public LocalContainerEntityManagerFactoryBean postgresEntityManagerFactory(EntityManagerFactoryBuilder builder,
       @Qualifier("postgresDataSource") DataSource dataSource) {
      Map<String, Object> properties = new HashMap<>();
      properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
      return builder
          .dataSource(dataSource)
          .packages("kr.co.weeds.analyzer1.entity.postgres")
          .persistenceUnit("postgres")
          .properties(properties)
          .build();
   }

   @Primary
   @Bean(name = "postgresTransactionManager")
   public PlatformTransactionManager postgresTransactionManager(
       @Qualifier("postgresEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
   }

}
