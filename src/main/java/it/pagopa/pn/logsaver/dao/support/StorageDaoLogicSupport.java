package it.pagopa.pn.logsaver.dao.support;

import it.pagopa.pn.logsaver.dao.AuditStorageMapper;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class StorageDaoLogicSupport {

  private static BigDecimal getExpirationByAuditList(List<AuditStorageEntity> auditList, Duration offsetDuration) {
    AtomicReference<Duration> maxDuration = new AtomicReference<>(Duration.ZERO);
    auditList.forEach(audit -> {
      Duration duration = Retention.valueOf(audit.getRetention()).getDuration();
      if (duration != null && duration.compareTo(maxDuration.get()) > 0) {
        maxDuration.set(duration);
      }
    });

    return BigDecimal.valueOf(OffsetDateTime.now()
            .plus(maxDuration.get()).plus(offsetDuration)
            .toInstant()
            .getEpochSecond());
  }


  public static ExecutionEntity from(List<AuditStorageEntity> auditList, LocalDate logDate, Set<LogFileType> types, Duration offsetDuration) {
    return ExecutionEntity.builder()
      .expiration(getExpirationByAuditList(auditList, offsetDuration))
      .logDate(DateUtils.format(logDate))
      .retentionResult(AuditStorageMapper.toResultExecution(auditList))
      .logFileTypes(LogFileType.valuesAsString(types)).build();
  }



  public static ExecutionEntity firstExececutionRow() {
    Map<String, RetentionResult> def = StorageDaoLogicSupport.defaultResultMap();

    return ExecutionEntity.builder().logDate(StorageDao.FIRST_START_DAY)
        .logFileTypes(LogFileType.valuesAsString()).retentionResult(def).build();
  }


  public static Map<String, RetentionResult> defaultResultMap() {
    return Stream.of(Retention.values())
        .flatMap(ret -> Stream.of(ExportType.values()).map(
            expTy -> new RetentionResult(ret.name(), AuditStorageStatus.SENT.name(), expTy.name())))
        .collect(Collectors.toMap(RetentionResult::getKey, Function.identity()));
  }

  public static LocalDate computeLastContinuosExecutionDate(LocalDate dateFrom,
      List<ExecutionEntity> execList) {

    Optional<LocalDate> lastExecutionDate = execList.stream()
        .map(ex -> DateUtils.parse(ex.getLogDate())).max(Comparator.comparing(d -> d));

    if (lastExecutionDate.isPresent()) {
      Map<LocalDate, ExecutionEntity> execMap = groupByDate(execList);

      return DateUtils.getDatesRange(dateFrom, lastExecutionDate.get().plusDays(1)).stream()
          .takeWhile(date -> execMap.containsKey(date) && !hasErrors(execMap.get(date)))
          .max(Comparator.comparing(d -> d)).orElse(dateFrom);
    } else {
      return dateFrom;
    }
  }

  public static boolean hasErrors(ExecutionEntity storExec) {
    return storExec.getRetentionResult().values().stream()
        .filter(detail -> !detail.getResult().equals(AuditStorageStatus.SENT.name())).count() > 0;
  }

  private static Map<LocalDate, ExecutionEntity> groupByDate(List<ExecutionEntity> execList) {
    return execList.stream()
        .collect(Collectors.toMap(ex -> DateUtils.parse(ex.getLogDate()), Function.identity()));
  }

  public static Map<String, RetentionResult> mergeRetentionResult(Map<String, RetentionResult> main,
      Map<String, RetentionResult> branch) {
    if (Objects.isNull(main)) {
      return branch;
    } else {
      branch.entrySet().stream().forEach(entry -> main.put(entry.getKey(), entry.getValue()));
      return main;
    }

  }
}
