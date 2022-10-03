package it.pagopa.pn.logsaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.config.TestConfig;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.AuditSaverService;


@DirtiesContext
class LogSaverApplicationSpringBootIntegrationTest {

  @Nested
  @DisplayName("Integration Test - Test with application argurmets --retention.export.types and --item.types")
  @SpringBootTest(webEnvironment = WebEnvironment.NONE,
      args = {"--retention.export.type=AUDIT10Y$ZIP|PDF_SIGNED,AUDIT5Y$ZIP", "--item.types=LOGS"})
  @Import(TestConfig.class)
  @TestPropertySource(locations = "classpath:application-test.properties")
  @ActiveProfiles("test")
  class Test1 {

    @SpyBean
    ClApplicationArguments args;
    @SpyBean
    LogSaverRunner runner;
    @SpyBean
    AuditSaverService saverService;

    @Test
    void whenApplicationStarded_thenHaveApplicationArguments() throws Exception {
      verify(runner, times(1)).run(any());
      verify(saverService, times(1)).dailySaverFromLatestExecutionToYesterday(anySet(), anyMap());
      assertEquals(2, args.getRetentionExportTypesMap().size());
      assertTrue(args.getRetentionExportTypesMap().containsKey(Retention.AUDIT10Y));
      assertTrue(args.getRetentionExportTypesMap().containsKey(Retention.AUDIT5Y));

      assertEquals(Set.of(ExportType.ZIP, ExportType.PDF_SIGNED),
          args.getRetentionExportTypesMap().get(Retention.AUDIT10Y));
      assertEquals(Set.of(ExportType.ZIP),
          args.getRetentionExportTypesMap().get(Retention.AUDIT5Y));


    }
  }


  @Nested
  @DisplayName("Integration Test - Test with application argurments --date.list and --item.types")
  @SpringBootTest(webEnvironment = WebEnvironment.NONE,
      args = {"--date.list=2022-07-09", "--item.types=LOGS,CDC"})
  @Import(TestConfig.class)
  @TestPropertySource(locations = "classpath:application-test.properties")
  @ExtendWith({OutputCaptureExtension.class, SpringExtension.class})
  @ActiveProfiles("test")
  class Test2 {

    @SpyBean
    ClApplicationArguments args;
    @SpyBean
    LogSaverRunner runner;

    @SpyBean
    AuditSaverService saverService;

    @SpyBean
    StorageDao storageDao;

    @BeforeEach
    void populateDb() {
      storageDao.getClass();
    }

    @Test
    void whenApplicationStarded_thenHaveApplicationArguments(CapturedOutput output)
        throws Exception {
      verify(runner, times(1)).run(any());

      assertEquals(2, args.getItemTypes().size());
      assertEquals(1, args.getDateList().size());

      assertTrue(args.getItemTypes().contains(ItemType.CDC));
      assertTrue(args.getItemTypes().contains(ItemType.LOGS));

      Awaitility.await().atMost(30, TimeUnit.SECONDS)
          .until(() -> output.getAll().contains("Log Saver Applicantion ends"));
      verify(saverService, times(1)).dailyListSaver(anyList());
      verify(storageDao, times(1)).updateExecution(any(), any(), any());


    }
  }

}
