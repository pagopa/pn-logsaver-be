package it.pagopa.pn.logsaver.services;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.ArchiveInfo;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.AllArgsConstructor;


@Service
@AllArgsConstructor
public class AuditSaverServiceImpl implements AuditSaverService {


  private final ItemReaderService readerService;
  private final ItemProcessorServiceImpl service;
  private final StorageService storageService;
  private final LogSaverCfg cfg;

  @Override
  public Boolean saveLogs() {

    DailyContextCfg dailyCtx =
        DailyContextCfg.of(DateUtils.parse("2022-07-11"), cfg.getTmpBasePath());

    try {


      dailyCtx.initContext();

      List<Item> items = readerService.findItems(dailyCtx.getLogDate());

      items.stream().map(item -> service.process(item, dailyCtx)).collect(Collectors.toList());

      List<ArchiveInfo> archives = service.zipAllItemsByRetention(dailyCtx);

      storageService.send(archives);

      return Boolean.TRUE;

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return Boolean.FALSE;
    } finally {
      dailyCtx.destroy();
    }
  }
}
