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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
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

    boolean res;
    List<DailySaverResult> results = logSaver.dailySaverFromLatestExecutionToYesterday(
        appArgs.getItemTypes(), appArgs.getRetentionExportTypesMap());

    res = results.stream().map(resDaily -> {
      log.info(resDaily.toString());
      return resDaily;
    }).filter(resDaily -> !resDaily.isSuccess()).map(resDaily -> {
      log.info("Error Message: {}", resDaily.getError().getCause().getMessage());
      return resDaily;
    }).count() == 0;
    log.debug("Applicantion ends with status as {}", res);
    return res ? 0 : 1;
  }
}

