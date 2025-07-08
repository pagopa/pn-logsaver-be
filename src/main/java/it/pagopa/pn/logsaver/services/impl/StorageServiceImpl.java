package it.pagopa.pn.logsaver.services.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import it.pagopa.pn.logsaver.exceptions.InternalException;
import it.pagopa.pn.logsaver.model.enums.Retention;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.logsaver.dao.AuditStorageMapper;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.DailyAuditDownloadable;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import it.pagopa.pn.logsaver.services.StorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Slf4j
@Service
@AllArgsConstructor
public class StorageServiceImpl implements StorageService {

  private final PnSafeStorageClient safeStorageClient;

  private final StorageDao storageDao;

  @Override
  public List<DailyAuditDownloadable> getAuditFile(LocalDate from, LocalDate to) {
    log.info("Read execution from {} to {}", from.toString(), to.toString());
    List<ExecutionEntity> executionList = storageDao.getExecutionBetween(from, to);
    // Raggruppo le partizioni dove sono memorizzate le informazioni dei file
    Set<String> partitionSet =
        executionList.stream().flatMap(exec -> exec.getRetentionResult().keySet().stream())
            .distinct().collect(Collectors.toSet());
    // Per ogni partizione leggo le info dei file per il range di date specificato
    List<DailyAuditDownloadable> resList = AuditStorageMapper
        .toModel(partitionSet.stream().flatMap(key -> storageDao.getAudits(key, from, to)));
    // Per i file che hanno la chiave di safeStorage recupero le info per il download
    resList.stream().flatMap(daily -> daily.audits().stream())
        .filter(audit -> AuditStorageStatus.SENT == audit.status())
        .forEach(safeStorageClient::downloadFileInfo);
    return resList;
  }

  @Override
  public AuditDownloadReference dowloadAuditFile(AuditDownloadReference audit,
      UnaryOperator<AuditDownloadReference> downloadFunction) {
    return safeStorageClient.downloadFile(audit, downloadFunction);
  }

  @Override
  public StorageExecution getLatestStorageExecution() {

    ExecutionEntity exec = storageDao.getLatestExecution();

    return AuditStorageMapper.toModel(exec);
  }

  @Override
  public LocalDate getLatestContinuosExecutionDate() {
    return storageDao.getLatestContinuosExecution();
  }

  @Override
  public List<StorageExecution> getStorageExecutionBetween(LocalDate from, LocalDate to) {
    return storageDao.getExecutionBetween(from, to).stream().map(AuditStorageMapper::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public List<AuditStorage> store(List<AuditFile> files, DailyContextCfg ctx) {
    return store(files, ctx, true);
  }

  @Override
  public List<AuditStorage> store(List<AuditFile> files, DailyContextCfg ctx, boolean continuousExecutionUpdate) {

    List<AuditStorage> auditStored = files.stream().map(this::send).collect(Collectors.toList());

    List<AuditStorageEntity> auditStoredEntity =
            auditStored.stream().map(AuditStorageMapper::toEntity).collect(Collectors.toList());
    log.info("Update log-saver execution");
    storageDao.updateExecution(auditStoredEntity, ctx.logDate(), ctx.logFileTypes(), continuousExecutionUpdate);

    return auditStored;
  }

  private AuditStorage send(AuditFile file) {

    log.info("Sending Audit file retention {} ", file.retention());
    AuditStorage itemUpd = safeStorageClient.uploadFiles(AuditStorage.from(file));
    log.info("Sent: {} ", !itemUpd.hasError());
    return itemUpd;

  }

  /**
   * Per ogni retentionType estrae da SS tutti i record con uno specifico result
   *
   * @param result il risultato da cercare
   * @return Lista di AuditStorageEntity con il risultato specificato
   */
  public List<AuditStorageEntity> findAuditStorageByResult(String result) {

    List<AuditStorageEntity> auditStorageEntityList = new ArrayList<>();
    try {
      //Ciclo sulla partition -> retentionType
      for (Retention retentionType : Retention.values()) {
        log.info("Storage Service - retentionType {} ", retentionType.name());

        List<AuditStorageEntity> entityList = (storageDao.getAuditsByResult(retentionType.name(), result)).collect(Collectors.toList());
        for (AuditStorageEntity entity : entityList)
          auditStorageEntityList.add(entity);
      }
    } catch (DynamoDbException e) {
      log.error("Unable to get item from DynamoDB: {}", e.getMessage(), e);
      throw new InternalException("GetItem failed: LogSaver DynamoDB Exception");

    }
    return auditStorageEntityList;
  }
}
