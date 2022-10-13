package it.pagopa.pn.logsaver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class LogSaverApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(LogSaverApplication.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    application.run(args);
  }

}

