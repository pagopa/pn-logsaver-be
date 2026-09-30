package it.pagopa.pn.logsaver.client.safestorage;

import java.nio.file.Path;
import java.util.function.UnaryOperator;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;

public interface PnSafeStorageClient {

  AuditStorage uploadFiles(AuditStorage itemUpd);

  String uploadFile(Path filePath, ExportType exportType, Retention retention);

  AuditDownloadReference downloadFileInfo(AuditDownloadReference audit);

  AuditDownloadReference downloadFile(AuditDownloadReference audit,
      UnaryOperator<AuditDownloadReference> downloadFunction);

}
