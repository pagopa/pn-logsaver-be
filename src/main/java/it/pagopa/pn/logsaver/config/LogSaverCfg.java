package it.pagopa.pn.logsaver.config;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;


@Configuration
@Getter
public class LogSaverCfg {


  @Value("${log-saver.tmp-folder}")
  private String tmpBasePath;

  @Value("${log-saver.logs-root-path-template}")
  private String logsRootPathTemplate;

  @Value("${log-saver.logs-microservice:#{T(java.util.Collections).emptyList()}}")
  private List<String> logsMicroservice;

  @Value("${log-saver.cdc-root-path-template}")
  private String cdcRootPathTemplate;

  @Value("${log-saver.cdc-tables:ALL}")
  private List<String> cdcTables;

  @Value("${log-saver.export-max-file-size:5MB}")
  private DataSize maxSize;

  @Value("${log-saver.process.prefetch:1}")
  private int processPrefetch;

  @Value("${log-saver.process.prefetch-max-bytes:32MB}")
  private DataSize processPrefetchMaxBytes;
  
  @Value("${log-saver.cdc-tables.prefix}")
  private String cdcTablesPrefix;

  @Value("${log-saver.audit-storage.offset-duration}")
  private Duration auditStorageOffsetDuration;

  @PostConstruct
  void validateProcessConfiguration() {
    if (processPrefetch < 1) {
      throw new InternalException(
          "Invalid configuration log-saver.process.prefetch: must be at least 1, found "
              + processPrefetch);
    }
    if (processPrefetchMaxBytes == null || processPrefetchMaxBytes.toBytes() < 1) {
      throw new InternalException(
          "Invalid configuration log-saver.process.prefetch-max-bytes: must be at least 1 byte, found "
              + (processPrefetchMaxBytes == null ? "none" : processPrefetchMaxBytes.toBytes()));
    }
  }

}
