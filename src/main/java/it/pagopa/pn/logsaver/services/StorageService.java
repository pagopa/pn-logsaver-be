package it.pagopa.pn.logsaver.services;

import java.time.LocalDate;
import java.util.List;
import java.util.function.UnaryOperator;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.DailyAuditDownloadable;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.StorageExecution;

public interface StorageService extends FileCompleteListener{

  List<AuditStorage> store(List<AuditFile> files, DailyContextCfg cfg);

  StorageExecution getLatestStorageExecution();

  LocalDate getLatestContinuosExecutionDate();

  List<StorageExecution> getStorageExecutionBetween(LocalDate from, LocalDate to);

  List<DailyAuditDownloadable> getAuditFile(LocalDate from, LocalDate to);

  AuditDownloadReference dowloadAuditFile(AuditDownloadReference audit,
      UnaryOperator<AuditDownloadReference> downloadFunction);

}
