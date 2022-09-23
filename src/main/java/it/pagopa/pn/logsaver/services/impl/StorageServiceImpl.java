package it.pagopa.pn.logsaver.services.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.logsaver.dao.AuditStorageMapper;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.services.StorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class StorageServiceImpl implements StorageService {

  private final PnSafeStorageClient safeStorageClient;

  private final StorageDao storageDao;


  public AuditStorage get(Retention type, LocalDate logDate) {

    AuditStorageEntity auditStoreEntity = storageDao.getAudit(logDate, type);

    return AuditStorageMapper.toModel(auditStoreEntity);
  }

  @Override
  public StorageExecution latestStorageExecution() {

    ExecutionEntity exec = storageDao.latestExecution();

    return AuditStorageMapper.toModel(exec);
  }



  @Override
  public List<AuditStorage> store(List<AuditFile> files, DailyContextCfg cfg) {

    List<AuditStorage> auditStored = files.stream().map(this::send).collect(Collectors.toList());

    List<AuditStorageEntity> auditStoredEntity =
        auditStored.stream().map(AuditStorageMapper::toEntity).collect(Collectors.toList());
    log.info("Update log-saver execution");
    storageDao.updateExecution(auditStoredEntity, cfg.logDate(), cfg.itemTypes(), cfg.exportTypes(),
        cfg.retentions());

    return auditStored;
  }

  private AuditStorage send(AuditFile file) {

    log.info("Sending Audit file {} ", file.filePath().getFileName().toString());
    AuditStorage itemUpd = safeStorageClient.uploadFile(AuditStorage.from(file));
    log.info("Success {} ", !itemUpd.sendingError());
    return itemUpd;

  }

  @Override
  public LocalDate latestContinuosExecutionDate() {
    return storageDao.latestContinuosExecution();
  }

  @Override
  public Map<LocalDate, StorageExecution> storageExecutionBetween(LocalDate from, LocalDate to) {
    return storageDao.executionBetween(from, to).stream().map(AuditStorageMapper::toModel)
        .collect(Collectors.toMap(StorageExecution::getLogDate, Function.identity()));
  }



}
