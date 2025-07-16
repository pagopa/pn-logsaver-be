package it.pagopa.pn.logsaver.services.impl;

import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.TTLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TTLServiceTest {

    private LogSaverCfg cfg;
    private TTLService ttlService;

    @BeforeEach
    void setUp() {
        cfg = mock(LogSaverCfg.class);
        ttlService = new TTLService(cfg);
    }

    @Test
    void testCalculateExpiration_withValidRetentionAndOffset() {
        when(cfg.getAuditStorageOffsetDuration()).thenReturn(Duration.ofDays(120));
        Retention retention = Retention.AUDIT10Y;

        Optional<BigDecimal> resultOpt = ttlService.calculateExpiration(retention, true, null);

        assertTrue(resultOpt.isPresent());
        long nowEpoch = OffsetDateTime.now().toEpochSecond();
        long actual = resultOpt.get().longValue();

        assertTrue(actual > nowEpoch, "Expiration should be in the future");
        assertTrue(actual < nowEpoch + Duration.ofDays(365 * 11).getSeconds(), "Expiration should be within 10 years");
    }

    @Test
    void testCalculateExpiration_withNullOffset_shouldReturnEmpty() {
        when(cfg.getAuditStorageOffsetDuration()).thenReturn(null);  // offset assente
        Retention retention = Retention.AUDIT10Y;
        Optional<BigDecimal> result = ttlService.calculateExpiration(retention, true, null);
        assertFalse(result.isPresent(), "Expected empty result when offset is null");
    }
}

