package it.pagopa.pn.logsaver.springbootcfg;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties("aws")
@Getter
@Setter
public class AwsConfigs {

  private String profileName;
  private String regionCode;
  private String endpointUrl;

  private String s3BucketName;
  private String dynamoDbTableName;

}
