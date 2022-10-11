package it.pagopa.pn.logsaver.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.LogFileType;


public interface StorageDao {
  public static final String FIRST_START_DAY = "2022-10-01";

  ExecutionEntity getLatestExecution();

  List<ExecutionEntity> getExecutionBetween(LocalDate dateFrom, LocalDate dateTo);

  LocalDate getLatestContinuosExecution();

  void updateExecution(List<AuditStorageEntity> auditList, LocalDate logDate, Set<LogFileType> types);
}
