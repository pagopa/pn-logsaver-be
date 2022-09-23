package it.pagopa.pn.logsaver.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.StorageExecution;

public interface StorageService {



  List<AuditStorage> store(List<AuditFile> files, DailyContextCfg cfg);

  StorageExecution latestStorageExecution();

  LocalDate latestContinuosExecutionDate();

  Map<LocalDate, StorageExecution> storageExecutionBetween(LocalDate from, LocalDate to);


}
