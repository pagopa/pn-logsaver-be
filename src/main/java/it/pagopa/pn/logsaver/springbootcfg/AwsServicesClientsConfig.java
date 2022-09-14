package it.pagopa.pn.logsaver.springbootcfg;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsServicesClientsConfig {

  private final AwsConfigs props;

  public AwsServicesClientsConfig(AwsConfigs props) {
    this.props = props;
  }


  @Bean
  public DynamoDbAsyncClient dynamoDbAsyncClient() {
    return this.configureBuilder(DynamoDbAsyncClient.builder());
  }

  @Bean
  public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(
      DynamoDbAsyncClient baseAsyncClient) {
    return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(baseAsyncClient).build();
  }

  @Bean
  public DynamoDbClient dynamoDbClient() {
    return configureBuilder(DynamoDbClient.builder());
  }

  @Bean
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient baseClient) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(baseClient).build();
  }



  private <C> C configureBuilder(AwsClientBuilder<?, C> builder) {

    if (props != null) {

      String profileName = props.getProfileName();
      if (StringUtils.isNotBlank(profileName)) {
        builder.credentialsProvider(ProfileCredentialsProvider.create(profileName));
      }

      String regionCode = props.getRegionCode();
      if (StringUtils.isNotBlank(regionCode)) {
        builder.region(Region.of(regionCode));
      }

      /*
       * String endpointUrl = props.getEndpointUrl(); if (StringUtils.isNotBlank(endpointUrl)) {
       * builder.endpointOverride(URI.create(endpointUrl)); }
       */

    }

    return builder.build();
  }


  @Bean
  public S3Client s3Client() {
    return configureBuilder(S3Client.builder());
  }

}
