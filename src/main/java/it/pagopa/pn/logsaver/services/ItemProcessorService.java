package it.pagopa.pn.logsaver.services;

import java.util.List;
import it.pagopa.pn.logsaver.model.AuditContainer;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;


public interface ItemProcessorService {

  Item process(Item log, DailyContextCfg dailyCxt);

  List<AuditContainer> groupByRetention(DailyContextCfg dailyCxt);

}
