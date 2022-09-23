package it.pagopa.pn.logsaver.services;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.model.Item;


public interface ItemReaderService {


  List<Item> findItems(LocalDate date);

  InputStream getItemContent(String key);

}
