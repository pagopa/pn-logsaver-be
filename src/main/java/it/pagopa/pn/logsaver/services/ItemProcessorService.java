package it.pagopa.pn.logsaver.services;

import java.util.List;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;


public interface ItemProcessorService {

  List<AuditFile> process(List<Item> items, DailyContextCfg dailyCtx);

}
