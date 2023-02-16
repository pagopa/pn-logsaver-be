package it.pagopa.pn.logsaver.client.s3;

import java.io.InputStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.springbootcfg.AwsConfigs;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;



@Service
@AllArgsConstructor
@Slf4j
public class S3BucketClientImpl implements S3BucketClient {

  private final S3Client clientS3;

  private final AwsConfigs awsCfg;



  @Override
  public Stream<S3Object> findObjects(String prefix) {
    log.debug("Call s3 bucket for list object with prefix {}", prefix);
    ListObjectsV2Response response = clientS3.listObjectsV2(
        ListObjectsV2Request.builder().bucket(awsCfg.getS3BucketName()).prefix(prefix).build());
    return response.contents().stream();
  }


  @Override
  public Stream<String> findSubFolders(String prefix, String suffix) {
    log.debug("Call s3 bucket for list subfolders between  {} and {} ", prefix, suffix);
    ListObjectsV2Response response = clientS3.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(awsCfg.getS3BucketName()).prefix(prefix).delimiter("/".concat(suffix)).build());
    return response.commonPrefixes().stream()
        .map(item -> StringUtils.removeStart(item.prefix(), prefix))
        .map(item -> StringUtils.removeEnd(item, "/".concat(suffix)));
  }


  @Override
  public InputStream getObjectContent(String key) {
    log.debug("Call s3 bucket for read content object with key {}", key);
    return clientS3
        .getObject(GetObjectRequest.builder().bucket(awsCfg.getS3BucketName()).key(key).build());
  }

  @Override
  public void uploadContent(String key, InputStream file, long size, String checksum) {
    log.debug("Call s3 bucket for upload content object with key {}", key);
    clientS3.putObject(PutObjectRequest.builder().bucket(awsCfg.getS3BucketName()).key(key)
        .contentMD5(checksum).build(), RequestBody.fromInputStream(file, size));
  }
}
