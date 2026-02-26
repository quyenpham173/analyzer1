package kr.co.weeds.analyzer1.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class CustomRestClientException extends Exception {

   private static final long serialVersionUID = 1254672957987028725L;
   private final transient HttpStatusCode status;
   private final transient Object body;

   public CustomRestClientException(Throwable e) {
      super(e);
      this.body = null;
      this.status = null;
   }

   public CustomRestClientException(HttpStatusCode status, Object body, Throwable e) {
      super(e);
      this.status = status;
      this.body = body;
   }
}
