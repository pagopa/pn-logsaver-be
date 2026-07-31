package it.pagopa.pn.logsaver.client.s3;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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

    return paginatedList(
        token -> {
          ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
              .bucket(awsCfg.getS3BucketName()).prefix(prefix);
          if (token != null) {
            builder.continuationToken(token);
          }
          return builder.build();
        },
        ListObjectsV2Response::contents);
  }


  @Override
  /** 
   * Metodo per la ricerca di subFolders per un dato prefix e suffix (delimitatore)
   * @param String prefix: prefisso del path s3
   * @param String suffix: delimitatore del path s3
   * @return Stream<String>: stream di subFolders
   */
  public Stream<String> findSubFolders(String prefix, String suffix) {
    log.debug("Call s3 bucket for list subfolders between  {} and {} ", prefix, suffix);
    ListObjectsV2Response response = clientS3.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(awsCfg.getS3BucketName()).prefix(prefix).delimiter("/".concat(suffix)).build());
    return response.commonPrefixes().stream()
        .map(item -> StringUtils.removeStart(item.prefix(), prefix))
        .map(item -> StringUtils.removeEnd(item, "/".concat(suffix)));
  }

  @Override
  /**
   * Metodo per la ricerca di subFolders per un dato pathPrefix, subFolderPrefix e suffix (delimitatore).
   * @param String pathPrefix: prefisso del path s3
   * @param String subFolderPrefix: prefisso dei subFolder oggetto della ricerca
   * @param String suffix: delimitatore del path s3
   * @return Stream<String>: stream di subFolders
   */
  public Stream<String> findSubFoldersWithPrefix(String pathPrefix, String subFolderPrefix, String suffix) {
    log.debug("Call s3 bucket for list subfolders between  {} and {} ", pathPrefix, suffix);
    ListObjectsV2Response response = clientS3.listObjectsV2(ListObjectsV2Request.builder()
            .bucket(awsCfg.getS3BucketName()).prefix(pathPrefix.concat(subFolderPrefix)).delimiter("/".concat(suffix)).build());
    return response.commonPrefixes().stream()
            .map(item -> StringUtils.removeStart(item.prefix(), pathPrefix))
            .map(item -> StringUtils.removeEnd(item, "/".concat(suffix)));
  }

  public Stream<String> findSubFoldersWithPrefix(String pathPrefix, String subFolderPrefix) {
    log.debug("Call s3 bucket for list pathPrefix={}, subFolderPrefix={}", pathPrefix, subFolderPrefix);

    return paginatedList(
        token -> {
          ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
              .bucket(awsCfg.getS3BucketName())
              .prefix(pathPrefix.concat(subFolderPrefix))
              .delimiter("/");
          if (token != null) {
            builder.continuationToken(token);
          }
          return builder.build();
        },
        response -> response.commonPrefixes().stream()
            .map(item -> StringUtils.removeStart(item.prefix(), pathPrefix))
            .map(item -> StringUtils.removeEnd(item, "/"))
            .collect(Collectors.toList()));
  }

  private <T> Stream<T> paginatedList(
      Function<String, ListObjectsV2Request> requestBuilder,
      Function<ListObjectsV2Response, List<T>> extractor) {
    Iterator<T> lazyIterator = new Iterator<>() {
      private Iterator<T> currentPage = Collections.emptyIterator();
      private String continuationToken = null;
      private boolean firstPage = true;
      private int page = 1;

      @Override
      public boolean hasNext() {
        while (!currentPage.hasNext() && (firstPage || continuationToken != null)) {
          ListObjectsV2Response response =
              clientS3.listObjectsV2(requestBuilder.apply(continuationToken));
          firstPage = false;
          continuationToken = Boolean.TRUE.equals(response.isTruncated())
              ? response.nextContinuationToken() : null;
          if (continuationToken != null) {
            log.debug("paginatedList fetching page {}", ++page);
          }
          currentPage = extractor.apply(response).iterator();
        }
        return currentPage.hasNext();
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return currentPage.next();
      }
    };

    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(lazyIterator, Spliterator.ORDERED | Spliterator.NONNULL),
        false);
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
