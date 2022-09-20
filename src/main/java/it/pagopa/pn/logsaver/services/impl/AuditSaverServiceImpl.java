package it.pagopa.pn.logsaver.services.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.DailySaverResult.DailySaverResultBuilder;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import it.pagopa.pn.logsaver.services.ItemProcessorService;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@AllArgsConstructor
@Slf4j
public class AuditSaverServiceImpl implements AuditSaverService {


  private final ItemReaderService readerService;
  private final ItemProcessorService service;
  private final StorageService storageService;
  private final LogSaverCfg cfg;

  @Override
  public List<DailySaverResult> dailySaverFromLatestExecutionToYesterday(ExportType exportType) {

    List<DailySaverResult> resList = new ArrayList<>();
    LocalDate yesterday = DateUtils.yesterday();

    log.info("Start LogSaver from latest execution to Yesterday {}. Check for last execution...",
        yesterday.toString());
    StorageExecution latestExec = storageService.latestStorageExecution();

    LocalDate lastExecDate = latestExec.getLatestExecutionDate();
    List<LocalDate> dateExecutionList = DateUtils.getDatesRange(lastExecDate, yesterday);

    if (!dateExecutionList.isEmpty()) {
      log.info("Date of last execution {}. There are {} previous days to be processed",
          lastExecDate.toString(), dateExecutionList.size());
      dailyListSaver(dateExecutionList, latestExec.getTypesProcessed(), latestExec.getExportType(),
          resList);
    }

    if (yesterday.isAfter(lastExecDate)) {
      resList.add(dailySaver(DailyContextCfg.of(yesterday, cfg.getTmpBasePath(),
          List.of(ItemType.values()), exportType)));

    } else {
      log.warn("Date of last execution {} is after or equal of yesterday {}",
          lastExecDate.toString(), yesterday.toString());
    }

    return resList;

  }

  @Override
  public List<DailySaverResult> dailyListSaver(List<LocalDate> dateExecutionList,
      List<ItemType> types, ExportType exportType) {
    return dailyListSaver(dateExecutionList, types, null, new ArrayList<>());
  }

  private List<DailySaverResult> dailyListSaver(List<LocalDate> dateExecutionList,
      List<ItemType> types, ExportType exportType, List<DailySaverResult> resList) {
    return dateExecutionList.stream()
        .map(date -> dailySaver(DailyContextCfg.of(date, cfg.getTmpBasePath(), types, exportType)))
        .collect(Collectors.toCollection(() -> resList));

  }

  @Override
  public DailySaverResult dailySaver(DailyContextCfg dailyCtx) {

    DailySaverResultBuilder resBuilder = DailySaverResult.builder();
    try {

      log.info("Start execution for day {}", dailyCtx.getLogDate());

      dailyCtx.initContext();

      List<Item> items = readerService.findItems(dailyCtx.getLogDate());

      List<AuditFile> grouped = service.process(items, dailyCtx);

      storageService.store(grouped, dailyCtx);
      log.info("End execution for day {}", dailyCtx.getLogDate());

      return resBuilder.auditList(grouped).build();

    } catch (Exception e) {
      log.error("Error processing audit for day {}", dailyCtx.getLogDate().toString());
      log.error("Error stacktrace", e);
      return resBuilder.error(e).build();
    } finally {
      // dailyCtx.destroy();
    }
  }
}
