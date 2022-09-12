package it.pagopa.pn.logsaver.services;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.model.ItemLog;
import it.pagopa.pn.logsaver.model.LogType;


public interface LogReaderService {

  List<ItemLog> findLogs(LogType type, LocalDate date);

  List<ItemLog> findLogs(LocalDate date);

  InputStream getLogContent(String key);

}
