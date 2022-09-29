package it.pagopa.pn.logsaver;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import it.pagopa.pn.logsaver.springbootcfg.ClApplicationArguments;
import it.pagopa.pn.logsaver.utils.LsUtils;

@Component
public class LogSaverRunner implements ApplicationRunner {

  @Autowired
  private AuditSaverService logSaver;

  @Autowired
  private ConfigurableApplicationContext ctx;

  @Autowired
  private ClApplicationArguments appArgs;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Value("${retention.export.type:}")
  String retentionExportType;

  @Override
  public void run(ApplicationArguments args) throws Exception {

    List<DailySaverResult> results = logSaver.dailySaverFromLatestExecutionToYesterday(
        appArgs.getItemTypes(), appArgs.getRetentionExportTypesMap());
    int exitCode = LsUtils.exitCodeAndLogResult(results);

    eventPublisher.publishEvent(new ExitCodeEvent(results, exitCode));
  }

  private class ExiCodeListener {
    @EventListener
    public void exitEvent(ExitCodeEvent event) {
      SpringApplication.exit(ctx, event::getExitCode);
    }
  }

  @Bean
  @Profile("!test")
  ExiCodeListener exiCodeListener() {
    return new ExiCodeListener();
  }

}

