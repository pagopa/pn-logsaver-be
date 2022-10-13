package it.pagopa.pn.logsaver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Configuration
public class LogSaverConfiguration {
  @Autowired
  private ConfigurableApplicationContext ctx;

  @Bean
  public SimpleAsyncTaskExecutor taskExecutor() {
    return new SimpleAsyncTaskExecutor("pn-log-saver");
  }

  @Bean
  @Profile("!test")
  ExiCodeListener exiCodeListener() {
    return new ExiCodeListener();
  }

  @AllArgsConstructor
  @Getter
  public static class ExitEvent {
    private final int exitCode;
  }


  private class ExiCodeListener {
    @EventListener
    public void exitEvent(ExitEvent event) {
      System.exit(SpringApplication.exit(ctx, event::getExitCode));
    }
  }

}
