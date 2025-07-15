package it.pagopa.pn.logsaver.utils;

import it.pagopa.pn.logsaver.model.enums.Retention;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

public class TTLUtils {
    public static BigDecimal calculateExpiration(Retention retention) {
        if (retention == null || retention.getCode() == null) {
            return null;
        }

        ConversionService conversionService = new ApplicationConversionService();
        Duration retentionDuration = conversionService.convert(retention.getCode(), Duration.class);

        assert retentionDuration != null;
        long expirationEpochSeconds = OffsetDateTime.now()
                .plus(retentionDuration)
                .toInstant()
                .getEpochSecond();

        //TODO: aggiungere offset

        return BigDecimal.valueOf(expirationEpochSeconds);
    }
}
