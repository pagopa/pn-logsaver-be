package it.pagopa.pn.logsaver.client.safestorage;

import it.pagopa.pn.logsaver.model.AuditStorage;

public interface PnSafeStorageClient {

  AuditStorage uploadFile(AuditStorage itemUpd);

}
