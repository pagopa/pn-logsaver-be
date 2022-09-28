package it.pagopa.pn.logsaver.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.ItemType;


public interface StorageDao {
  public static final String FIRST_START_DAY = "2022-07-07";

  ExecutionEntity latestExecution();

  List<ExecutionEntity> executionBetween(LocalDate dateFrom, LocalDate dateTo);

  LocalDate latestContinuosExecution();

  void updateExecution(List<AuditStorageEntity> auditList, LocalDate logDate, Set<ItemType> types);
}
