package it.pagopa.pn.logsaver.client.s3;

import java.io.InputStream;
import java.util.List;
import software.amazon.awssdk.services.s3.model.S3Object;


public interface S3BucketClient {

  List<S3Object> findObjects(String prefix);

  List<String> findSubFolders(String prefix);

  InputStream getObjectContent(String key);

}
