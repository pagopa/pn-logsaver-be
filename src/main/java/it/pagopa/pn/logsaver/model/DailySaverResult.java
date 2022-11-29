package it.pagopa.pn.logsaver.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DailySaverResult extends DailyResult<AuditStorage> {

  private LocalDate logDate;
  @Default
  private List<AuditStorage> auditStorageList = new ArrayList<>();

  @Override
  public String toString() {
    return String.format("Date %s result %s file processed %d ", logDate.toString(),
        (!hasErrors() ? "SUCCESS" : "ERRORS"), auditStorageList.size());
  }

  @Override
  String handleBaseMessage(AuditStorage audit) {
    return String.format("File  Retention '%s' ExportType '%s' ", audit.retention().name(),
        audit.exportType().name());
  }

  @Override
  List<AuditStorage> getItems() {
    return auditStorageList;
  }

  @Override
  boolean itemHasError(AuditStorage audit) {
    return audit.hasError();
  }

  @Override
  String detailErrorMessage(AuditStorage audit) {
    return String.format("Error: '%s'", audit.getErrorMessage());
  }

  @Override
  String detailSuccessMessage(AuditStorage audit) {
    return String.format(" StorageKey: '%s'", audit.uploadKey());
  }

}
