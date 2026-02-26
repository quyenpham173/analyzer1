package kr.co.weeds.analyzer1.exceptions.pojo;

import java.util.Arrays;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Getter
public class AlertMessage {

  private final IAlertCode iAlertCode;

  private final Object[] args;

  private AlertMessage(IAlertCode iAlertCode) {
    this(iAlertCode, "");
  }

  public static AlertMessage alert(IAlertCode iAlertCode) {
    if (Objects.isNull(iAlertCode)) {
      throw new IllegalArgumentException("The AlertCode must not be null.");
    }

    return new AlertMessage(iAlertCode);
  }

  public static AlertMessage alert(IAlertCode alertCode, Object... args) {
    if (Objects.isNull(alertCode)) {
      throw new IllegalArgumentException("The ErrorCode must not be null.");
    }

    return new AlertMessage(alertCode, args);
  }

  private AlertMessage(IAlertCode iAlertCode, Object... args) {
    this.iAlertCode = iAlertCode;
    this.args = args;
  }

  @Override
  public String toString() {
    return "AlertMessages [alertCode=" + iAlertCode + ", args=" + Arrays.toString(args) + "]";
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, true);
  }

  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }
}
