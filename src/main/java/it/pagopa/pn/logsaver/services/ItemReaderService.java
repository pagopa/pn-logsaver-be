package it.pagopa.pn.logsaver.services;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.ItemType;


public interface ItemReaderService {

  List<Item> findItems(ItemType type, LocalDate date);

  List<Item> findItems(LocalDate date);

  InputStream getItemContent(String key);

}
