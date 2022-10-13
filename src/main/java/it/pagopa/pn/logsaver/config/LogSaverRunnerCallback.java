package it.pagopa.pn.logsaver.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;
import it.pagopa.pn.logsaver.config.LogSaverConfiguration.ExitEvent;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class LogSaverRunnerCallback implements ListenableFutureCallback<List<DailySaverResult>> {

  @Autowired
  private final ApplicationEventPublisher eventPublisher;


  @Override
  public void onFailure(Throwable ex) {
    log.error("Faliure executing log saver: " + ex.getMessage(), ex);
    eventPublisher.publishEvent(new ExitEvent(1));
  }

  @Override
  public void onSuccess(List<DailySaverResult> result) {
    int exitCode = LogSaverUtils.exitCodeAndLogResult(result);

    eventPublisher.publishEvent(new ExitEvent(exitCode));
  }
}
