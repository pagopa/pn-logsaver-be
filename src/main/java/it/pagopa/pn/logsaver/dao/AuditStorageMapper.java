package it.pagopa.pn.logsaver.dao;

import java.nio.file.Path;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditStorageMapper {


  public static AuditStorageEntity toEntity(AuditFile file) {
    return Objects.isNull(file) ? null
        : AuditStorageEntity.builder()
            .fileName(
                Objects.nonNull(file.filePath()) ? file.filePath().getFileName().toString() : null)
            .logDate(DateUtils.format(file.logDate())).type(file.retention().name()).build();
  }

  public static AuditStorage toModel(AuditStorageEntity entity) {
    return Objects.isNull(entity) ? null
        : AuditStorage.builder().retention(Retention.valueOf(entity.getType()))
            .logDate(DateUtils.parse(entity.getLogDate()))
            .filePath(
                StringUtils.isEmpty(entity.getTmpPath()) ? null : Path.of(entity.getTmpPath()))
            .status(AuditStorageStatus.valueOf(entity.getResult())).build();
  }
}
