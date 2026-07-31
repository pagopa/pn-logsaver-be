package it.pagopa.pn.logsaver.dao.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;

class StorageDaoLogicSupportCharacterizationTest {

  private static final LocalDate LOGDATE = LocalDate.parse("2022-07-11");

  @Test
  @DisplayName("from(): retentionResult, logFileTypes ed expiration pinnati sul comportamento attuale")
  void fromPinsExecutionEntityMapping() {
    List<AuditStorageEntity> auditList = List.of(
        AuditStorageEntity.builder().exportType(ExportType.PDF_SIGNED)
            .result(AuditStorageStatus.SENT.name()).logDate(LOGDATE.toString())
            .retention(Retention.AUDIT10Y).build(),
        AuditStorageEntity.builder().exportType(ExportType.ZIP)
            .result(AuditStorageStatus.CREATED.name()).logDate(LOGDATE.toString())
            .retention(Retention.AUDIT5Y).build());

    Set<LogFileType> types = Set.of(LogFileType.LOGS);
    Duration offsetDuration = Duration.ofDays(1);

    ExecutionEntity result =
        StorageDaoLogicSupport.from(auditList, LOGDATE, types, offsetDuration, false);

    assertEquals(DateUtils.format(LOGDATE), result.getLogDate());
    assertEquals(LogFileType.valuesAsString(types), result.getLogFileTypes());

    assertEquals(2, result.getRetentionResult().size());
    assertTrue(result.getRetentionResult().containsKey("AUDIT10Y$PDF_SIGNED"));
    assertTrue(result.getRetentionResult().containsKey("AUDIT5Y$ZIP"));
    assertEquals(AuditStorageStatus.SENT.name(),
        result.getRetentionResult().get("AUDIT10Y$PDF_SIGNED").getResult());
    assertEquals(AuditStorageStatus.CREATED.name(),
        result.getRetentionResult().get("AUDIT5Y$ZIP").getResult());

    OffsetDateTime base = LOGDATE.plusDays(1L).atStartOfDay().atOffset(ZoneOffset.UTC);
    BigDecimal expectedExpiration = BigDecimal.valueOf(
        base.plus(Retention.AUDIT10Y.getDuration()).plus(offsetDuration).toInstant().getEpochSecond());
    assertEquals(expectedExpiration, result.getExpiration());
  }

  @Test
  @DisplayName("from(): con lista audit vuota, expiration si basa sulla durata massima ZERO")
  void fromWithEmptyAuditListPinsZeroDurationExpiration() {
    Duration offsetDuration = Duration.ofHours(6);

    ExecutionEntity result =
        StorageDaoLogicSupport.from(List.of(), LOGDATE, Set.of(LogFileType.values()), offsetDuration,
            false);

    assertEquals(0, result.getRetentionResult().size());

    OffsetDateTime base = LOGDATE.plusDays(1L).atStartOfDay().atOffset(ZoneOffset.UTC);
    BigDecimal expectedExpiration =
        BigDecimal.valueOf(base.plus(offsetDuration).toInstant().getEpochSecond());
    assertEquals(expectedExpiration, result.getExpiration());
  }
}
