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
import it.pagopa.pn.logsaver.config.TestConfig;
import it.pagopa.pn.logsaver.utils.DateUtils;


@ExtendWith({OutputCaptureExtension.class, SpringExtension.class})
@DirtiesContext
class LogSaverApplicationIntegrationTest {


  @Test
  void whenApplicationTeminated_thenReturnExitCode_0(CapturedOutput output) throws Exception {
    TestConfig.setUp();
    String data = DateUtils.yesterday().minusDays(1).toString();
    LogSaverApplication.main(new String[] {"--spring.profiles.active=test",
        "--spring.config.location=classpath:application-test.properties", "--date.list=" + data});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Log Saver Applicantion ends"));

    assertThat(output).contains("Log Saver Applicantion ends with status as 0");
    TestConfig.destroy();
  }


  @Test
  void whenApplicationTeminated_thenReturnExitCode_1(CapturedOutput output) throws Exception {

    LogSaverApplication.main(new String[] {"--spring.profiles.active=test",
        "--spring.config.location=classpath:application-test.properties"});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Log Saver Applicantion ends"));

    assertThat(output).contains("Log Saver Applicantion ends with status as 1");
  }

}
