package it.pagopa.pn.logsaver.command.base;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.LogSaverResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class CommandRunner {

  @Autowired
  private final Map<String, Command> commands;
  private final SimpleAsyncTaskExecutor executor;


  public void run(ClApplicationArguments args) {
    log.info("Run command {}", args.getCommand().getCommandName());
    Command commandImpl = commands.get(args.getCommand().getCommandName());

    ListenableFuture<LogSaverResult> res =
        executor.submitListenable(() -> commandImpl.execute(args));
    res.addCallback(commandImpl);
  }


}
