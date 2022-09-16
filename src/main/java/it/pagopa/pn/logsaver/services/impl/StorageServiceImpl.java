package it.pagopa.pn.logsaver.services.impl;

import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorage;
import it.pagopa.pn.logsaver.dao.entity.Execution;
import it.pagopa.pn.logsaver.model.AuditContainer;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.ItemUpload;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class StorageServiceImpl implements StorageService {

  private final PnSafeStorageClient safeStorageClient;

  private final StorageDao storageDao;


  @Override
  public StorageExecution latestStorageExecution() {

    Execution exec = storageDao.latestExecution();

    return new StorageExecution(exec.getLatestExecutionDate(),
        ItemType.values(exec.getTypesProcessed()));

  }



  @Override
  public void store(List<AuditContainer> files, DailyContextCfg cfg) {

    files.stream().forEach(this::send);

    storageDao.updateLatestExecution(cfg.getLogDate(), ItemType.valuesAsString(cfg.getTypes()));

  }

  private void send(AuditContainer file) {

    ItemUpload itemUpd = safeStorageClient.uploadFile(ItemUpload.from(file));

    AuditStorage auditStorage =
        AuditStorage.builder().fileName(FilenameUtils.getName(file.filePath().toString()))
            .logDate(DateUtils.format(file.logDate())).type(file.retention().name())
            .storageKey(itemUpd.uploadKey()).build();

    storageDao.insertAudit(auditStorage);
    // TODO UPD DynamoDb
  }



}
