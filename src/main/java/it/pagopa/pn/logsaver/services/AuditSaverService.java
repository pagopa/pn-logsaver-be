package it.pagopa.pn.logsaver.services;

import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;

public interface AuditSaverService {

  List<DailySaverResult> dailySaverFromLatestExecutionToYesterday(ExportType ExportType);


  DailySaverResult dailySaver(DailyContextCfg dailyCtx);

  List<DailySaverResult> dailyListSaver(List<LocalDate> dateExecutionList, List<ItemType> types,
      ExportType exportType);

}
