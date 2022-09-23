package it.pagopa.pn.logsaver.services.impl;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.DailySaverResult.DailySaverResultBuilder;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.StorageExecution.ExecutionDetails;
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


  private DailyContextCfg checkDate(LocalDate date, Map<LocalDate, StorageExecution> execList) {

    if (execList.containsKey(date)) {
      // La data ha un esecuzione
      StorageExecution storExec = execList.get(date);
      // Dal dettaglio dell'esecuzione raggruppo eventuali failure per file Audit
      Map<Retention, Set<ExportType>> recoveryMap =
          handleRetentionExportTypeFromStorageExecution(storExec, true);

      return recoveryMap.isEmpty() ? null
          : DailyContextCfg.builder().logDate(date).tmpBasePath(cfg.getTmpBasePath())
              // .retentions(recoveryMap.keySet())
              .itemTypes(storExec.getTypesProcessed()).retentionExportTypeMap(recoveryMap).build();
    } else {
      // Non ho esecuzioni per la data.
      // Recupero le configurazioni dall'esecuzione precedente
      LocalDate dateToSearch = LocalDate.from(date);
      do {
        dateToSearch = dateToSearch.minusDays(1);
      } while (Collections.binarySearch(execList.keySet().stream().collect(toList()),
          dateToSearch) < 0);

      StorageExecution storExec = execList.get(dateToSearch);

      return DailyContextCfg.builder().logDate(date).tmpBasePath(cfg.getTmpBasePath())
          .itemTypes(storExec.getTypesProcessed())
          .retentionExportTypeMap(handleRetentionExportTypeFromStorageExecution(storExec, false))
          .build();
    }
  }

  private Map<Retention, Set<ExportType>> handleRetentionExportTypeFromStorageExecution(
      StorageExecution storExec, boolean filterNotSent) {

    return storExec.getDetails().stream()
        .filter(detail -> !filterNotSent || detail.getStatus() != AuditStorageStatus.SENT)
        .collect(groupingBy(ExecutionDetails::getRetention, collectingAndThen(toList(), list -> list
            .stream().map(ExecutionDetails::getExportType).distinct().collect(toSet()))));
  }

  @Override
  public List<DailySaverResult> dailySaverFromLatestExecutionToYesterday(Set<ItemType> itemTypes,
      Map<Retention, Set<ExportType>> retentionExportTypeMap) {

    List<DailySaverResult> resList = new ArrayList<>();
    LocalDate yesterday = DateUtils.yesterday();

    log.info("Start LogSaver from latest execution to Yesterday {}. Check for last execution...",
        yesterday.toString());

    // Leggo ultima esecuzione consecutiva
    LocalDate lastContExecDate = storageService.latestContinuosExecutionDate();

    // se yesterday-lastContExecDate > 1 sono presenti esecuzioni non processate correttamente o
    // date senza esecuzione
    if (Duration.between(lastContExecDate.atStartOfDay(), yesterday.atStartOfDay()).toDays() > 1) {
      // Recupero date da elaborare:
      // Leggo tutte le esecuzioni registrate da lastContExecDate a yesterday
      //
      log.info(
          "There are  previous days to be processed. Read executions after the last continuos date {}",
          lastContExecDate);
      Map<LocalDate, StorageExecution> execList =
          storageService.storageExecutionBetween(lastContExecDate, yesterday);

      List<DailyContextCfg> workList = DateUtils.getDatesRange(lastContExecDate, yesterday).stream() //
          .map(dateToCheck -> checkDate(dateToCheck, execList)).filter(Objects::nonNull)
          .collect(Collectors.toList());
      log.info("There are {} previous days to be processed", workList.size());

      workList.stream().map(this::dailySaver).collect(toCollection(() -> resList));
      log.info("Processing previous days finished");
    }


    if (yesterday.isAfter(lastContExecDate)) {
      resList.add(dailySaver(DailyContextCfg.builder().logDate(yesterday)
          .retentionExportTypeMap(retentionExportTypeMap).itemTypes(itemTypes)
          .tmpBasePath(cfg.getTmpBasePath()).build()));

    } else {
      log.warn("Date of last execution {} is after or equal of yesterday {}",
          lastContExecDate.toString(), yesterday.toString());
    }

    return resList;

  }

  @Override
  public List<DailySaverResult> dailyListSaver(List<LocalDate> dateExecutionList,
      Set<ItemType> types, ExportType exportType) {
    // TODO Valutare i controlli da fare sulla lista date e come costruire il contesto della data
    throw new NotImplementedException("It will be implemented!");
  }



  @Override
  public DailySaverResult dailySaver(DailyContextCfg dailyCtx) {

    DailySaverResultBuilder resBuilder = DailySaverResult.builder().logDate(dailyCtx.logDate());
    try {

      log.info("Start execution for day {}", dailyCtx.logDate());

      dailyCtx.initContext();

      List<Item> items = readerService.findItems(dailyCtx.logDate());

      List<AuditFile> grouped = service.process(items, dailyCtx);

      List<AuditStorage> auditStorageList = storageService.store(grouped, dailyCtx);
      log.info("End execution for day {}", dailyCtx.logDate());

      return resBuilder.auditStorageList(auditStorageList).build();

    } catch (Exception e) {
      log.error("Error processing audit for day {}", dailyCtx.logDate().toString());
      log.error("Error stacktrace", e);
      return resBuilder.error(e).build();
    } finally {
      // dailyCtx.destroy();
    }
  }
}
