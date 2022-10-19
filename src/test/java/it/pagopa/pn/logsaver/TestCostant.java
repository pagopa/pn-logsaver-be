package it.pagopa.pn.logsaver;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;

public final class TestCostant {

  public static final String FILE_LOG =
      "classpath:files/s3/pn-pnDelivery-ecs-delivery-stream-1-2022-07-12-00-05-07-ed57bcd0-ce62-4566-a943-04f9e462e54c";
  public static final String TMP_FOLDER = "/tmp/logsaver/";

  public static final LocalDate LOGDATE_FROM = LocalDate.parse("2022-07-08");
  public static final LocalDate LATEST_CONTUOS_EXEC_DATE = LocalDate.parse("2022-07-10");
  public static final LocalDate LOGDATE = LocalDate.parse("2022-07-11");
  public static final String BUCKET_NAME = "pn-logs-bucket-eu-south-1";
  public static final String S3_KEY =
      "logs/ecs/pnDelivery/2022/07/11/12/pn-pnDelivery-ecs-delivery-stream-1-2022-07-11-12-56-15";

  public static final List<String> MICROSERVICES = List.of("pnDelivery", "pnDeliveryPush",
      "pnExternalRegistry", "pnMandate", "pnUserAttributes");

  public static final List<String> TABLES =
      List.of("pnMandateCdc", "pnNotificationCdc", "pnTimelineCdc", "pnUserAttributesCdc");

  public static final List<String> EXPECTED_PREFIX = List.of("logs/ecs/pnDelivery/2022/07/11",
      "logs/ecs/pnDeliveryPush/2022/07/11", "logs/ecs/pnExternalRegistry/2022/07/11",
      "logs/ecs/pnMandate/2022/07/11", "logs/ecs/pnUserAttributes/2022/07/11",
      "cdc/pnMandateCdc/2022/07/11", "cdc/pnNotificationCdc/2022/07/11",
      "cdc/pnTimelineCdc/2022/07/11", "cdc/pnUserAttributesCdc/2022/07/11"

  );

  public static final List<LogFileReference> items = List.of(
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.CDC).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.CDC).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.CDC).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.CDC).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.CDC).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.CDC).s3Key(S3_KEY).build(),

      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.LOGS).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.LOGS).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.LOGS).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.LOGS).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.LOGS).s3Key(S3_KEY).build(),
      LogFileReference.builder().logDate(LOGDATE).type(LogFileType.LOGS).s3Key(S3_KEY).build());

  public static List<AuditFile> auditFiles = List.of(
      AuditFile.builder().exportType(ExportType.PDF_SIGNED).logDate(TestCostant.LOGDATE)
          .retention(Retention.AUDIT10Y).build(),
      AuditFile.builder().exportType(ExportType.PDF_SIGNED).logDate(TestCostant.LOGDATE)
          .retention(Retention.AUDIT5Y).build(),
      AuditFile.builder().exportType(ExportType.PDF_SIGNED).logDate(TestCostant.LOGDATE)
          .retention(Retention.DEVELOPER).build());


  public static List<AuditStorage> auditStorage = List.of(
      AuditStorage.builder().exportType(ExportType.PDF_SIGNED).logDate(TestCostant.LOGDATE)
          .retention(Retention.AUDIT10Y).status(AuditStorageStatus.SENT).uploadKey("updKey")
          .build(),
      AuditStorage.builder().exportType(ExportType.PDF_SIGNED).logDate(TestCostant.LOGDATE)
          .retention(Retention.AUDIT5Y).status(AuditStorageStatus.SENT).uploadKey("updKey").build(),
      AuditStorage.builder().exportType(ExportType.PDF_SIGNED).logDate(TestCostant.LOGDATE)
          .retention(Retention.DEVELOPER).status(AuditStorageStatus.SENT).uploadKey("updKey")
          .build());


  public static final DailyContextCfg CTX = DailyContextCfg.builder()
      .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap()).tmpBasePath(TMP_FOLDER)
      .logFileTypes(Set.of(LogFileType.values())).logDate(LOGDATE).build();


  public static List<AuditStorageEntity> auditFilesEntity = List.of(
      AuditStorageEntity.builder().exportType(ExportType.PDF_SIGNED)
          .logDate(TestCostant.LOGDATE.toString()).retention(Retention.AUDIT10Y)
          .result(AuditStorageStatus.SENT.name()).storageKey("updKey").build(),
      AuditStorageEntity.builder().exportType(ExportType.PDF_SIGNED)
          .logDate(TestCostant.LOGDATE.toString()).retention(Retention.AUDIT5Y)
          .result(AuditStorageStatus.SENT.name()).storageKey("updKey").build(),
      AuditStorageEntity.builder().exportType(ExportType.PDF_SIGNED)
          .logDate(TestCostant.LOGDATE.toString()).retention(Retention.DEVELOPER)
          .result(AuditStorageStatus.SENT.name()).storageKey("updKey").build(),
      AuditStorageEntity.builder().exportType(ExportType.ZIP)
          .logDate(TestCostant.LOGDATE.toString()).retention(Retention.AUDIT10Y)
          .result(AuditStorageStatus.SENT.name()).storageKey("updKey").build(),
      AuditStorageEntity.builder().exportType(ExportType.ZIP)
          .logDate(TestCostant.LOGDATE.toString()).retention(Retention.AUDIT5Y)
          .result(AuditStorageStatus.SENT.name()).storageKey("updKey").build(),
      AuditStorageEntity.builder().exportType(ExportType.ZIP)
          .logDate(TestCostant.LOGDATE.toString()).retention(Retention.DEVELOPER)
          .result(AuditStorageStatus.SENT.name()).storageKey("updKey").build());

}
