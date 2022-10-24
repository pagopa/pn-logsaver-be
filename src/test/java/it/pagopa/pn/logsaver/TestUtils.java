package it.pagopa.pn.logsaver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.StorageExecution.ExecutionDetails;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;

public class TestUtils {



  public static String getValue(Map<String, AttributeValue> map, String name) {
    return map.get(":AMZN_MAPPED_" + name).s();
  }

  public static List<Update> searchUpd(List<TransactWriteItem> transac, String key, String value) {
    return transac.stream().map(TransactWriteItem::update).filter(Objects::nonNull).filter(upd -> {
      return upd.key().get(key).s().equalsIgnoreCase(value);
    }).collect(Collectors.toList());
  }

  public static List<Put> searchPut(List<TransactWriteItem> transac, String key, String value) {
    return transac.stream().map(TransactWriteItem::put).filter(Objects::nonNull).filter(put -> {
      return put.item().get(key).s().equalsIgnoreCase(value);
    }).collect(Collectors.toList());
  }

  public static boolean equals(ExecutionEntity ent, ExecutionEntity entTar) {

    return ent.getLogFileTypes().equals(entTar.getLogFileTypes())
        && ent.getLogDate().equals(entTar.getLogDate())
        && equals(ent.getRetentionResult(), entTar.getRetentionResult());
  }


  public static boolean equals(Map<String, RetentionResult> first,
      Map<String, RetentionResult> second) {
    return first.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(),
            e -> TestUtils.equals(e.getValue(), second.get(e.getKey()))))
        .values().stream().filter(x -> !x).count() == 0;
  }


  public static boolean equals(ExecutionEntity ent, StorageExecution model) {

    return LogFileType.values(ent.getLogFileTypes()).equals(model.getLogFileTypes())
        && DateUtils.parse(ent.getLogDate()).equals(model.getLogDate())
        && equals(ent.getRetentionResult().values(), model.getDetails());
  }

  public static boolean equals(Collection<RetentionResult> entList,
      Collection<ExecutionDetails> modelList) {
    return entList.size() == modelList.size()
        && modelList.stream().filter(model -> contains(entList, model)).count() == modelList.size();
  }

  public static boolean contains(Collection<RetentionResult> entList, ExecutionDetails model) {
    return entList.stream().filter(ent -> equals(ent, model)).findFirst().isPresent();
  }

  public static boolean equals(RetentionResult ent, ExecutionDetails model) {
    return ExportType.valueOf(ent.getExportType()) == model.getExportType()
        && AuditStorageStatus.valueOf(ent.getResult()) == model.getStatus()
        && Retention.valueOf(ent.getRetention()) == model.getRetention();
  }

  public static boolean equals(RetentionResult ent, RetentionResult entTarg) {
    return ent.getExportType().equals(entTarg.getExportType())
        && ent.getResult().equals(entTarg.getResult())
        && ent.getRetention().equals(entTarg.getRetention());
  }


  public static List<ExecutionDetails> defaultExecutionDetails() {
    return Stream.of(Retention.values())
        .flatMap(ret -> Stream.of(ExportType.values())
            .map(expTy -> new ExecutionDetails(ret, AuditStorageStatus.SENT, expTy)))
        .collect(Collectors.toList());
  }

  public static List<ExecutionDetails> defaultErrorExecutionDetails() {
    List<ExecutionDetails> list = defaultExecutionDetails();
    list.stream()
        .filter(ex -> ex.getRetention() == Retention.AUDIT10Y
            && ex.getExportType() == ExportType.PDF_SIGNED)
        .forEach(ex -> ex.setStatus(AuditStorageStatus.CREATED));
    return list;
  }
}
