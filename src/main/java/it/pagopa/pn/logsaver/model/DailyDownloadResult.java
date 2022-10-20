package it.pagopa.pn.logsaver.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class DailyDownloadResult {

  private DailyAuditDownloadable audit;

  private Throwable error;


  public boolean hasErrors() {
    return Objects.nonNull(error) || CollectionUtils.emptyIfNull(audit.audits()).stream()
        .filter(AuditDownloadReference::haveError).count() > 0;
  }

  public List<String> successMessages() {
    return messages(au -> !this.haveError(au), this::handleSuccessMessage);
  }

  public List<String> errorMessages() {
    return messages(this::haveError, this::handleErrorMessage);
  }


  private boolean haveError(AuditDownloadReference file) {
    return file.haveError() || AuditStorageStatus.SENT != file.status();
  }

  private String handleErrorMessage(AuditDownloadReference audit) {
    return handleBaseMessage(audit).concat(
        String.format("Status: '%s' Error: '%s'", audit.status().name(), audit.getErrorMessage()));
  }

  private String handleSuccessMessage(AuditDownloadReference audit) {
    return handleBaseMessage(audit)
        .concat(String.format("Download: '%s'", audit.destinationFolder()));
  }

  private String handleBaseMessage(AuditDownloadReference audit) {
    return String.format("File '%s' Retention '%s' ExportType '%s' ", audit.fileName(),
        audit.retention().name(), audit.exportType().name());
  }

  private List<String> messages(Predicate<AuditDownloadReference> predicate,
      Function<AuditDownloadReference, String> mapper) {
    return CollectionUtils.emptyIfNull(audit.audits()).stream().filter(predicate).map(mapper)
        .collect(Collectors.toList());
  }

}
