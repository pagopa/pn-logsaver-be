package it.pagopa.pn.logsaver.model;

import java.util.List;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DailyDownloadResult extends DailyResult<AuditDownloadReference> {

  private DailyAuditDownloadable audit;


  @Override
  List<AuditDownloadReference> getItems() {
    return audit.audits();
  }

  @Override
  boolean itemHasError(AuditDownloadReference file) {
    return file.hasError() || AuditStorageStatus.SENT != file.status();
  }

  @Override
  String detailErrorMessage(AuditDownloadReference file) {
    return String.format("Status: '%s' Error: '%s'", file.status().name(), file.getErrorMessage());
  }

  @Override
  String detailSuccessMessage(AuditDownloadReference file) {
    return String.format("Download: '%s'", file.destinationFolder());
  }

  @Override
  String handleBaseMessage(AuditDownloadReference file) {
    return String.format("File '%s'  ", file.fileName());
  }

}
