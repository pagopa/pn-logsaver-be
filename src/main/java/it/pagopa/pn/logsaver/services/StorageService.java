package it.pagopa.pn.logsaver.services;

import java.util.List;
import it.pagopa.pn.logsaver.model.AuditContainer;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.StorageExecution;

public interface StorageService {



  void store(List<AuditContainer> files, DailyContextCfg cfg);

  StorageExecution latestStorageExecution();

}
