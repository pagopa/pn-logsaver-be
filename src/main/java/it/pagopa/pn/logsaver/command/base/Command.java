package it.pagopa.pn.logsaver.command.base;

import org.springframework.util.concurrent.ListenableFutureCallback;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.LogSaverResult;


public interface Command extends ListenableFutureCallback<LogSaverResult> {

  LogSaverResult execute(ClApplicationArguments args);

}
