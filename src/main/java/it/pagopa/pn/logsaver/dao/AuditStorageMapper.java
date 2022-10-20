package it.pagopa.pn.logsaver.dao;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.DailyAuditDownloadable;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.StorageExecution.ExecutionDetails;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
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

    if (storage.haveError()) {
      entity.setResult(AuditStorageStatus.CREATED.name());
    } else {
      entity.setResult(AuditStorageStatus.SENT.name());
      entity.setStorageKey(storage.uploadKey());
    }
    return entity;
  }

  public static List<DailyAuditDownloadable> toModel(Stream<AuditStorageEntity> entityStream) {
    return entityStream.collect(Collectors.groupingBy(AuditStorageEntity::getLogDate)).entrySet()
        .stream().map(entry -> new DailyAuditDownloadable(DateUtils.parse(entry.getKey()),
            AuditStorageMapper.toModel(entry.getValue())))
        .collect(Collectors.toList());
  }

  public static List<AuditDownloadReference> toModel(List<AuditStorageEntity> entityList) {
    return entityList.stream().map(AuditStorageMapper::toModel).collect(Collectors.toList());
  }

  public static AuditDownloadReference toModel(AuditStorageEntity entity) {
    return Objects.isNull(entity) ? null
        : AuditDownloadReference.builder().retention(Retention.valueOf(entity.getRetention()))
            .exportType(ExportType.valueOf(entity.getContentType())).fileName(entity.getFileName())
            .logDate(DateUtils.parse(entity.getLogDate())).uploadKey(entity.getStorageKey())
            .status(AuditStorageStatus.valueOf(entity.getResult())).build();
  }


  public static StorageExecution toModel(ExecutionEntity entity) {
    return new StorageExecution(DateUtils.parse(entity.getLogDate()),
        LogFileType.values(entity.getLogFileTypes()), toModel(entity.getRetentionResult()));
  }

  public static List<ExecutionDetails> toModel(Map<String, RetentionResult> resList) {
    return MapUtils.emptyIfNull(resList).values().stream()
        .map(au -> new ExecutionDetails(Retention.valueOf(au.getRetention()),
            AuditStorageStatus.valueOf(au.getResult()), ExportType.valueOf(au.getExportType())))
        .collect(Collectors.toList());
  }

}
