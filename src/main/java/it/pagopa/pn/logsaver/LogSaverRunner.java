package it.pagopa.pn.logsaver;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.config.LogSaverRunnerCallback;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class LogSaverRunner implements ApplicationRunner {

  private final SimpleAsyncTaskExecutor executor;

  private final LogSaverRunnerCallback logSaverRunnerCallback;

  private final AuditSaverService logSaver;

  private final ClApplicationArguments appArgs;


  @Override
  public void run(ApplicationArguments args) throws Exception {

    ListenableFuture<List<DailySaverResult>> res = executor.submitListenable(this::startLogSaver);

    res.addCallback(logSaverRunnerCallback);
  }

  private List<DailySaverResult> startLogSaver() {
    List<DailySaverResult> results;

    if (appArgs.getDateList().isEmpty()) {
      results = logSaver.dailySaverFromLatestExecutionToYesterday(appArgs.getItemTypes(),
          appArgs.getRetentionExportTypesMap());
    } else {
      results = logSaver.dailyListSaver(appArgs.getDateList());
    }

    return results;
  }

}

