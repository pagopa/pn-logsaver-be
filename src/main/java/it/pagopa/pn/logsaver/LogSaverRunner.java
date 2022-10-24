package it.pagopa.pn.logsaver;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.command.base.CommandRunner;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class LogSaverRunner implements ApplicationRunner {

  private final CommandRunner runner;
  private final ClApplicationArguments appArgs;


  @Override
  public void run(ApplicationArguments args) throws Exception {

    runner.run(appArgs);
  }



}

