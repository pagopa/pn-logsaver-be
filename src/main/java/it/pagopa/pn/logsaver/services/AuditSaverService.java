package it.pagopa.pn.logsaver.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.LogFileType;
import it.pagopa.pn.logsaver.model.Retention;

public interface AuditSaverService {

  List<DailySaverResult> dailyListSaver(List<LocalDate> dateExecutionList);

  List<DailySaverResult> dailySaverFromLatestExecutionToYesterday(Set<LogFileType> logFileTypes,
      Map<Retention, Set<ExportType>> retentionExportTypeMap);

}
