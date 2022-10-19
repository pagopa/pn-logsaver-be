package it.pagopa.pn.logsaver.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.command.base.BaseCommand;
import it.pagopa.pn.logsaver.command.base.Commands;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.DailySaverResultList;
import it.pagopa.pn.logsaver.services.AuditSaverService;

@Service(Commands.DAILY_LOGSAVER_S)
public class DailyLogSaverCommand extends BaseCommand {

  protected final AuditSaverService logSaver;

  public DailyLogSaverCommand(ApplicationEventPublisher eventPublisher,
      AuditSaverService logSaver) {
    super(eventPublisher);
    this.logSaver = logSaver;
  }


  @Override
  public DailySaverResultList execute(ClApplicationArguments args) {
    return logSaver.dailySaverFromLatestExecutionToYesterday(args.getLogFileTypes(),
        args.getRetentionExportTypesMap());
  }


}
