package it.pagopa.pn.logsaver.services.impl;

import static java.util.stream.Collectors.toCollection;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.DailySaverResult.DailySaverResultBuilder;
import it.pagopa.pn.logsaver.model.DailySaverResultList;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import it.pagopa.pn.logsaver.services.LogFileProcessorService;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.services.support.AuditSaverLogicSupport;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class AuditSaverServiceImpl implements AuditSaverService {

  private final LogFileReaderService readerService;
  private final LogFileProcessorService service;
  private final StorageService storageService;
  private final LogSaverCfg cfg;


  @Override
  public DailySaverResultList dailySaverFromLatestExecutionToYesterday(Set<LogFileType> logFileType,
      Map<Retention, Set<ExportType>> retentionExportTypeMap) {

    List<DailySaverResult> resList = new ArrayList<>();
    LocalDate yesterday = DateUtils.yesterday();
    LocalDate today = yesterday.plusDays(1);

    // non tratta yesterday diversamente dai giorni precedenti: se yesterday è stato precedentemente
    // eseguito correttamente, non lo rifà

    log.info("Start LogSaver from latest execution to Yesterday {}. Check for last execution...",
        yesterday);

    // Leggo ultima esecuzione consecutiva
    LocalDate lastContExecDate = storageService.getLatestContinuosExecutionDate();
    Map<LocalDate, StorageExecution> executionMap = new HashMap<>();
    // se yesterday-lastContExecDate > 1 sono presenti esecuzioni non processate correttamente o
    // date senza esecuzione
    if (Duration.between(lastContExecDate.atStartOfDay(), today.atStartOfDay()).toDays() >= 1) {
      // Recupero date da elaborare:
      // Leggo tutte le esecuzioni registrate da lastContExecDate a yesterday
      //
      log.info(
          "There are  previous days to be processed. Read executions after the last continuos date {}",
          lastContExecDate);

      AuditSaverLogicSupport.groupByDate(
          storageService.getStorageExecutionBetween(lastContExecDate, today), executionMap);

      List<DailyContextCfg> workList = DateUtils.getDatesRange(lastContExecDate, today).stream() //
          .map(dateToCheck -> recoveryDailyContext(dateToCheck, executionMap))
          .filter(Objects::nonNull).toList();

      log.info("There are {} previous days to be processed", workList.size());
      workList.stream().map(this::dailySaver).collect(toCollection(() -> resList));
      log.info("Processing previous days finished");
    }else{
      log.info("There are NOT days to be processed. Last continuos date: {}", lastContExecDate);
    }


    if (executionMap.containsKey(yesterday)) {
      DailyContextCfg ctx = handleDailyContext(yesterday, yesterday, executionMap, true);
      if (Objects.nonNull(ctx)) {
        resList.add(dailySaver(ctx));
      } else {
        log.info("Log date {} has already been successfully executed", yesterday);
      }
    } /*else {
      resList.add(dailySaver(DailyContextCfg.builder().logDate(yesterday)
          .retentionExportTypeMap(retentionExportTypeMap).logFileTypes(logFileType)
          .tmpBasePath(cfg.getTmpBasePath()).build()));
      // after the changes, this caused a double processing for yesterday
    }*/

    return new DailySaverResultList(resList);
  }

  @Override
  public DailySaverResultList dailyListSaver(List<LocalDate> dateExecutionList) {
    List<DailySaverResult> resList = new ArrayList<>();
    // Leggo ultima esecuzione consecutiva
    LocalDate lastContExecDate = storageService.getLatestContinuosExecutionDate();

    List<LocalDate> dateExecutionListFiltered = dateExecutionList.stream()
        .filter(date -> date.isAfter(lastContExecDate) && date.isBefore(LocalDate.now()))
        .toList();
    Optional<LocalDate> maxDate =
        dateExecutionListFiltered.stream().max(Comparator.comparing(d -> d));

    if (maxDate.isPresent()) {
      Map<LocalDate, StorageExecution> executionMap = new HashMap<>();
      AuditSaverLogicSupport.groupByDate(
          storageService.getStorageExecutionBetween(lastContExecDate, maxDate.get()), executionMap);

      List<DailyContextCfg> workList = dateExecutionListFiltered.stream() //
          .map(dateToCheck -> recoveryDailyContext(dateToCheck, executionMap))
          .filter(Objects::nonNull).toList();

      log.info("There are {} days to be processed", workList.size());
      workList.stream().map(this::dailySaver).collect(toCollection(() -> resList));
      log.info("Processing days finished");
    }

    return new DailySaverResultList(resList);
  }


  private DailyContextCfg recoveryDailyContext(LocalDate logDate,
      Map<LocalDate, StorageExecution> execList) {

    Validate.noNullElements(execList.values());

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

    log.info("handleDailyContext - logDate {}, recoveryDate {}, filterNotSent {}", logDate, recoveryDate, filterNotSent);

    //log all execList for debugging purposes
    log.info("handleDailyContext - execList SIZE {} ", execList.size());
    for (Map.Entry<LocalDate, StorageExecution> entry : execList.entrySet()) {
        log.info("handleDailyContext - execList entry: date {}, storageExecution {}", entry.getKey(), entry.getValue());
    }

    StorageExecution storExec = execList.get(recoveryDate);
    Map<Retention, Set<ExportType>> recoveryMap = AuditSaverLogicSupport
        .handleRetentionExportTypeFromStorageExecution(storExec, filterNotSent);

    recoveryMap.forEach((key, value) -> log.info("Retention {} has ExportType {} for logDate {}", key.name(),
            value.stream().map(ExportType::name).toList(), logDate));

    log.info("handleDailyContext - recoveryMap SIZE {} ", recoveryMap.size());
    return recoveryMap.isEmpty() ? null
        : DailyContextCfg.builder().logDate(logDate).tmpBasePath(cfg.getTmpBasePath())
            .logFileTypes(storExec.getLogFileTypes()).retentionExportTypeMap(recoveryMap).build();
  }



  private DailySaverResult dailySaver(DailyContextCfg dailyCtx) {

    DailySaverResultBuilder<?, ?> resBuilder =
        DailySaverResult.builder().logDate(dailyCtx.logDate());
    try {

      dailyCtx.initContext();
      log.info("Start execution for day {}", dailyCtx.logDate());

      Stream<LogFileReference> files = readerService.findLogFiles(dailyCtx);

      List<AuditFile> auditFiles = service.process(files, dailyCtx);

      List<AuditStorage> auditStorageList = storageService.store(auditFiles, dailyCtx);
      log.info("End execution for day {}", dailyCtx.logDate());
      return resBuilder.auditStorageList(auditStorageList).build();

    } catch (Exception e) {
      log.error("Error processing audit for day " + dailyCtx.logDate(), e);
      resBuilder.error(e);
      return resBuilder.build();
    } finally {
      dailyCtx.destroy();
    }
  }

  @Override
  public void dailySaverFixer() {
    try {
      log.info("Start execution daily saver fixer for day on result CREATED");
      List<AuditStorageEntity> files = readerService.findLogFilesByResult(AuditStorageStatus.CREATED.name());
      log.info("AuditStorageEntityList size after findLogFilesByResult(CREATED): {}", files.size());
      List<AuditStorage> auditStorageList = new ArrayList<>();
      files.forEach(file -> {

        log.info("Found date {} for file: {}", LocalDate.parse(file.getLogDate()), file.getStorageKey().values());
        Map<LocalDate, StorageExecution> executionMap = new HashMap<>();
        AuditSaverLogicSupport.groupByDate(
                storageService.getStorageExecutionBetween(LocalDate.parse(file.getLogDate()), LocalDate.parse(file.getLogDate())),
                executionMap
        );
        log.info("Found {} executions for file {}", executionMap.size(),  file.getStorageKey().values());

        DailyContextCfg ctx = handleDailyContext(LocalDate.parse(file.getLogDate()), LocalDate.parse(file.getLogDate()), executionMap, true);
        if (ctx != null) {
          ctx.initContext();
          log.info("DailyContext in dailySaverFixer {} ", ctx);
          Stream<LogFileReference> fileReferenceStream = readerService.findLogFiles(ctx);

          List<AuditFile> auditFiles = service.process(fileReferenceStream, ctx);
          List<AuditStorage> store = storageService.store(auditFiles, ctx, false);
          auditStorageList.addAll(store);

          log.info("End execution for file {} in date {}", file.getStorageKey().values(), ctx.logDate());
          ctx.destroy();
        } else {
            log.info("DailyContextCfg NULL: No context found for file {} in date {}", file.getStorageKey().values(), file.getLogDate());
        }
      });

      DailySaverResult.builder().auditStorageList(auditStorageList).build();

    } catch (Exception e) {
        log.error("Error processing audit for day on result CREATED", e);
      DailySaverResult.builder().error(e).build();
    }

    log.info("End execution daily saver fixer for day on result CREATED");
  }

}
