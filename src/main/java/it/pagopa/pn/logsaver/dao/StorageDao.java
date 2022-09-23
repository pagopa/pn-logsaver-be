package it.pagopa.pn.logsaver.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;


public interface StorageDao {
  public static final String FIRST_START_DAY = "2022-07-10";

  List<AuditStorageEntity> getAudits(LocalDate dateFrom, LocalDate dateTo,
      Set<Retention> retentions);

  ExecutionEntity latestExecution();

  List<ExecutionEntity> executionBetween(LocalDate dateFrom, LocalDate dateTo);


  void insertAudit(AuditStorageEntity as);

  AuditStorageEntity getAudit(LocalDate dateLog, Retention retention);

  LocalDate latestContinuosExecution();

  void updateExecution(List<AuditStorageEntity> auditList, LocalDate day, Set<ItemType> types,
      Set<ExportType> typeExport, Set<Retention> retentions);
}
