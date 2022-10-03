package it.pagopa.pn.logsaver.services;

import java.io.InputStream;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;


public interface ItemReaderService {


  Stream<Item> findItems(DailyContextCfg dailyCtx);

  InputStream getItemContent(String key);

}
