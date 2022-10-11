package it.pagopa.pn.logsaver.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class StorageExecution {

  private LocalDate logDate;

  private Set<LogFileType> logFileTypes;

  private List<ExecutionDetails> details;


  @Setter
  @Getter
  @AllArgsConstructor
  public static final class ExecutionDetails {
    private Retention retention;
    private AuditStorageStatus status;
    private ExportType exportType;
  }
}
