package it.pagopa.pn.logsaver.client.safestorage;

import java.util.function.UnaryOperator;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;

public interface PnSafeStorageClient {

  AuditStorage uploadFile(AuditStorage itemUpd);

  AuditDownloadReference dowloadFileInfo(AuditDownloadReference audit);

  AuditDownloadReference dowloadFile(AuditDownloadReference audit,
      UnaryOperator<AuditDownloadReference> downloadFunction);

}
