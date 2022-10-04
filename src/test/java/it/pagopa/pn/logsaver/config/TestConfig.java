package it.pagopa.pn.logsaver.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.dao.StorageDao;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@TestConfiguration
// @ExtendWith(MockServerExtension.class)
// @MockServerSettings(ports = {8088})
public class TestConfig {

  private static ClientAndServer server;

  @PostConstruct
  public static void setUp() {
    System.setProperty("-Dmockserver.logLevel", "OFF");
    if (Objects.isNull(server) || !server.isRunning()) {
      server = ClientAndServer.startClientAndServer(8089);
    }



    server.when(request().withMethod("POST").withPath("/safe-storage/v1/files")).respond(
        response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON).withBody(
            "{\n  \"uploadMethod\": \"PUT\",\n  \"uploadUrl\": \"http://localhost:8089/sage-storage/v1/upload-with-presigned-url\",\n  \"secret\": \"AZ23RF12\",\n  \"key\": \"8F7E/9A3B/1234/AB87\"\n}"));

    server.when(request().withMethod("PUT").withPath("/sage-storage/v1/upload-with-presigned-url"))
        .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
            .withBody(""));

  }

  // @PreDestroy
  public static void destroy() {
    server.stop();
  }

  @Bean
  StorageDao storageDao() {
    return new StorageDaoInMemoryImpl();

  }

  @Bean
  S3Client s3Client() {
    S3Object mockRes = S3Object.builder().key(TestCostant.S3_KEY).build();


    List<CommonPrefix> resPrefixList =
        List.of(CommonPrefix.builder().prefix("logs/ecs/pnDelivery/").build(),
            CommonPrefix.builder().prefix("logs/ecs/pnDeliveryPush/").build(),
            CommonPrefix.builder().prefix("logs/ecs/pnExternalRegistry/").build());



    S3Client client = Mockito.mock(S3Client.class);

    when(client.listObjectsV2(any(ListObjectsV2Request.class))).thenAnswer(inv -> {
      if (StringUtils.isEmpty(inv.getArgument(0, ListObjectsV2Request.class).delimiter())) {
        return ListObjectsV2Response.builder().contents(mockRes).build();
      } else {
        return ListObjectsV2Response.builder().commonPrefixes(resPrefixList).build();
      }

    });

    when(client.getObject(any(GetObjectRequest.class))).then(in -> {
      Resource file = new ClassPathResource(StringUtils.remove(TestCostant.FILE_LOG, "classpath:"));
      AbortableInputStream inStream = AbortableInputStream.create(file.getInputStream());
      GetObjectResponse objectResponse = GetObjectResponse.builder().build();
      return new ResponseInputStream<>(objectResponse, inStream);

    });

    return client;
  }
}
