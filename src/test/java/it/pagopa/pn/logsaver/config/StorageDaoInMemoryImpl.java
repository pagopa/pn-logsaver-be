package it.pagopa.pn.logsaver.config;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.support.StorageDaoLogicSupport;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.utils.DateUtils;


public class StorageDaoInMemoryImpl implements StorageDao {

  LocalDate FIRST_START_DAY_MOCK = DateUtils.yesterday().minusDays(3);

  HashMap<LocalDate, ExecutionEntity> execution = new HashMap<>();

  LocalDate continuosExecution = FIRST_START_DAY_MOCK;

  @PostConstruct
  void init() {

    execution.put(LocalDate.from(FIRST_START_DAY_MOCK),
        StorageDaoLogicSupport.firstExececutionRow());
  }



  @Override
  public ExecutionEntity getLatestExecution() {
    return execution.keySet().stream().max(Comparator.comparing(d -> d)).map(d -> execution.get(d))
        .orElseThrow();
  }

  @Override
  public List<ExecutionEntity> getExecutionBetween(LocalDate dateFrom, LocalDate dateTo) {
    return execution.entrySet().stream()
        .filter(e -> (dateFrom.compareTo(e.getKey()) * e.getKey().compareTo(dateTo) >= 0))
        .map(Entry::getValue).collect(Collectors.toList());

  }

  @Override
  public LocalDate getLatestContinuosExecution() {
    return continuosExecution;
  }

  @Override
  public void updateExecution(List<AuditStorageEntity> auditList, LocalDate logDate,
      Set<ItemType> types) {
    LocalDate lastContinuosExecutionReg = getLatestContinuosExecution();

    ExecutionEntity newExecution = StorageDaoLogicSupport.from(auditList, logDate, types);

    if (!StorageDaoLogicSupport.hasErrors(newExecution) && Duration
        .between(lastContinuosExecutionReg.atStartOfDay(), logDate.atStartOfDay()).toDays() == 1) {
      // Determino la data esecuzione continua
      List<ExecutionEntity> execList = this.getExecutionBetween(logDate, logDate);
      LocalDate lastContinuosExecutionDate =
          StorageDaoLogicSupport.computeLastContinuosExecutionDate(logDate, execList);

      this.continuosExecution = lastContinuosExecutionDate;


    }
    if (execution.containsKey(logDate)) {
      newExecution.getRetentionResult().forEach((key, value) -> execution.get(logDate)
          .getRetentionResult().merge(key, value, (v1, v2) -> v2));
    } else {
      execution.put(logDate, newExecution);
    }

  }



}
