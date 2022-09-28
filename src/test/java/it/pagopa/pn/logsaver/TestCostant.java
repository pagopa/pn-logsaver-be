package it.pagopa.pn.logsaver;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.utils.LsUtils;

public final class TestCostant {

  public final static String TMP_FOLDER = "/tmp/logsaver/";

  public final static LocalDate LOGDATE_FROM = LocalDate.parse("2022-07-08");
  public final static LocalDate LATEST_CONTUOS_EXEC_DATE = LocalDate.parse("2022-07-10");
  public final static LocalDate LOGDATE = LocalDate.parse("2022-07-11");
  public final static String BUCKET_NAME = "pn-logs-bucket-eu-south-1";
  public final static String S3_KEY =
      "logs/ecs/pnDelivery/2022/07/11/12/pn-pnDelivery-ecs-delivery-stream-1-2022-07-11-12-56-15";

  public final static List<String> MICROSERVICES = List.of("pnDelivery", "pnDeliveryPush",
      "pnExternalRegistry", "pnMandate", "pnUserAttributes");

  public final static List<String> TABLES =
      List.of("pnMandateCdc", "pnNotificationCdc", "pnTimelineCdc", "pnUserAttributesCdc");

  public final static List<String> EXPECTED_PREFIX = List.of("logs/ecs/pnDelivery/2022/07/11",
      "logs/ecs/pnDeliveryPush/2022/07/11", "logs/ecs/pnExternalRegistry/2022/07/11",
      "logs/ecs/pnMandate/2022/07/11", "logs/ecs/pnUserAttributes/2022/07/11",
      "cdc/pnMandateCdc/2022/07/11", "cdc/pnNotificationCdc/2022/07/11",
      "cdc/pnTimelineCdc/2022/07/11", "cdc/pnUserAttributesCdc/2022/07/11"

  );

  public final static List<Item> items =
      List.of(Item.builder().logDate(LOGDATE).type(ItemType.CDC).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.CDC).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.CDC).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.CDC).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.CDC).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.CDC).s3Key(S3_KEY).build(),

          Item.builder().logDate(LOGDATE).type(ItemType.LOGS).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.LOGS).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.LOGS).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.LOGS).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.LOGS).s3Key(S3_KEY).build(),
          Item.builder().logDate(LOGDATE).type(ItemType.LOGS).s3Key(S3_KEY).build());

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


  public final static DailyContextCfg CTX =
      DailyContextCfg.builder().retentionExportTypeMap(LsUtils.defaultRetentionExportTypeMap())
          .tmpBasePath(TMP_FOLDER).itemTypes(Set.of(ItemType.values())).logDate(LOGDATE).build();

}
