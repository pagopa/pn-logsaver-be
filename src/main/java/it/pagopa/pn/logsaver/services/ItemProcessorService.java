package it.pagopa.pn.logsaver.services;

import java.util.List;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;


public interface ItemProcessorService {

  List<AuditFile> process(Stream<Item> items, DailyContextCfg dailyCtx);

}
