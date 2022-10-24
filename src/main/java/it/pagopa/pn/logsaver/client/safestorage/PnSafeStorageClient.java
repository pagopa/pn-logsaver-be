package it.pagopa.pn.logsaver.client.safestorage;

import java.util.function.UnaryOperator;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.AuditStorage;

public interface PnSafeStorageClient {

  AuditStorage uploadFile(AuditStorage itemUpd);

  AuditDownloadReference downloadFileInfo(AuditDownloadReference audit);

  AuditDownloadReference downloadFile(AuditDownloadReference audit,
      UnaryOperator<AuditDownloadReference> downloadFunction);

}
