package it.pagopa.pn.logsaver.client.s3;

import java.io.InputStream;
import java.util.stream.Stream;
import software.amazon.awssdk.services.s3.model.S3Object;


public interface S3BucketClient {

  Stream<S3Object> findObjects(String prefix);

  Stream<String> findSubFolders(String prefix, String suffix);

  InputStream getObjectContent(String key);

  void uploadContent(String key, InputStream file, long size, String checksum);
  
  Stream<String> findSubFoldersWithPrefix(String pathPrefix, String subFolderPrefix, String suffix);

}
