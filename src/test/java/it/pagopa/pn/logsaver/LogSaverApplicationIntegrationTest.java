package it.pagopa.pn.logsaver;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith({OutputCaptureExtension.class, SpringExtension.class})
@DirtiesContext // (methodMode = MethodMode.BEFORE_METHOD)
class LogSaverApplicationIntegrationTest {
  // EnvironmentTestUtils.addEnvironment(env, "org=Spring", "name=Boot");

  private ConfigurableApplicationContext context;

  @Test
  void whenApplicationTeminated_thenReturnExitCode(CapturedOutput output) throws Exception {
    // verify(applicationRunnerTaskExecutor, times(1)).run(any());
    // verify(commandLineTaskExecutor, times(1)).run(any());

    SpringApplication application = new SpringApplication(LogSaverApplication.class);
    // application.setWebApplicationType(WebApplicationType.NONE);
    this.context = application.run("--spring.profiles.active=test");
    assertThat(output).contains("The following profiles are active: test");
    assertThat(output).contains("Log Saver Applicantion ends with status as 0");

    SpringApplication.exit(context, () -> 0);
  }


}
