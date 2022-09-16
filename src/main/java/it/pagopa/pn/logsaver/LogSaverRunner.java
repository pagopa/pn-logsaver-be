package it.pagopa.pn.logsaver;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
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


    if (Objects.nonNull(appArgs.getDateFrom())) {

    } else if (Objects.nonNull(appArgs.getDateList()) && !appArgs.getDateList().isEmpty()) {

      logSaver.dailyListSaver(appArgs.getDateList(), null);
    } else {


    }



    SpringApplication.exit(ctx, () -> lunnchApp(args));

  }

  private int lunnchApp(ApplicationArguments args) {

    Boolean res = true;// logSaver.saveLogs();


    log.debug("Applicantion ends with status as {}", res);
    return res ? 0 : 1;
  }
}

