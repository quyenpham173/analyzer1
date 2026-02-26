package kr.co.weeds.analyzer1.exceptions;

import java.util.List;
import kr.co.weeds.analyzer1.exceptions.pojo.AlertMessage;
import org.springframework.http.HttpStatusCode;

public class TechnicalException extends AbstractException {


   public TechnicalException(Throwable cause) {
      super(cause);
   }

   public TechnicalException(List<AlertMessage> alertMessages, HttpStatusCode httpStatus) {
      super(alertMessages, httpStatus);
   }

   public TechnicalException(AlertMessage alertMessage, HttpStatusCode httpStatus) {
      super(alertMessage, httpStatus);
   }

   public TechnicalException(AlertMessage alertMessage, HttpStatusCode httpStatus, Throwable cause) {
      super(alertMessage, httpStatus, cause);
   }

   public TechnicalException(List<AlertMessage> alertMessages, HttpStatusCode httpStatus, Throwable cause) {
      super(alertMessages, httpStatus, cause);
   }
}
