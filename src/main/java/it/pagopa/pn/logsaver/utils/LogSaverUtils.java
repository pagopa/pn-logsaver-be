package it.pagopa.pn.logsaver.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.MDC;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class LogSaverUtils {


  public static Map<Retention, Set<ExportType>> defaultRetentionExportTypeMap() {
    return Stream.of(Retention.values())
        .collect(Collectors.toMap(r -> r, r -> Set.of(ExportType.values())));
  }

  public static void initMDC(DailyContextCfg dailyCtx) {
    MDC.put("logDate", dailyCtx.logDate().toString());
  }

  public static void clearMdcFromForkThread() {
    if (Thread.currentThread() instanceof ForkJoinWorkerThread) {
      MDC.clear();
    }
  }


  /**
   * Trasforma lo stream di input in stream parallelo. incapsulamento necessario per poter scrivere
   * gli unit test. I verificationMode di Mockito non funzionano corretamente su un esecuzione
   * parallela. In questo modo il comportamento può essere pilotato con un mock su questo metodo
   * 
   * @param itemList
   * @return
   */
  public static Stream<LogFileReference> toParallelStream(List<LogFileReference> itemList) {
    return itemList.stream().parallel();
  }

  public static Stream<LogFileReference> toStream(List<LogFileReference> itemList) {
    return itemList.stream();
  }


}
