package it.pagopa.pn.logsaver.springbootcfg;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.Getter;

// @ConfigurationProperties
@Configuration
@Getter
public class LogSaverCfg {

  // @Value("#{T(it.pagopa.pn.logsaver.utils.DateUtils).parse('${logDate}'):T(it.pagopa.pn.logsaver.utils.DateUtils).yesterday()
  // }")
  private LocalDate logDate;

  @Value("${log-parser.tmp-folder}")
  private String tempBasePath;

  @Autowired
  private void setLogDate(@Value("${logDate}") String date) {
    logDate = DateUtils.parse(date);
    if (Objects.isNull(logDate)) {
      logDate = DateUtils.yesterday();
    }
  }


}
