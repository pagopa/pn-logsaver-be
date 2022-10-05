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

  private String s3ProfileName;
  private String s3RegionCode;
  private String s3BucketName;

  private String dynamoDbProfileName;
  private String dynamoDbRegionCode;
  private String dynamoDbTableName;

}
