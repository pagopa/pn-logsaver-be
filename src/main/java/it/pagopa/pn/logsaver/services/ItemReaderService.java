package it.pagopa.pn.logsaver.services;

import java.io.InputStream;
import java.util.List;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;


public interface ItemReaderService {


  List<Item> findItems(DailyContextCfg dailyCtx);

  InputStream getItemContent(String key);

}
