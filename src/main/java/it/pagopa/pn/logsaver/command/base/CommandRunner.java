package it.pagopa.pn.logsaver.command.base;

import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    Validate.notNull(commandImpl, "Error in command name", args.getCommand().getCommandName());
    CompletableFuture.supplyAsync(() -> commandImpl.execute(args), executor)
        .thenAccept(commandImpl::onSuccess);
  }

}
