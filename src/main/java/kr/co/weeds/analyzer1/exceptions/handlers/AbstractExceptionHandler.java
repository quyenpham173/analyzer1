package kr.co.weeds.analyzer1.exceptions.handlers;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import kr.co.weeds.analyzer1.exceptions.AbstractException;
import kr.co.weeds.analyzer1.exceptions.constants.AlertType;
import kr.co.weeds.analyzer1.exceptions.pojo.Alert;
import kr.co.weeds.analyzer1.exceptions.pojo.AlertWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

public abstract class AbstractExceptionHandler {

   private static final Logger LOGGER = LogManager.getLogger(AbstractExceptionHandler.class);

   public ResponseEntity<Object> handle(AbstractException exception, Locale locale) {

      AlertWrapper alertWrapper = new AlertWrapper();
      List<Alert> alertCodes = new ArrayList<>();
      if (CollectionUtils.isEmpty(exception.getAlertMessages())) {
         alertCodes.add(generateUnhandledError(exception));
      } else {
         try {
            alertCodes.addAll(
                exception.getAlertMessages().stream()
                    .map(ex -> new Alert(ex.getIAlertCode().getAlertCode().getCode(),
                        getMessageSource().getMessage(ex.getIAlertCode().getAlertCode().getLabel(),
                            ex.getArgs(), locale),
                        ex.getIAlertCode().getAlertCode().getType().name())
                    ).collect(Collectors.toList()));
         } catch (Exception e) {
            LOGGER.error("[AbstractExceptionHandler] An error has been occurred while generating the list of alerts.",
                e);
            alertCodes.add(generateUnhandledError(e));
         }

      }
      HttpStatusCode httpStatus = exception.getHttpStatus();
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      Set<Alert> setAlert = new HashSet<>(alertCodes);
      alertWrapper.setAlerts(setAlert);
      return new ResponseEntity<>(alertWrapper, headers, httpStatus);
   }

   protected static Alert generateUnhandledError(Exception ex) {
      return new Alert("TECHNICAL", ex.getMessage(), AlertType.ERROR.name());
   }

   protected abstract MessageSource getMessageSource();
}
