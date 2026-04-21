package it.pagopa.pn.logsaver.command.base;

import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.LogSaverResult;


public interface Command {

  LogSaverResult execute(ClApplicationArguments args);
  void onSuccess(LogSaverResult result);
  void onFailure(Throwable ex);

}
