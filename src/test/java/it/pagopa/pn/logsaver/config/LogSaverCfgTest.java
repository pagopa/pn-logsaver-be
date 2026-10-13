package it.pagopa.pn.logsaver.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.exceptions.InternalException;

class LogSaverCfgTest {

  private LogSaverCfg cfg(int prefetch, DataSize maxBytes) {
    LogSaverCfg cfg = new LogSaverCfg();
    ReflectionTestUtils.setField(cfg, "processPrefetch", prefetch);
    ReflectionTestUtils.setField(cfg, "processPrefetchMaxBytes", maxBytes);
    return cfg;
  }

  @Test
  void validate_rejectsZeroByteBudget() {
    LogSaverCfg cfg = cfg(8, DataSize.ofBytes(0));

    InternalException thrown =
        assertThrows(InternalException.class, cfg::validateProcessConfiguration);

    assertTrue(thrown.getMessage().contains("prefetch-max-bytes"),
        "il messaggio deve indicare quale parametro e' fuori intervallo: " + thrown.getMessage());
  }

  @Test
  void validate_rejectsNullByteBudget() {
    LogSaverCfg cfg = cfg(8, null);

    assertThrows(InternalException.class, cfg::validateProcessConfiguration);
  }

  @Test
  void validate_rejectsNonPositivePrefetch() {
    LogSaverCfg cfg = cfg(0, DataSize.ofMegabytes(32));

    InternalException thrown =
        assertThrows(InternalException.class, cfg::validateProcessConfiguration);

    assertTrue(thrown.getMessage().contains("prefetch"),
        "il messaggio deve indicare quale parametro e' fuori intervallo: " + thrown.getMessage());
  }

  @Test
  void validate_acceptsSmallestUsefulBudget() {
    LogSaverCfg cfg = cfg(1, DataSize.ofBytes(1));

    assertDoesNotThrow(cfg::validateProcessConfiguration);
  }

  @Test
  void validate_acceptsReleasedConfiguration() {
    LogSaverCfg cfg = cfg(8, DataSize.ofMegabytes(32));

    assertDoesNotThrow(cfg::validateProcessConfiguration);
  }

  private ApplicationContextRunner runner(String... extraProperties) {
    return new ApplicationContextRunner()
        .withInitializer(ctx -> ctx.getBeanFactory()
            .setConversionService(ApplicationConversionService.getSharedInstance()))
        .withUserConfiguration(LogSaverCfg.class)
        .withPropertyValues("log-saver.tmp-folder=/tmp", "log-saver.logs-root-path-template=logs",
            "log-saver.cdc-root-path-template=cdc", "log-saver.cdc-tables.prefix=T_",
            "log-saver.audit-storage.offset-duration=PT1H")
        .withPropertyValues(extraProperties);
  }

  @Test
  void firstStartDay_propertyAbsent_defaultsTo20230302() {
    runner().run(ctx -> assertEquals(LocalDate.parse("2023-03-02"),
        ctx.getBean(LogSaverCfg.class).getFirstStartDay()));
  }

  @Test
  void firstStartDay_propertyPresent_bindsConfiguredValue() {
    runner("log-saver.first-start-day=2024-05-10").run(ctx -> assertEquals(
        LocalDate.parse("2024-05-10"), ctx.getBean(LogSaverCfg.class).getFirstStartDay()));
  }
}
