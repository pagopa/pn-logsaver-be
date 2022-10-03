package it.pagopa.pn.logsaver.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;


@Configuration
@Getter
public class LogSaverCfg {


  @Value("${log-saver.tmp-folder}")
  private String tmpBasePath;

  @Value("${log-saver.logs-root-path-template}")
  private String logsRootPathTemplate;

  @Value("${log-saver.logs-microservice}#{T(java.util.Collections).emptyList()}")
  private List<String> logsMicroservice;

  @Value("${log-saver.cdc-root-path-template}")
  private String cdcRootPathTemplate;

  @Value("${log-saver.cdc-tables:}#{T(java.util.Collections).emptyList()}")
  private List<String> cdcTables;



}
