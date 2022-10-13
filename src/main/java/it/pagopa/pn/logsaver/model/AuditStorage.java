package it.pagopa.pn.logsaver.model;

import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@NoArgsConstructor
@Accessors(fluent = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class AuditStorage extends AuditFile {

  public enum AuditStorageStatus {
    CREATED, SENT
  }

  private String uploadKey;

  private Throwable error;

  private AuditStorageStatus status;

  public static AuditStorage from(AuditFile arc) {
    return AuditStorage.builder().retention(arc.retention()).filePath(arc.filePath())
        .logDate(arc.logDate()).exportType(arc.exportType()).build();
  }

  public boolean sendingError() {
    return Objects.nonNull(error);
  }
}
