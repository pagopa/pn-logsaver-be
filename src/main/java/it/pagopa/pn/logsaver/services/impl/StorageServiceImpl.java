package it.pagopa.pn.logsaver.services.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.logsaver.dao.AuditStorageMapper;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;
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

    return new StorageExecution(exec.getLatestExecutionDate(),
        ItemType.values(exec.getTypesProcessed()), ExportType.valueOf(exec.getExportType()));
  }



  @Override
  public List<AuditStorage> store(List<AuditFile> files, DailyContextCfg cfg) {

    List<AuditStorage> auditStored = files.stream().map(this::send).collect(Collectors.toList());

    log.info("Update log-saver execution");
    storageDao.updateExecution(cfg.getLogDate(), ItemType.valuesAsString(cfg.getTypes()),
        cfg.getExportType());
    return auditStored;
  }

  private AuditStorage send(AuditFile file) {

    log.info("Sending Audit file {} ", file.filePath().getFileName().toString());
    AuditStorageEntity auditStorage = AuditStorageMapper.toEntity(file);
    AuditStorage itemUpd = safeStorageClient.uploadFile(AuditStorage.from(file));

    if (itemUpd.sendingError()) {
      log.info("Upload audit file in error");
      auditStorage.setResult(AuditStorageStatus.CREATED.name());
      auditStorage.setTmpPath(file.filePath().toString());
    } else {
      log.info("File upload successfully");
      auditStorage.setResult(AuditStorageStatus.SENT.name());
      auditStorage.setStorageKey(itemUpd.uploadKey());
    }
    log.info("Insert file audit references in db");
    storageDao.insertAudit(auditStorage);
    return AuditStorageMapper.toModel(auditStorage);

  }



}
