package it.pagopa.pn.logsaver.config;

import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.dao.support.StorageDaoLogicSupport;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@TestConfiguration
@Configuration
public class TestConfig {

  private static ClientAndServer server;

  // @Bean
  public static ClientAndServer setUp() {
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

    server.when(request().withMethod("GET").withPath("/safe-storage/v1/files/updKey")).respond(
        response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON).withBody(
            "{\n  \"key\": \"random/path/of/the/file\",\n  \"versionId\": \"3Z9SdhZ50PBeIj617KEMrztNKDMJj8FZ\",\n  \"contentType\": \"application/pdf\",\n  \"contentLength\": 30438,\n  \"checksum\": \"91375e9e5a9510087606894437a6a382fa5bc74950f932e2b85a788303cf5ba0\",\n  \"retentionUntil\": \"2032-04-12T12:32:04.000Z\",\n  \"documentType\": \"PN_LEGALFACT\",\n  \"documentStatus\": \"SAVED\",\n  \"download\": {\n    \"url\": \"http://localhost:8089/sage-storage/v1/download\"\n  }\n}"));

    server.when(request().withMethod("GET").withPath("/safe-storage/v1/files/updKey2")).respond(
        response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON).withBody(
            "{\n  \"key\": \"random/path/of/the/file\",\n  \"versionId\": \"3Z9SdhZ50PBeIj617KEMrztNKDMJj8FZ\",\n  \"contentType\": \"application/pdf\",\n  \"contentLength\": 30438,\n  \"checksum\": \"91375e9e5a9510087606894437a6a382fa5bc74950f932e2b85a788303cf5ba0\",\n  \"retentionUntil\": \"2032-04-12T12:32:04.000Z\",\n  \"documentType\": \"PN_LEGALFACT\",\n  \"documentStatus\": \"SAVED\",\n  \"download\": {\n    \"url\": \"http://localhost:8089/sage-storage/v1/download\"\n  }\n}"));

    server.when(request().withMethod("GET").withPath("/sage-storage/v1/download")).respond(req -> {
      Resource file = new ClassPathResource(StringUtils.remove(TestCostant.FILE_PDF, "classpath:"));
      return response().withStatusCode(200).withContentType(MediaType.PDF)
          .withBody(file.getInputStream().readAllBytes());
    });

    return server;

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
  CommandLineRunner commandLineRunner(Environment env, StorageDao dao) {
    return args -> {
      if (List.of(env.getActiveProfiles()).stream().filter(prof -> "test-download".equals(prof))
              .findFirst().isPresent()) {
        Map<String, RetentionResult> retentionResult = StorageDaoLogicSupport.defaultResultMap();
        ExecutionEntity exec = ExecutionEntity.builder().logFileTypes(LogFileType.valuesAsString())
                .retentionResult(retentionResult).logDate(DateUtils.yesterday().minusDays(1).toString())
                .build();
        ((StorageDaoInMemoryImpl) dao).insertExecution(DateUtils.yesterday().minusDays(1), exec);

        TestCostant.auditFilesEntity.stream().forEach(ent -> {
          ent.setLogDate(DateUtils.yesterday().minusDays(1).toString());
          ((StorageDaoInMemoryImpl) dao).insertAuditStorage(ent);
        });

      }
    };
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
