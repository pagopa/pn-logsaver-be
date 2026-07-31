package it.pagopa.pn.logsaver.services;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.UnaryOperator;

import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.DailyAuditDownloadable;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;

public interface StorageService {

  List<AuditStorage> store(List<AuditFile> files, DailyContextCfg cfg);

  List<AuditStorage> store(List<AuditFile> files, DailyContextCfg cfg, boolean continuousExecutionUpdate);

  String uploadPart(Path part, Retention retention, ExportType exportType);

  List<AuditStorage> persist(List<AuditStorage> uploaded, DailyContextCfg cfg,
      boolean continuousExecutionUpdate);

  StorageExecution getLatestStorageExecution();

  LocalDate getLatestContinuosExecutionDate();

  List<StorageExecution> getStorageExecutionBetween(LocalDate from, LocalDate to);

  List<DailyAuditDownloadable> getAuditFile(LocalDate from, LocalDate to);

  AuditDownloadReference dowloadAuditFile(AuditDownloadReference audit,
      UnaryOperator<AuditDownloadReference> downloadFunction);


  List<AuditStorageEntity> findAuditStorageByResult(String result);
}
