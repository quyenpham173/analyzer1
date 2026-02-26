package kr.co.weeds.analyzer1.integration;

import java.util.List;
import kr.co.weeds.analyzer1.dto.OpUserResponseDTO;
import kr.co.weeds.analyzer1.exceptions.CustomRestClientException;
import kr.co.weeds.analyzer1.exceptions.TechnicalException;
import kr.co.weeds.analyzer1.exceptions.constants.TechnicalAlertCode;
import kr.co.weeds.analyzer1.exceptions.pojo.AlertMessage;
import kr.co.weeds.analyzer1.model.loader.OpUser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class OpUserAdapter extends BaseIntegration {

   private static final Logger LOGGER = LogManager.getLogger(OpUserAdapter.class);

   @Value("${cache-server.protocol}")
   private String protocol;

   @Value("${cache-server.host}")
   private String host;

   @Value("${cache-server.port}")
   private String port;

   @Value("${cache-server.context-path}")
   private String contextPath;

   protected OpUserAdapter(RestTemplate restTemplate) {
      super(restTemplate);
   }

   public OpUser getOpUserInfo(MultiValueMap<String, String> params, String path) {
      String url = String.format("%s://%s:%s%s%s", protocol, host, port, contextPath, path);
      try {
         ResponseEntity<OpUserResponseDTO> responseEntity = callExternalApi(
             url,
             HttpMethod.GET,
             OpUserResponseDTO.class,
             null,
             params);
         OpUserResponseDTO response = responseEntity.getBody();
         if (response != null) {
            List<OpUser> opUserList = response.getOpUserList();
            if (CollectionUtils.isNotEmpty(opUserList)) {
               return opUserList.get(0);
            } else {
               return response.getOpUser();
            }
         }
         return null;
      } catch (CustomRestClientException e) {
         LOGGER.error("Error when get opuser info from cache server.", e);
         throw new TechnicalException(
             AlertMessage.alert(TechnicalAlertCode.CACHE_SERVER_ERROR),
             HttpStatus.INTERNAL_SERVER_ERROR);
      }
   }

}
