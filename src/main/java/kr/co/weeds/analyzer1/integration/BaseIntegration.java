package kr.co.weeds.analyzer1.integration;

import java.util.Collections;
import java.util.Objects;
import kr.co.weeds.analyzer1.exceptions.CustomRestClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class BaseIntegration {

   private static final Logger LOGGER = LogManager.getLogger(BaseIntegration.class);
   protected RestTemplate restTemplate;

   protected BaseIntegration(RestTemplate restTemplate) {
      this.restTemplate = restTemplate;
   }

   protected <T, S> ResponseEntity<T> callExternalApi(String url, HttpMethod method, Class<T> clazz, S body,
       MultiValueMap<String, String> params) throws CustomRestClientException {
      try {
         HttpHeaders headers = new HttpHeaders();
         headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
         HttpEntity<S> entity = new HttpEntity<>(body, headers);
         UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
         if (Objects.nonNull(params)) {
            builder.queryParams(params);
         }
         UriComponents uriComponents = builder.build().encode();
         return restTemplate.exchange(uriComponents.toUri(), method, entity, clazz);
      } catch (HttpStatusCodeException e) {
         LOGGER.error(e);
         throw new CustomRestClientException(e.getStatusCode(), e.getResponseBodyAsString(), e);
      } catch (Exception e) {
         LOGGER.error(e);
         throw new CustomRestClientException(e);
      }
   }

   protected <T, S> ResponseEntity<T> callExternalApiParameterizedTypeReference(String url, HttpMethod method,
       ParameterizedTypeReference<T> responseType, S body, MultiValueMap<String, String> params)
       throws CustomRestClientException {
      try {
         HttpHeaders headers = new HttpHeaders();
         headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
         HttpEntity<S> entity = new HttpEntity<>(body, headers);
         UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
         if (Objects.nonNull(params)) {
            builder.queryParams(params);
         }
         UriComponents uriComponents = builder.build().encode();
         return restTemplate.exchange(uriComponents.toUri(), method, entity, responseType);
      } catch (HttpStatusCodeException e) {
         LOGGER.error(e);
         throw new CustomRestClientException(e.getStatusCode(), e.getResponseBodyAsString(), e);
      } catch (Exception e) {
         LOGGER.error(e);
         throw new CustomRestClientException(e);
      }
   }

   protected <T, S> ResponseEntity<T> callExternalApi(String url, HttpMethod method, S body, Class<T> clazz)
       throws CustomRestClientException {
      return callExternalApi(url, method, clazz, body, null);
   }
}
