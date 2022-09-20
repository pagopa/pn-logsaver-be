package it.pagopa.pn.logsaver.model;

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
public class AuditStorage extends AuditFile {

  public enum AuditStorageStatus {
    CREATED, SENT
  };

  private String uploadKey;

  private boolean sendingError;

  private AuditStorageStatus status;

  public static AuditStorage from(AuditFile arc) {
    return AuditStorage.builder().retention(arc.retention()).filePath(arc.filePath())
        .logDate(arc.logDate()).build();
  }



}
