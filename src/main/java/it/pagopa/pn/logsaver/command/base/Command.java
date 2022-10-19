package it.pagopa.pn.logsaver.command.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.ListenableFutureCallback;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.LogSaverResult;


public interface Command extends ListenableFutureCallback<LogSaverResult> {
  static final Logger log = LoggerFactory.getLogger(Command.class);

  LogSaverResult execute(ClApplicationArguments args);

}
