package it.pagopa.pn.logsaver.springbootcfg;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "pn")
@Data
public class PnSafeStorageConfigs {

  private String safeStorageBaseUrl;

  private String safeStorageCxId;

}
