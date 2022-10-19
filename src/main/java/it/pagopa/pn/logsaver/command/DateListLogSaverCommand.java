package it.pagopa.pn.logsaver.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.command.base.Commands;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.DailySaverResultList;
import it.pagopa.pn.logsaver.services.AuditSaverService;

@Service(Commands.DATELIST_LOGSAVER_S)
public class DateListLogSaverCommand extends DailyLogSaverCommand {


  public DateListLogSaverCommand(ApplicationEventPublisher eventPublisher,
      AuditSaverService logSaver) {
    super(eventPublisher, logSaver);
  }


  @Override
  public DailySaverResultList execute(ClApplicationArguments args) {
    return logSaver.dailyListSaver(args.getDateList());
  }



}
