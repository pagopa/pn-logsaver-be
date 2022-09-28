package it.pagopa.pn.logsaver.dao;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.StorageExecution.ExecutionDetails;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditStorageMapper {
  public static final String KEY_SEPARATOR = "$";


  public static Map<String, RetentionResult> toResultExecution(List<AuditStorageEntity> auditList) {
    return CollectionUtils.emptyIfNull(auditList).stream()
        .map(au -> new RetentionResult(au.getRetention(), au.getResult(), au.getExportType()))
        .collect(Collectors.toMap(RetentionResult::getKey, Function.identity()));
  }



  public static AuditStorageEntity toEntity(AuditStorage storage) {

    if (Objects.isNull(storage)) {
      return null;
    }
    AuditStorageEntity entity = AuditStorageEntity.builder().fileName(storage.fileName())
        .logDate(DateUtils.format(storage.logDate())).retention(storage.retention())
        .exportType(storage.exportType()).build();

    if (storage.sendingError()) {
      entity.setResult(AuditStorageStatus.CREATED.name());
    } else {
      entity.setResult(AuditStorageStatus.SENT.name());
      entity.setStorageKey(storage.uploadKey());
    }
    return entity;
  }


  public static StorageExecution toModel(ExecutionEntity entity) {
    return new StorageExecution(DateUtils.parse(entity.getLogDate()),
        ItemType.values(entity.getItemTypes()), toModel(entity.getRetentionResult()));
  }

  public static List<ExecutionDetails> toModel(Map<String, RetentionResult> resList) {
    return MapUtils.emptyIfNull(resList).values().stream()
        .map(au -> new ExecutionDetails(Retention.valueOf(au.getRetention()),
            AuditStorageStatus.valueOf(au.getResult()), ExportType.valueOf(au.getExportType())))
        .collect(Collectors.toList());
  }

}
