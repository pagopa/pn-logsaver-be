package it.pagopa.pn.logsaver.services;

import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TTLService {
    private final ConversionService conversionService;
    private final LogSaverCfg cfg;

    public Optional<BigDecimal> calculateExpiration(Retention retention) {
        if (retention == null || retention.getCode() == null) {
            return Optional.empty();
        }

        Duration retentionDuration = conversionService.convert(retention.getCode(), Duration.class);
        Duration offsetDuration = ((cfg.getAuditStorageOffsetDuration() == null ) ? Duration.ofSeconds(-1) : cfg.getAuditStorageOffsetDuration());

        assert retentionDuration != null;
        if (!offsetDuration.isNegative()) {
            log.info("Calculating TTL for retention: {}, retention duration: {}, offset duration: {}",
                    retention.name(), retentionDuration, offsetDuration);

            return Optional.of(BigDecimal.valueOf(OffsetDateTime.now()
                    .plus(retentionDuration).plus(offsetDuration)
                    .toInstant()
                    .getEpochSecond()));
        } else {
            log.info("Offset duration is negative or not set, returning empty TTL for retention: {}", retention);
            return Optional.empty();
        }
    }
}
