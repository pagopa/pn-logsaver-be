package it.pagopa.pn.logsaver.command.base;

import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.LogSaverResult;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class CommandRunner {

  @Autowired
  private final Map<String, Command> commands;
  private final SimpleAsyncTaskExecutor executor;
  private final AuditSaverService auditSaverService;


  public void run(ClApplicationArguments args) {
//    log.info("Run command {}", args.getCommand().getCommandName());
//    Command commandImpl = commands.get(args.getCommand().getCommandName());
//
//    ListenableFuture<LogSaverResult> res =
//        executor.submitListenable(() -> commandImpl.execute(args));
//    res.addCallback(commandImpl);

    log.info("Run command {} - dailySaverFixer" , args.getCommand());
    auditSaverService.dailySaverFixer();
  }


}
