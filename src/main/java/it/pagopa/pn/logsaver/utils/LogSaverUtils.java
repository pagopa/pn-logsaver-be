package it.pagopa.pn.logsaver.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Retention;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class LogSaverUtils {


  public static Map<Retention, Set<ExportType>> defaultRetentionExportTypeMap() {
    return Stream.of(Retention.values())
        .collect(Collectors.toMap(r -> r, r -> Set.of(ExportType.values())));
  }


  /**
   * Trasforma lo stream di input in stream parallelo. incapsulamento necessario per poter scrivere
   * gli unit test. I verificationMode di Mockito non funzionano corretamente su un esecuzione
   * parallela. In questo modo il comportamento può essere pilotato con un mock su questo metodo
   * 
   * @param itemStream
   * @return
   */
  public static Stream<Item> toParallelStream(List<Item> itemStream) {
    return itemStream.stream().parallel();
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
