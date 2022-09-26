package it.pagopa.pn.logsaver.dao.support;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StorageDaoLogicSupport {



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
}
