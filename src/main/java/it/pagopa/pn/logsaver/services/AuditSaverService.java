package it.pagopa.pn.logsaver.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import it.pagopa.pn.logsaver.model.DailySaverResultList;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;

public interface AuditSaverService {

  DailySaverResultList dailyListSaver(List<LocalDate> dateExecutionList);

  DailySaverResultList dailySaverFromLatestExecutionToYesterday(Set<LogFileType> logFileTypes,
      Map<Retention, Set<ExportType>> retentionExportTypeMap);

}
