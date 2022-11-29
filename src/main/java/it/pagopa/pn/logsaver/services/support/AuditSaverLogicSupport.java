package it.pagopa.pn.logsaver.services.support;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.StorageExecution.ExecutionDetails;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditSaverLogicSupport {

  public static void groupByDate(List<StorageExecution> execList,
      Map<LocalDate, StorageExecution> ret) {
    execList.stream().collect(
            Collectors.toMap(StorageExecution::getLogDate,
                            Function.identity(),
                            (a, b) -> a,
                    () -> ret));
  }



  public static Map<Retention, Set<ExportType>> handleRetentionExportTypeFromStorageExecution(
      StorageExecution storExec, boolean filterNotSent) {

    return storExec.getDetails().stream()
        .filter(detail -> !filterNotSent || detail.getStatus() != AuditStorageStatus.SENT)
        .collect(groupingBy(ExecutionDetails::getRetention, collectingAndThen(toList(), list -> list
            .stream().map(ExecutionDetails::getExportType).distinct().collect(toSet()))));
  }

}
