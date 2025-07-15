package it.pagopa.pn.logsaver.services;

import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
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
            return Optional.of(BigDecimal.valueOf(OffsetDateTime.now()
                    .plus(retentionDuration).plus(offsetDuration)
                    .toInstant()
                    .getEpochSecond()));
        } else {
            return Optional.empty();
        }
    }
}
