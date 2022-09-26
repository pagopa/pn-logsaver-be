package it.pagopa.pn.logsaver.services.impl;

import static java.util.stream.Collectors.toCollection;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.DailySaverResult.DailySaverResultBuilder;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import it.pagopa.pn.logsaver.services.ItemProcessorService;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.services.support.AuditSaverLogicSupport;
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
  public List<DailySaverResult> dailySaverFromLatestExecutionToYesterday(Set<ItemType> itemTypes,
      Map<Retention, Set<ExportType>> retentionExportTypeMap) {

    List<DailySaverResult> resList = new ArrayList<>();
    LocalDate yesterday = DateUtils.yesterday();

    log.info("Start LogSaver from latest execution to Yesterday {}. Check for last execution...",
        yesterday.toString());

    // Leggo ultima esecuzione consecutiva
    LocalDate lastContExecDate = storageService.latestContinuosExecutionDate();
    Map<LocalDate, StorageExecution> executionMap = new HashMap<>();
    // se yesterday-lastContExecDate > 1 sono presenti esecuzioni non processate correttamente o
    // date senza esecuzione
    if (Duration.between(lastContExecDate.atStartOfDay(), yesterday.atStartOfDay()).toDays() > 1) {
      // Recupero date da elaborare:
      // Leggo tutte le esecuzioni registrate da lastContExecDate a yesterday
      //
      log.info(
          "There are  previous days to be processed. Read executions after the last continuos date {}",
          lastContExecDate);

      AuditSaverLogicSupport.groupByDate(
          storageService.storageExecutionBetween(lastContExecDate, yesterday), executionMap);

      List<DailyContextCfg> workList = DateUtils.getDatesRange(lastContExecDate, yesterday).stream() //
          .map(dateToCheck -> recoveryDailyContext(dateToCheck, executionMap))
          .filter(Objects::nonNull).collect(Collectors.toList());

      log.info("There are {} previous days to be processed", workList.size());
      workList.stream().map(this::dailySaver).collect(toCollection(() -> resList));
      log.info("Processing previous days finished");
    }


    if (executionMap.containsKey(yesterday)) {
      DailyContextCfg ctx = handleDailyContext(yesterday, yesterday, executionMap, true);
      if (Objects.nonNull(ctx)) {
        resList.add(dailySaver(ctx));
      } else {
        log.info("Log date {} has already been successfully executed", yesterday.toString());
      }
    } else {
      resList.add(dailySaver(DailyContextCfg.builder().logDate(yesterday)
          .retentionExportTypeMap(retentionExportTypeMap).itemTypes(itemTypes)
          .tmpBasePath(cfg.getTmpBasePath()).build()));
    }

    return resList;
  }

  @Override
  public List<DailySaverResult> dailyListSaver(List<LocalDate> dateExecutionList,
      Set<ItemType> types, ExportType exportType) {
    // TODO Valutare i controlli da fare sulla lista date e come costruire il contesto della data
    throw new NotImplementedException("It will be implemented!");
  }


  private DailyContextCfg recoveryDailyContext(LocalDate logDate,
      Map<LocalDate, StorageExecution> execList) {

    if (execList.containsKey(logDate)) {
      // Costruisco il contesto prendendo dall'ultima esecuzione le configurazioni dei file non
      // inviati
      // se il contesto null l'esecuzione è stata completata correttamente
      return handleDailyContext(logDate, logDate, execList, true);

    } else {
      // Non ho esecuzioni per la data.
      // Recupero le configurazioni dall'esecuzione precedente
      LocalDate dateToSearch = LocalDate.from(logDate);
      do {
        dateToSearch = dateToSearch.minusDays(1);
      } while (!execList.containsKey(dateToSearch));

      return handleDailyContext(logDate, dateToSearch, execList, false);

    }
  }

  private DailyContextCfg handleDailyContext(LocalDate logDate, LocalDate recoveryDate,
      Map<LocalDate, StorageExecution> execList, boolean filterNotSent) {

    StorageExecution storExec = execList.get(recoveryDate);

    Map<Retention, Set<ExportType>> recoveryMap = AuditSaverLogicSupport
        .handleRetentionExportTypeFromStorageExecution(storExec, filterNotSent);

    return recoveryMap.isEmpty() ? null
        : DailyContextCfg.builder().logDate(logDate).tmpBasePath(cfg.getTmpBasePath())
            .itemTypes(storExec.getItemTypes()).retentionExportTypeMap(recoveryMap).build();

  }



  private DailySaverResult dailySaver(DailyContextCfg dailyCtx) {

    DailySaverResultBuilder resBuilder = DailySaverResult.builder().logDate(dailyCtx.logDate());
    try {

      log.info("Start execution for day {}", dailyCtx.logDate());
      dailyCtx.initContext();

      List<Item> items = readerService.findItems(dailyCtx);

      List<AuditFile> grouped = service.process(items, dailyCtx);

      List<AuditStorage> auditStorageList = storageService.store(grouped, dailyCtx);
      log.info("End execution for day {}", dailyCtx.logDate());

      return resBuilder.auditStorageList(auditStorageList).build();

    } catch (Exception e) {
      log.error("Error processing audit for day {}", dailyCtx.logDate().toString());
      log.error("Error stacktrace", e);
      return resBuilder.error(e).build();
    } finally {
      dailyCtx.destroy();
    }
  }
}
