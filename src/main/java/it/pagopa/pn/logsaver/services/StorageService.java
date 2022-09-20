package it.pagopa.pn.logsaver.services;

import java.util.List;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.StorageExecution;

public interface StorageService {



  List<AuditStorage> store(List<AuditFile> files, DailyContextCfg cfg);

  StorageExecution latestStorageExecution();

}
