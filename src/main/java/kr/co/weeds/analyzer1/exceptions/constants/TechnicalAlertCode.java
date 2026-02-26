package kr.co.weeds.analyzer1.exceptions.constants;

import kr.co.weeds.analyzer1.exceptions.pojo.AlertCode;
import kr.co.weeds.analyzer1.exceptions.pojo.IAlertCode;

public enum TechnicalAlertCode implements IAlertCode {
  CACHE_SERVER_ERROR("002000001", "cache.server.error", AlertType.ERROR),
  ERROR_BACKUP_FILE("002000002", "error.backup.file", AlertType.ERROR),
  RESOURCE_EXIST("002000003", "resource.exist", AlertType.ERROR),
  RESOURCE_NOT_FOUND("002000004", "resource.not.found", AlertType.ERROR);

  private final AlertCode alertCode;

  TechnicalAlertCode(final String code, final String label, final AlertType alertType) {
    alertCode = new AlertCode(code, label, alertType);
  }

  @Override
  public AlertCode getAlertCode() {
    return this.alertCode;
  }
}
