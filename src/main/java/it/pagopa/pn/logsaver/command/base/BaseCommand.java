package it.pagopa.pn.logsaver.command.base;


import org.springframework.context.ApplicationEventPublisher;
import it.pagopa.pn.logsaver.config.LogSaverConfiguration.ExitEvent;
import it.pagopa.pn.logsaver.model.LogSaverResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public abstract class BaseCommand implements Command {

  public final ApplicationEventPublisher eventPublisher;

  @Override
  public void onFailure(Throwable ex) {
    log.error("Failure executing log saver: " + ex.getMessage(), ex);
    eventPublisher.publishEvent(new ExitEvent(1));
  }

  @Override
  public void onSuccess(LogSaverResult result) {
    eventPublisher.publishEvent(new ExitEvent(result.exitCodeAndLogResult()));

  }



}
