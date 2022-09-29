package it.pagopa.pn.logsaver.springbootcfg;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsServicesClientsConfig {

  private final AwsConfigs props;

  public AwsServicesClientsConfig(AwsConfigs props) {
    this.props = props;
  }

  @Bean
  @ConditionalOnProperty(name = "aws.dynamoDb-profile-name", havingValue = "")
  public DynamoDbClient dynamoDbClient() {
    return DynamoDbClient.builder()
        .credentialsProvider(ProfileCredentialsProvider.create(props.getDynamoDbProfileName()))
        .region(Region.of(props.getDynamoDbRegionCode())).build();
  }

  @Bean
  @ConditionalOnProperty(name = "aws.dynamoDb-profile-name")
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient baseClient) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(baseClient).build();
  }

  @Bean
  @ConditionalOnProperty(name = "aws.s3-profile-name", havingValue = "")
  public S3Client s3Client() {
    return S3Client.builder()
        .credentialsProvider(ProfileCredentialsProvider.create(props.getS3ProfileName()))
        .region(Region.of(props.getS3RegionCode())).build();
  }

}
