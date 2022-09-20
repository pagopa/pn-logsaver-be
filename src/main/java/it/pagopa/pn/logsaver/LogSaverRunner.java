package it.pagopa.pn.logsaver;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import it.pagopa.pn.logsaver.springbootcfg.ClApplicationArguments;
import it.pagopa.pn.logsaver.utils.DateUtils;
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



    SpringApplication.exit(ctx, () -> lunnchApp(args));

  }

  private int lunnchApp(ApplicationArguments args) {

    Boolean res = true;


    List<ItemType> types =
        appArgs.getTypes().isEmpty() ? List.of(ItemType.values()) : appArgs.getTypes();

    if (Objects.nonNull(appArgs.getDateFrom()) && Objects.nonNull(appArgs.getDateTo())) {

      logSaver.dailyListSaver(DateUtils.getDatesRange(appArgs.getDateFrom(), appArgs.getDateTo()),
          types, appArgs.getExportType());
    } else if (Objects.nonNull(appArgs.getDateList()) && !appArgs.getDateList().isEmpty()) {

      logSaver.dailyListSaver(appArgs.getDateList(), types, appArgs.getExportType());

    } else {

      logSaver.dailySaverFromLatestExecutionToYesterday(appArgs.getExportType());
    }

    log.debug("Applicantion ends with status as {}", res);
    return res ? 0 : 1;
  }
}

