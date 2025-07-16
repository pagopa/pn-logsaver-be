package it.pagopa.pn.logsaver.services;

import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TTLService {
    private final LogSaverCfg cfg;

    public Optional<BigDecimal> calculateExpiration(Retention retention, boolean dailySaverSource, String logDate) {
        if (retention == null || retention.getCode() == null) {
            return Optional.empty();
        }

        Duration retentionDuration = retention.getDuration();
        Duration offsetDuration = getOffsetDuration();

        assert retentionDuration != null;
        if (!offsetDuration.isNegative()) {
            log.info("Calculating TTL for retention: {}, retention duration: {}, offset duration: {}",
                    retention.name(), retentionDuration, offsetDuration);

            return Optional.of(BigDecimal.valueOf(getDurationBase(dailySaverSource, logDate)
                    .plus(retentionDuration).plus(offsetDuration)
                    .toInstant()
                    .getEpochSecond()));
        } else {
            log.info("Offset duration is negative or not set, returning empty TTL for retention: {}", retention);
            return Optional.empty();
        }
    }

    public Duration getOffsetDuration() {
        return (cfg.getAuditStorageOffsetDuration() == null) ? Duration.ofSeconds(-1) : cfg.getAuditStorageOffsetDuration();
    }

    public static OffsetDateTime getDurationBase(boolean dailySaverSource, String logDate) {
        return getDurationBase(dailySaverSource, DateUtils.parse(logDate));
    }

    public static OffsetDateTime getDurationBase(boolean dailySaverSource, LocalDate logDate) {
        if(dailySaverSource) {
            return OffsetDateTime.now();
        }else {
            if(logDate == null){
                throw new RuntimeException("logDate di Retention cannot be NULL");
            }
            return logDate.plusDays(1L).atStartOfDay().atOffset(ZoneOffset.UTC);
        }
    }
}
