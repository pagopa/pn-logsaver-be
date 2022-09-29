package it.pagopa.pn.logsaver.config;

import java.io.InputStream;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3BucketClientMockedImpl implements S3BucketClient {

  @Override
  public Stream<S3Object> findObjects(String prefix) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<String> findSubFolders(String prefix) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public InputStream getObjectContent(String key) {
    // TODO Auto-generated method stub
    return null;
  }

}
