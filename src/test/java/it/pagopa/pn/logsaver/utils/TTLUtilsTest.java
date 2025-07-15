package it.pagopa.pn.logsaver.utils;

import it.pagopa.pn.logsaver.model.enums.Retention;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TTLUtilsTest {

    @Test
    void testCalculateExpiration_AUDIT2M() {
        BigDecimal result = TTLUtils.calculateExpiration(Retention.AUDIT2Y);
        assertNotNull(result);
    }

}

