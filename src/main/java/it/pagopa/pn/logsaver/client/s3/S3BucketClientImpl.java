package it.pagopa.pn.logsaver.client.s3;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.springbootcfg.AwsConfigs;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;



@Service
@AllArgsConstructor
@Slf4j
public class S3BucketClientImpl implements S3BucketClient {

  private final S3Client clientS3;

  private final AwsConfigs awsCfg;



  @Override
  public List<S3Object> findObjects(String prefix) {
    ListObjectsV2Response response = clientS3.listObjectsV2(
        ListObjectsV2Request.builder().bucket(awsCfg.getBucketName()).prefix(prefix).build());
    return response.contents().stream()
        // .map(item -> item.key())
        .collect(Collectors.toList());
  }


  @Override
  public List<String> findSubFolders(String prefix) {
    ListObjectsV2Response response = clientS3.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(awsCfg.getBucketName()).prefix(prefix).delimiter("/").build());
    return response.commonPrefixes().stream()
        .map(item -> StringUtils.removeStart(item.prefix(), prefix))
        .map(item -> StringUtils.removeEnd(item, "/")).collect(Collectors.toList());
  }


  @Override
  public InputStream getObjectContent(String key) {
    ResponseInputStream<GetObjectResponse> response = clientS3
        .getObject(GetObjectRequest.builder().bucket(awsCfg.getBucketName()).key(key).build());

    return response;

  }



}
