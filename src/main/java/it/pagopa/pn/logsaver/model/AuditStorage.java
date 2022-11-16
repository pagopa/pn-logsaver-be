package it.pagopa.pn.logsaver.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
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
public class AuditStorage extends AuditFile {

  public enum AuditStorageStatus {
    CREATED, SENT
  }

  @Default
  private Map<String, String> uploadKey = new HashMap<>();

  private Throwable error;

  private AuditStorageStatus status;



  public static AuditStorage from(AuditFile arc) {
    return (AuditStorage) new AuditStorage().retention(arc.retention()).filePath(arc.filePath())
        .logDate(arc.logDate()).exportType(arc.exportType());
  }

  public boolean hasError() {
    return Objects.nonNull(error);
  }

  public String getErrorMessage() {
    return hasError() ? error.getMessage() : StringUtils.EMPTY;
  }

}
