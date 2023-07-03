package it.pagopa.pn.logsaver.model;

import java.util.HashMap;
import java.util.Map;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import lombok.Builder.Default;
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
public class AuditStorage extends AuditFile implements ErrorAware {

  @Default
  private Map<String, String> uploadKey = new HashMap<>();

  private Throwable error;

  private AuditStorageStatus status;

  public static AuditStorage from(AuditFile arc) {
    return (AuditStorage) new AuditStorage().retention(arc.retention()).filePath(arc.filePath())
        .logDate(arc.logDate()).exportType(arc.exportType());
  }

}
