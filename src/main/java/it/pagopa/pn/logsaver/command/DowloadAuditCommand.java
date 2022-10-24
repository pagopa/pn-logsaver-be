package it.pagopa.pn.logsaver.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.command.base.BaseCommand;
import it.pagopa.pn.logsaver.command.base.Commands;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.DailyDownloadResultList;
import it.pagopa.pn.logsaver.services.AuditDownloadService;

@Service(Commands.DOWNLOAD_AUDIT_S)
public class DowloadAuditCommand extends BaseCommand {

  protected final AuditDownloadService service;

  public DowloadAuditCommand(ApplicationEventPublisher eventPublisher,
      AuditDownloadService service) {
    super(eventPublisher);
    this.service = service;
  }

  @Override
  public DailyDownloadResultList execute(ClApplicationArguments args) {
    return service.downloadAudits(args.getDate(), args.getDate(), args.getDownloadFolder());
  }


}
