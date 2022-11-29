package it.pagopa.pn.logsaver.dao;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.DailyAuditDownloadable;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.StorageExecution.ExecutionDetails;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.experimental.UtilityClass;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;

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
    AuditStorageEntity entity =
        AuditStorageEntity.builder().logDate(DateUtils.format(storage.logDate()))
            .retention(storage.retention()).exportType(storage.exportType()).build();

    if (storage.hasError()) {
      entity.setResult(AuditStorageStatus.CREATED.name());
    } else {
      entity.setResult(AuditStorageStatus.SENT.name());
    }
    entity.setStorageKey(mergeStorageKeyMap(storage));
    return entity;
  }

  private static Map<String, String> mergeStorageKeyMap(AuditStorage storage) {
    Map<String, String> branch = storage.uploadKey();
    Map<String, String> main = storage.filePath().stream().collect(Collectors
        .toMap(filePath -> filePath.getFileName().toString(), filePath -> StringUtils.EMPTY));
    if (Objects.isNull(branch) || branch.isEmpty()) {
      return main;
    } else {
      branch.entrySet().stream().forEach(entry -> main.put(entry.getKey(), entry.getValue()));
      return main;
    }

  }


  public static List<DailyAuditDownloadable> toModel(Stream<AuditStorageEntity> entityStream) {
    return entityStream.collect(Collectors.groupingBy(AuditStorageEntity::getLogDate)).entrySet()
        .stream().map(entry -> new DailyAuditDownloadable(DateUtils.parse(entry.getKey()),
            AuditStorageMapper.toModel(entry.getValue())))
        .collect(Collectors.toList());
  }

  public static List<AuditDownloadReference> toModel(List<AuditStorageEntity> entityList) {
    return entityList.stream().flatMap(AuditStorageMapper::toModel).collect(Collectors.toList());
  }

  public static Stream<AuditDownloadReference> toModel(AuditStorageEntity entity) {

    return Objects.isNull(entity) ? null
        : entity.getStorageKey().entrySet().stream()
            .map(entry -> AuditDownloadReference.builder()
                .logDate(DateUtils.parse(entity.getLogDate()))
                .status(AuditStorageStatus.valueOf(entity.getResult())).fileName(entry.getKey())
                .uploadKey(entry.getValue()).build());
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


  public static Map<String, RetentionResult> toEntity(Map<Retention, Set<ExportType>> resList) {
    return resList.keySet().stream()
        .flatMap(key -> resList.get(key).stream()
            .map(ex -> new RetentionResult(key.name(), AuditStorageStatus.SENT.name(), ex.name())))
        .collect(Collectors.toMap(
            en -> en.getRetention().concat(KEY_SEPARATOR).concat(en.getExportType()),
            Function.identity()));

  }
}
