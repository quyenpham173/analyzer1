package kr.co.weeds.analyzer1.exceptions;

import java.util.Collections;
import java.util.List;
import kr.co.weeds.analyzer1.exceptions.pojo.AlertMessage;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public abstract class AbstractException extends RuntimeException {

  private final HttpStatusCode httpStatus;
  private final transient List<AlertMessage> alertMessages;

  protected AbstractException(Throwable cause) {
    super(cause);
    this.alertMessages = Collections.emptyList();
    this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  protected AbstractException(List<AlertMessage> alertMessages, HttpStatusCode httpStatus) {
    this.alertMessages = alertMessages;
    this.httpStatus = httpStatus;
  }

  protected AbstractException(AlertMessage alertMessage, HttpStatusCode httpStatus) {
    this.alertMessages = Collections.singletonList(alertMessage);
    this.httpStatus = httpStatus;
  }

  protected AbstractException(AlertMessage alertMessage, HttpStatusCode httpStatus, Throwable cause) {
    super(cause);
    this.alertMessages = Collections.singletonList(alertMessage);
    this.httpStatus = httpStatus;
  }

  protected AbstractException(List<AlertMessage> alertMessages, HttpStatusCode httpStatus, Throwable cause) {
    super(cause);
    this.alertMessages = alertMessages;
    this.httpStatus = httpStatus;
  }
}
