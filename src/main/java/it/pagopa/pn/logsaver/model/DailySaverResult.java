package it.pagopa.pn.logsaver.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class DailySaverResult {

  private LocalDate logDate;
  @Default
  private List<AuditStorage> auditStorageList = new ArrayList<>();

  private Throwable error;

  @Override
  public String toString() {
    return String.format("Date %s result %s file processed %d ", logDate.toString(),
        (!hasErrors() ? "SUCCESS" : "ERRORS"), auditStorageList.size());
  }

  public boolean hasErrors() {
    return Objects.nonNull(error) || CollectionUtils.emptyIfNull(auditStorageList).stream()
        .filter(AuditStorage::hasError).count() > 0;
  }

  public List<String> successMessages() {
    return messages(au -> !au.hasError(), this::handleSuccessMessage);
  }

  public List<String> errorMessages() {
    return messages(AuditStorage::hasError, this::handleErrorMessage);
  }

  private String handleErrorMessage(AuditStorage audit) {
    return handleBaseMessage(audit).concat(String.format("Error: '%s'", audit.getErrorMessage()));
  }

  private String handleSuccessMessage(AuditStorage audit) {
    return handleBaseMessage(audit).concat(String.format(" StorageKey: '%s'", audit.uploadKey()));
  }

  private String handleBaseMessage(AuditStorage audit) {
    return String.format("File '%s' Retention '%s' ExportType '%s' ", audit.fileName(),
        audit.retention().name(), audit.exportType().name());
  }

  private List<String> messages(Predicate<AuditStorage> predicate,
      Function<AuditStorage, String> mapper) {
    return CollectionUtils.emptyIfNull(auditStorageList).stream().filter(predicate).map(mapper)
        .collect(Collectors.toList());
  }
}
