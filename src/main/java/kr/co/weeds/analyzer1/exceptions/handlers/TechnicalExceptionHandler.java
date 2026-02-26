package kr.co.weeds.analyzer1.exceptions.handlers;

import java.util.Locale;
import kr.co.weeds.analyzer1.exceptions.AbstractException;
import kr.co.weeds.analyzer1.exceptions.TechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class TechnicalExceptionHandler extends AbstractExceptionHandler {

  private final MessageSource exceptionMessages;

  @Autowired
  public TechnicalExceptionHandler(MessageSource exceptionMessages) {
    this.exceptionMessages = exceptionMessages;
  }

  @Override
  @ExceptionHandler(TechnicalException.class)
  public ResponseEntity<Object> handle(AbstractException exception, Locale locale) {
    return super.handle(exception, locale);
  }

  @Override
  protected MessageSource getMessageSource() {
    return exceptionMessages;
  }
}
