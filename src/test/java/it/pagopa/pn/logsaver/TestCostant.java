package it.pagopa.pn.logsaver;

import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.ItemType;

public final class TestCostant {

  public final static String TMP_FOLDER = "/tmp/logsaver/";

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

}
