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


@ExtendWith({OutputCaptureExtension.class, SpringExtension.class})
@DirtiesContext
class LogSaverApplicationIntegrationTest {


  @Test
  void whenApplicationTeminated_thenReturnExitCode(CapturedOutput output) throws Exception {
    TestConfig.setUp();
    LogSaverApplication.main(new String[] {"--spring.profiles.active=test",
        "--spring.config.location=classpath:application-test.properties"});
    Awaitility.await().atMost(120, TimeUnit.SECONDS)
        .until(() -> output.getAll().contains("Log Saver Applicantion ends"));

    assertThat(output).contains("Log Saver Applicantion ends with status as 0");
    TestConfig.destroy();
  }


}
