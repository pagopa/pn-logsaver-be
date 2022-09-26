package it.pagopa.pn.logsaver;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
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

  @Override
  public void run(ApplicationArguments args) throws Exception {

    SpringApplication.exit(ctx, this::lunnchApp);
  }

  private int lunnchApp() {
    List<DailySaverResult> results = logSaver.dailySaverFromLatestExecutionToYesterday(
        appArgs.getItemTypes(), appArgs.getRetentionExportTypesMap());
    return LsUtils.exitCodeAndLogResult(results);
  }
}

