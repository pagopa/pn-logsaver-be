package it.pagopa.pn.logsaver.client.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.springbootcfg.AwsConfigs;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class S3BucketClientImplTest {

  @Mock
  private S3Client clientS3;

  @Mock
  private AwsConfigs awsCfg;

  private S3BucketClient client;

  @BeforeEach
  public void createService() {
    this.client = new S3BucketClientImpl(clientS3, awsCfg);
    when(awsCfg.getS3BucketName()).thenReturn(TestCostant.BUCKET_NAME);
  }

  @Test
  void findObjects() {
    String s3Key = TestCostant.S3_KEY;
    S3Object mockRes = S3Object.builder().key(s3Key).build();


    when(clientS3.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(ListObjectsV2Response.builder().contents(mockRes).build());

    List<S3Object> resList =
        client.findObjects("logs/ecs/pnDelivery/2022/07/11/12/").collect(Collectors.toList());

    verify(clientS3, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    assertEquals(1, resList.size());
    assertEquals(s3Key, resList.get(0).key());

  }

  @Test
  void findSubFolders() {

    String prefix1 = "logs/ecs/pnDelivery/";
    String prefix2 = "logs/ecs/pnDeliveryPush/";
    String prefix3 = "logs/ecs/pnExternalRegistry/";
    List<CommonPrefix> resPrefixList = List.of(CommonPrefix.builder().prefix(prefix1).build(),
        CommonPrefix.builder().prefix(prefix2).build(),
        CommonPrefix.builder().prefix(prefix3).build());


    when(clientS3.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(ListObjectsV2Response.builder().commonPrefixes(resPrefixList).build());


    List<String> resList = client.findSubFolders("logs/ecs/", "2022").collect(Collectors.toList());

    verify(clientS3, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    assertEquals(3, resList.size());
    assertTrue(resList.contains("pnDelivery"));
    assertTrue(resList.contains("pnDeliveryPush"));
    assertTrue(resList.contains("pnExternalRegistry"));

  }

  @Test
  void getObjectContent() throws IOException {
    String fileContent = "TEST";
    AbortableInputStream inStream =
        AbortableInputStream.create(IOUtils.toInputStream(fileContent, Charset.defaultCharset()));
    GetObjectResponse objectResponse = GetObjectResponse.builder().build();
    ResponseInputStream<GetObjectResponse> mockRes =
        new ResponseInputStream<>(objectResponse, inStream);

    when(clientS3.getObject(any(GetObjectRequest.class))).thenReturn(mockRes);

    InputStream res = client.getObjectContent(
        "logs/ecs/pnDelivery/2022/07/11/12/pn-pnDelivery-ecs-delivery-stream-1-2022-07-11-12-56-15");

    verify(clientS3, times(1)).getObject(any(GetObjectRequest.class));
    assertEquals(fileContent, IOUtils.toString(res, Charset.defaultCharset()));
  }
}
