package it.pagopa.pn.logsaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import it.pagopa.pn.logsaver.config.TestConfig;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.springbootcfg.ClApplicationArguments;

@SpringBootTest(
    args = {"--retention.export.type=AUDIT10Y$ZIP|PDF_SIGNED,AUDIT5Y$ZIP", "--item.types=LOGS"})
@Import(TestConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@DirtiesContext
class LogSaverApplicationSpringBootIntegrationTest {
  // EnvironmentTestUtils.addEnvironment(env, "org=Spring", "name=Boot");

  private ConfigurableApplicationContext context;


  @SpyBean
  ClApplicationArguments args;
  @SpyBean
  LogSaverRunner runner;

  @Test
  void whenApplicationStarded_thenHaveApplicationArguments() throws Exception {
    verify(runner, times(1)).run(any());
    assertEquals(2, args.getRetentionExportTypesMap().size());
    assertTrue(args.getRetentionExportTypesMap().containsKey(Retention.AUDIT10Y));
    assertTrue(args.getRetentionExportTypesMap().containsKey(Retention.AUDIT5Y));

    assertEquals(Set.of(ExportType.ZIP, ExportType.PDF_SIGNED),
        args.getRetentionExportTypesMap().get(Retention.AUDIT10Y));
    assertEquals(Set.of(ExportType.ZIP), args.getRetentionExportTypesMap().get(Retention.AUDIT5Y));


  }

}
