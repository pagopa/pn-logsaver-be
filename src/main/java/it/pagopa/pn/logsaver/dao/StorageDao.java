package it.pagopa.pn.logsaver.dao;

import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.Retention;


public interface StorageDao {

  List<AuditStorageEntity> getAudits(LocalDate dateFrom, LocalDate dateTo,
      List<Retention> retentions);

  ExecutionEntity latestExecution();

  ExecutionEntity updateExecution(LocalDate latestExecutionsDay, List<String> types);

  void insertAudit(AuditStorageEntity as);

  AuditStorageEntity getAudit(LocalDate dateLog, Retention retention);

}
