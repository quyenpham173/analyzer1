package kr.co.weeds.analyzer1.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.opensearch.client.RestClientBuilder.RequestConfigCallback;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SourceNoSqlConfiguration {

   @Value("${lh-server.repo.host}")
   private String host;

   @Value("${lh-server.repo.port}")
   private int port;

   @Value("${lh-server.repo.protocol}")
   private String protocol;

   @Value("${lh-server.repo.username:}")
   private String username;

   @Value("${lh-server.repo.password:}")
   private String password;

   @Value("${lh-server.repo.cacert:}")
   private String cacert;

   @Value("${lh-server.repo.hostname_verify:false}")
   private Boolean hostnameVerify;

   @Value("${lh-server.timeout.connect:5000}")
   private int connectTimeout;

   @Value("${lh-server.timeout.socket:300000}")
   private int socketTimeout;

   @Value("${lh-server.timeout.connection-request:30000}")
   private int connectionRequestTimeout;

   @Bean("clientSource")
   public RestHighLevelClient clientSource(RequestConfigCallback sourceRequestConfigCallback,
       HttpClientConfigCallback sourceHttpClientConfigCallback) {
      return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, protocol))
          .setRequestConfigCallback(sourceRequestConfigCallback)
          .setHttpClientConfigCallback(sourceHttpClientConfigCallback));
   }

   @Bean
   public RestClientBuilder.RequestConfigCallback requestConfigCallback() {
      return requestConfigBuilder -> requestConfigBuilder
          .setConnectTimeout(connectTimeout)
          .setSocketTimeout(socketTimeout)
          .setConnectionRequestTimeout(connectionRequestTimeout);
   }

   @Bean("sourceHttpClientConfigCallback")
   public RestClientBuilder.HttpClientConfigCallback sourceHttpClientConfigCallback() {
      if (StringUtils.isBlank(username)) {
         return httpClientBuilder -> httpClientBuilder;
      }
      try {
         final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
         credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

         SSLContext sslContext = getSSLContext();

         return httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
             .setSSLContext(sslContext)
             .setSSLHostnameVerifier(
                 Boolean.TRUE.equals(hostnameVerify) ? new DefaultHostnameVerifier() : new NoopHostnameVerifier());
      } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException |
               KeyManagementException e) {
         return httpClientBuilder -> httpClientBuilder;
      }
   }

   private SSLContext getSSLContext()
       throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException, IOException {
      if (StringUtils.isBlank(cacert)) {
         return SSLContexts.custom()
             .loadTrustMaterial(null, (chain, authType) -> true)
             .build();
      }
      Path caCertificatePath = Paths.get(cacert);

      final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      KeyStore keyStore = KeyStore.getInstance("pkcs12");
      keyStore.load(null, null);
      keyStore.setCertificateEntry("ca",
          certificateFactory.generateCertificate(Files.newInputStream(caCertificatePath)));

      return SSLContexts.custom()
          .loadTrustMaterial(keyStore, null)
          .build();
   }

}
