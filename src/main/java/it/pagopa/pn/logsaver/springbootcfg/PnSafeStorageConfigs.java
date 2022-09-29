package it.pagopa.pn.logsaver.springbootcfg;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "pn")
@Getter
@Setter
public class PnSafeStorageConfigs {

  private String safeStorageBaseUrl;

  private String safeStorageCxId;

}
