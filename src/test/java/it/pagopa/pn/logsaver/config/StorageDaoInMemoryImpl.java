package it.pagopa.pn.logsaver.config;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import com.google.common.collect.HashBasedTable;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.support.StorageDaoLogicSupport;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageDaoInMemoryImpl implements StorageDao {

  LocalDate FIRST_START_DAY_MOCK = DateUtils.yesterday().minusDays(3);

  HashMap<LocalDate, ExecutionEntity> execution = new HashMap<>();


  HashBasedTable<String, LocalDate, AuditStorageEntity> auditStorage = HashBasedTable.create(0, 0);

  LocalDate continuosExecution = FIRST_START_DAY_MOCK;

  @PostConstruct
  void init() {

    execution.put(LocalDate.from(FIRST_START_DAY_MOCK),
        StorageDaoLogicSupport.firstExececutionRow());
  }


  public void insertExecution(LocalDate logDate, ExecutionEntity exec) {
    execution.put(logDate, exec);
  }

  public void insertAuditStorage(AuditStorageEntity exec) {
    auditStorage.put(exec.getType(), DateUtils.parse(exec.getLogDate()), exec);
  }

  @Override
  public ExecutionEntity getLatestExecution() {
    return execution.keySet().stream().max(Comparator.comparing(d -> d)).map(d -> execution.get(d))
        .orElseThrow();
  }

  @Override
  public List<ExecutionEntity> getExecutionBetween(LocalDate dateFrom, LocalDate dateTo) {
    List<ExecutionEntity> retList = execution.entrySet().stream().filter(e -> {
      int ret = (dateFrom.compareTo(e.getKey()) * e.getKey().compareTo(dateTo));
      return ret >= 0;
    }).map(ent -> {
      return ent.getValue();
    }).collect(Collectors.toList());
    return retList;

  }

  @Override
  public LocalDate getLatestContinuosExecution() {
    return continuosExecution;
  }

  @Override
  public void updateExecution(List<AuditStorageEntity> auditList, LocalDate logDate,
      Set<LogFileType> types) {
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



  @Override
  public Stream<AuditStorageEntity> getAudits(String key, LocalDate dateFrom, LocalDate dateTo) {

    List<AuditStorageEntity> list = dateFrom.datesUntil(dateTo.plusDays(1)).filter(data -> {
      return auditStorage.columnMap().containsKey(data)
          && auditStorage.columnMap().get(data).containsKey(key);
    }).map(data -> auditStorage.columnMap().get(data).get(key)).collect(Collectors.toList());
    // auditStorage.
    return list.stream();
  }



}
