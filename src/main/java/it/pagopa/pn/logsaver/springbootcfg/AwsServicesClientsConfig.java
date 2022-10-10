package it.pagopa.pn.logsaver.springbootcfg;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class AwsServicesClientsConfig {

  private final AwsConfigs props;

  public AwsServicesClientsConfig(AwsConfigs props) {
    this.props = props;
  }

  @Bean
  @ConditionalOnProperty(name = "aws.use-dynamoDb", havingValue = "true", matchIfMissing = true)
  public DynamoDbClient dynamoDbClient() {
    DynamoDbClientBuilder clientBuilder = configureBuilder( DynamoDbClient.builder() );

    return clientBuilder.build();
  }

  @Bean
  @ConditionalOnBean(DynamoDbClient.class)
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient baseClient) {
    return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(baseClient)
            .build();
  }

  @Bean
  @ConditionalOnProperty(name = "aws.use-s3", havingValue = "true", matchIfMissing = true)
  public S3Client s3Client() {
    S3ClientBuilder clientBuilder = configureBuilder( S3Client.builder() );

    return clientBuilder.build();
  }

  private <B extends AwsClientBuilder> B configureBuilder(B builder) {
    if( props != null ) {

      String profileName = props.getProfileName();
      if( StringUtils.isNotBlank( profileName ) ) {
        builder.credentialsProvider( ProfileCredentialsProvider.create( profileName ));
      }

      String regionCode = props.getRegionCode();
      if( StringUtils.isNotBlank( regionCode )) {
        builder.region( Region.of( regionCode ));
      }

      String endpointUrl = props.getEndpointUrl();
      if( StringUtils.isNotBlank( endpointUrl )) {
        builder.endpointOverride( URI.create( endpointUrl ));
      }

    }
    return builder;
  }

}
