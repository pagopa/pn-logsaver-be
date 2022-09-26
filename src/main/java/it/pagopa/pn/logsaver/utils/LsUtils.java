package it.pagopa.pn.logsaver.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.collect.HashBasedTable;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Retention;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class LsUtils {

  static HashBasedTable<Retention, ExportType, String> table;
  static {
    table = HashBasedTable.create(0, 0);
    table.put(Retention.AUDIT10Y, ExportType.PDF_SIGNED, "PN_LOGS_PDF_AUDIT10Y");
    table.put(Retention.AUDIT5Y, ExportType.PDF_SIGNED, "PN_LOGS_PDF_AUDIT5Y");
    table.put(Retention.DEVELOPER, ExportType.PDF_SIGNED, "PN_LOGS_PDF_TEMP");

    table.put(Retention.AUDIT10Y, ExportType.ZIP, "PN_LOGS_ARCHIVE_AUDIT10Y");
    table.put(Retention.AUDIT5Y, ExportType.ZIP, "PN_LOGS_ARCHIVE_AUDIT5Y");
    table.put(Retention.DEVELOPER, ExportType.ZIP, "PN_LOGS_ARCHIVE_TEMP");
  }


  public static String getStorageDocumentType(AuditStorage audit) {
    return table.get(audit.retention(), audit.exportType());
  }

  public static Map<Retention, Set<ExportType>> defaultRetentionExportTypeMap() {
    return Stream.of(Retention.values())
        .collect(Collectors.toMap(r -> r, r -> Set.of(ExportType.values())));
  }

  //
  public static <T extends Object> Function<T, T> peek(Consumer<T> action) {
    return e -> {
      action.accept(e);
      return e;
    };
  }

  public static int exitCodeAndLogResult(List<DailySaverResult> results) {
    long exitCode = results.stream().map(resDaily -> {
      log.info(resDaily.toString());
      resDaily.successMessages().stream().forEach(log::info);
      resDaily.errorMessages().stream().forEach(log::info);
      return resDaily;
    }).filter(DailySaverResult::hasErrors).count();
    exitCode = exitCode > 0 ? 1 : 0;
    log.info("Log Saver Applicantion ends with status as {}", exitCode);
    return Math.toIntExact(exitCode);
  }


}
