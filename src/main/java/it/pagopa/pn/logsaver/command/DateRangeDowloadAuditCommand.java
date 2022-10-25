package it.pagopa.pn.logsaver.command;

import java.time.LocalDate;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.command.base.BaseCommand;
import it.pagopa.pn.logsaver.command.base.Commands;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.model.DailyDownloadResultList;
import it.pagopa.pn.logsaver.services.AuditDownloadService;

@Service(Commands.DATERANGE_DOWNLOAD_AUDIT_S)
public class DateRangeDowloadAuditCommand extends BaseCommand {

  protected final AuditDownloadService service;

  public DateRangeDowloadAuditCommand(ApplicationEventPublisher eventPublisher,
      AuditDownloadService service) {
    super(eventPublisher);
    this.service = service;
  }

  @Override
  public DailyDownloadResultList execute(ClApplicationArguments args) {
    LocalDate dateFrom = args.getDateFrom();
    LocalDate dateTo = args.getDateTo();
    Validate.isTrue(dateFrom.isBefore(dateTo) || dateFrom.isEqual(dateTo),
        "dateFrom argument must be less than or equal to dateTo argument.");
    return service.downloadAudits(dateFrom, dateTo, args.getDownloadFolder());
  }


}
