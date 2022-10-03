package it.pagopa.pn.logsaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.config.TestConfig;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Retention;

@DisplayName("Integration Test - Test with application argurmets --retention.export.types and --item.types")
@SpringBootTest(webEnvironment = WebEnvironment.NONE,
    args = {"--retention.export.type=AUDIT10Y$ZIP|PDF_SIGNED,AUDIT5Y$ZIP", "--item.types=LOGS"})
@Import(TestConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@DirtiesContext
class LogSaverApplicationSpringBootIntegrationTest {

  @BeforeEach
  void init() {
    TestConfig.setUp();
  }

  @AfterEach
  void destroy() {
    TestConfig.destroy();
  }



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
    // TestConfig.destroy();

  }

}
