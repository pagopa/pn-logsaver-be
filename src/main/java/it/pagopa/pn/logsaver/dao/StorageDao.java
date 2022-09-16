package it.pagopa.pn.logsaver.dao;

import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.dao.entity.AuditStorage;
import it.pagopa.pn.logsaver.dao.entity.Execution;
import it.pagopa.pn.logsaver.model.Retention;


public interface StorageDao {

  List<AuditStorage> getAudits(LocalDate dateFrom, LocalDate dateTo, List<Retention> retentions);

  Execution latestExecution();

  Execution updateLatestExecution(LocalDate latestExecutionsDay, List<String> types);

  void insertAudit(AuditStorage as);

}
