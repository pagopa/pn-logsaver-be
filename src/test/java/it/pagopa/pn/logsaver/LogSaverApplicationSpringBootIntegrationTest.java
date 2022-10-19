package it.pagopa.pn.logsaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.config.TestConfig;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;


class LogSaverApplicationSpringBootIntegrationTest {
  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();


  @Test
  @Disabled
  void whenApplicationStarded_thenHaveApplicationArguments() throws Exception {
    this.contextRunner
        .withPropertyValues("retention.export.type=AUDIT10Y$ZIP|PDF_SIGNED,AUDIT5Y$ZIP",
            "log.file.types=LOGS")
        .withUserConfiguration(new Class[] {ClApplicationArguments.class, TestConfig.class,
            DefaultApplicationArguments.class})
        .run(context -> {
          ClApplicationArguments args = context.getBean(ClApplicationArguments.class);
          assertEquals(2, args.getRetentionExportTypesMap().size());
          assertTrue(args.getRetentionExportTypesMap().containsKey(Retention.AUDIT10Y));
          assertTrue(args.getRetentionExportTypesMap().containsKey(Retention.AUDIT5Y));

          assertEquals(Set.of(ExportType.ZIP, ExportType.PDF_SIGNED),
              args.getRetentionExportTypesMap().get(Retention.AUDIT10Y));
          assertEquals(Set.of(ExportType.ZIP),
              args.getRetentionExportTypesMap().get(Retention.AUDIT5Y));
        });

  }


}
