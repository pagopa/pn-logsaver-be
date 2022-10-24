package it.pagopa.pn.logsaver.services;

import java.time.LocalDate;
import it.pagopa.pn.logsaver.model.DailyDownloadResultList;


public interface AuditDownloadService {

  DailyDownloadResultList downloadAudits(LocalDate from, LocalDate to, String destinationFolder);

}
