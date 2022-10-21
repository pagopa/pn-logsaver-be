package it.pagopa.pn.logsaver;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import it.pagopa.pn.logsaver.command.base.Commands;
import it.pagopa.pn.logsaver.config.TestConfig;
import it.pagopa.pn.logsaver.utils.DateUtils;


@ExtendWith({OutputCaptureExtension.class, SpringExtension.class})
@DirtiesContext
class LogSaverApplicationIntegrationTest {


  @Test
  void whenApplicationTeminated_thenReturnExitCode_0(CapturedOutput output) throws Exception {
    TestConfig.setUp();
    String data = DateUtils.yesterday().minusDays(1).toString();
    LogSaverApplication.main(new String[] {"datelist-saver", "--spring.profiles.active=test",
        "--spring.config.location=classpath:application-test.properties", "--date.list=" + data});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Log Saver Application ends"));

    assertThat(output).contains("Log Saver Application ends with status as 0");
    TestConfig.destroy();
  }


  @Test
  void whenApplicationTeminated_thenReturnExitCode_1(CapturedOutput output) throws Exception {

    LogSaverApplication.main(new String[] {"--spring.profiles.active=test",
        "--spring.config.location=classpath:application-test.properties",
        "--retention.export.type=AUDIT10Y$ZIP|PDF_SIGNED,AUDIT5Y$ZIP", "--log.file.types=LOGS"});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Log Saver Application ends"));

    assertThat(output).contains("Log Saver Application ends with status as 1");
  }

  @Test
  void downloadCommand_whenApplicationTeminated_thenReturnExitCode_0(CapturedOutput output)
      throws Exception {
    TestConfig.setUp();
    String data = DateUtils.yesterday().minusDays(1).toString();
    LogSaverApplication
        .main(new String[] {"download", "--spring.profiles.active=test,test-download",
            "--spring.config.location=classpath:application-test.properties", "--date=" + data});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Log Saver Application ends"));

    assertThat(output).contains("Log Saver Application ends with status as 0");
    TestConfig.destroy();
  }


  @Test
  void dateRange_downloadCommand_whenApplicationTeminated_thenReturnExitCode_0(
      CapturedOutput output) throws Exception {
    TestConfig.setUp();
    String data = DateUtils.yesterday().minusDays(1).toString();
    LogSaverApplication.main(new String[] {Commands.DATERANGE_DOWNLOAD_AUDIT_S,
        "--spring.profiles.active=test,test-download",
        "--spring.config.location=classpath:application-test.properties", "--date.from=" + data,
        "--date.to=" + data,});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Log Saver Application ends"));

    assertThat(output).contains("Log Saver Application ends with status as 0");
    TestConfig.destroy();
  }


  @Test
  void downloadCommand_emptyArgs_whenApplicationTeminated_thenReturnExitCode_1(
      CapturedOutput output) throws Exception {

    LogSaverApplication.main(new String[] {"download", "--spring.profiles.active=test",
        "--spring.config.location=classpath:application-test.properties"});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Failure executing log saver"));

    assertThat(output).contains("Failure executing log saver");
  }


  @Test
  void downloadCommand_whenApplicationTeminated_thenReturnExitCode_1(CapturedOutput output)
      throws Exception {
    String data = DateUtils.yesterday().minusDays(1).toString();
    LogSaverApplication
        .main(new String[] {"download", "--date=" + data, "--spring.profiles.active=test",
            "--spring.config.location=classpath:application-test.properties"});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Log Saver Application ends"));

    assertThat(output).contains("Log Saver Application ends with status as 1");
  }

}
