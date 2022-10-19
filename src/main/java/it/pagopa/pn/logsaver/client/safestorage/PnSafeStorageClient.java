package it.pagopa.pn.logsaver.client.safestorage;

import java.util.function.UnaryOperator;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorageReference;

public interface PnSafeStorageClient {

  AuditStorage uploadFile(AuditStorage itemUpd);

  AuditStorageReference dowloadFileInfo(AuditStorageReference audit);

  AuditStorageReference dowloadFile(AuditStorageReference audit,
      UnaryOperator<AuditStorageReference> downloadFunction);

}
