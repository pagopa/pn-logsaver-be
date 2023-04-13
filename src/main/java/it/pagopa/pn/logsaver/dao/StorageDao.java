package it.pagopa.pn.logsaver.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.enums.LogFileType;


public interface StorageDao {
  public static final String FIRST_START_DAY = "2023-01-01"; //"2022-11-01"

  ExecutionEntity getLatestExecution();

  List<ExecutionEntity> getExecutionBetween(LocalDate dateFrom, LocalDate dateTo);

  LocalDate getLatestContinuosExecution();

  void updateExecution(List<AuditStorageEntity> auditList, LocalDate logDate,
      Set<LogFileType> types);

  Stream<AuditStorageEntity> getAudits(String key, LocalDate dateFrom, LocalDate dateTo);
}
