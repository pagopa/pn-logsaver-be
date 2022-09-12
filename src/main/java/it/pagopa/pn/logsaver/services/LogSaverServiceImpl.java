package it.pagopa.pn.logsaver.services;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.ArchiveInfo;
import it.pagopa.pn.logsaver.model.ItemLog;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import lombok.AllArgsConstructor;


@Service
@AllArgsConstructor
public class LogSaverServiceImpl implements LogSaverService {


  private final LogReaderService readerService;
  private final LogProcessorServiceImpl service;
  private final SafeStorageService storageService;
  private final LogSaverCfg cfg;

  @Override
  public Boolean saveLogs() {


    try {
      List<ItemLog> items = readerService.findLogs(cfg.getLogDate());

      items.stream().map(service::temporaryStore).collect(Collectors.toList());

      List<ArchiveInfo> archives = service.zipAllItemsByRetention();

      storageService.send(archives);

      return Boolean.TRUE;

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return Boolean.FALSE;
    }
  }
}
