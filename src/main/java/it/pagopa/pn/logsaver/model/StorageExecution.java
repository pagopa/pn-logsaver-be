package it.pagopa.pn.logsaver.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.*;


@Setter
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString
public class StorageExecution {

  private LocalDate logDate;

  private Set<LogFileType> logFileTypes;

  private List<ExecutionDetails> details;


  @Setter
  @Getter
  @AllArgsConstructor
  @ToString
  public static final class ExecutionDetails {
    private Retention retention;
    private AuditStorageStatus status;
    private ExportType exportType;

  }
}
