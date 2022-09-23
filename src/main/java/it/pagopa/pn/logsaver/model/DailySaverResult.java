package it.pagopa.pn.logsaver.model;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class DailySaverResult {

  private LocalDate logDate;

  private List<AuditStorage> auditStorageList;

  private boolean success;

  private Throwable error;

  @Override
  public String toString() {
    return String.format("Date %s result %s file uploaded %d ", logDate.toString(),
        (success ? "SUCCESS" : "ERROR"), auditStorageList.size());
  }



}
