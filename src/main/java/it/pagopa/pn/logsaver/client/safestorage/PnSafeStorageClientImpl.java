package it.pagopa.pn.logsaver.client.safestorage;


import java.net.URI;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import it.pagopa.pn.logsaver.exceptions.ExternalException;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.ApiClient;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileDownloadApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.springbootcfg.PnSafeStorageConfigs;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PnSafeStorageClientImpl implements PnSafeStorageClient {
  public static final String SAVED = "SAVED";

  private final FileUploadApi fileUploadApi;
  private final FileDownloadApi fileDownloadApi;
  private final PnSafeStorageConfigs cfg;
  private final RestTemplate restTemplate;

  public PnSafeStorageClientImpl(RestTemplate rest, PnSafeStorageConfigs cfg) {
    ApiClient newApiClient = new ApiClient(rest);
    newApiClient.setBasePath(cfg.getSafeStorageBaseUrl());
    this.fileUploadApi = new FileUploadApi(newApiClient);
    this.fileDownloadApi = new FileDownloadApi(newApiClient);
    this.cfg = cfg;
    this.restTemplate = rest;
  }


  @Override
  public AuditStorage uploadFile(AuditStorage audit) {
    String mediaType = audit.exportType().getMediaType();
    try {
      String sha256 = FilesUtils.computeSha256(audit.filePath());
      log.info("Send fileCreationRequest for file {}", audit.filePath().toString());
      FileCreationResponse res = createFile(sha256, mediaType, cfg.getStorageDocumentType(audit));

      log.info("Send fileContent to received url {}", res.getUploadUrl());
      this.uploadContent(res, sha256, audit.filePath(), mediaType);

      log.info("File {} sent successfully. SafeStorage key {}", audit.fileName(), res.getKey());

      return audit.uploadKey(res.getKey());

    } catch (Exception e) {
      log.error("Exception on upload file {}", audit.filePath().toString());
      return audit.error(e);
    }

  }


  private FileCreationResponse createFile(String sha256, String mediaType, String docType) {
    FileCreationRequest fileCreationRequest = new FileCreationRequest();
    fileCreationRequest.setContentType(mediaType);
    fileCreationRequest.setDocumentType(docType);
    fileCreationRequest.setStatus(SAVED);
    try {

      return fileUploadApi.createFile(this.cfg.getSafeStorageCxId(), "SHA-256", sha256,
          fileCreationRequest);

    } catch (Exception ee) {
      log.error("Exception on SafeStorage createFile: {} ", ee.getMessage());
      throw new ExternalException("SafeStorage create file error", ee);
    }
  }

  private void uploadContent(FileCreationResponse fileCreationResponse, String sha256,
      Path resource, String mediaType) {

    try {

      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add(HttpHeaders.CONTENT_TYPE, mediaType);
      headers.add("x-amz-checksum-sha256", sha256);
      headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

      HttpEntity<Resource> req = new HttpEntity<>(new FileSystemResource(resource), headers);

      URI url = URI.create(fileCreationResponse.getUploadUrl());
      HttpMethod method =
          fileCreationResponse.getUploadMethod() == FileCreationResponse.UploadMethodEnum.POST
              ? HttpMethod.POST
              : HttpMethod.PUT;

      ResponseEntity<String> res = restTemplate.exchange(url, method, req, String.class);

      if (res.getStatusCode() != org.springframework.http.HttpStatus.OK) {
        String body = res.getBody();
        log.error("SafeStorage return status {} message: '{}'", res.getStatusCodeValue(), body);
        throw new ExternalException(String.format("File upload failed. Received error code %s",
            res.getStatusCode().toString()));
      }
    } catch (Exception ee) {
      log.error("Exception on uploadContent file {}", resource, toString());
      throw new ExternalException("Exception uploading file", ee);
    }
  }


  @Override
  public AuditDownloadReference downloadFileInfo(AuditDownloadReference audit) {
    try {
      log.info("Read download info from SafeStorage for file {}", audit.fileName());

      FileDownloadResponse res =
          fileDownloadApi.getFile(audit.uploadKey(), this.cfg.getSafeStorageCxId(), Boolean.FALSE);
      log.info("Download info {} readed successfully. SafeStorage key {}", audit.fileName(),
          res.getKey());
      return audit.size(res.getContentLength()).downloadUrl(res.getDownload().getUrl());

    } catch (Exception e) {
      log.error("Exception on downloadFileInfo {}", audit.fileName());
      audit.error(e);
      return audit;
    }

  }

  @Override
  public AuditDownloadReference downloadFile(AuditDownloadReference audit,
      UnaryOperator<AuditDownloadReference> downloadFunction) {
    try {
      log.info("Download from SafeStorage for file {}", audit.fileName());

      return restTemplate.execute(URI.create(audit.downloadUrl()), HttpMethod.GET, null,
          clientHttpResponse -> {
            HttpStatus respStatus = clientHttpResponse.getStatusCode();
            if (respStatus == HttpStatus.OK) {
              audit.content(clientHttpResponse.getBody());
              return downloadFunction.apply(audit);
            } else {
              throw new ExternalException(
                  String.format("File not retrived. Url: %s, ResponseCode: %s", audit.downloadUrl(),
                      respStatus.name()));
            }
          });

    } catch (Exception e) {
      log.error("Exception on download file {}", audit.fileName());
      audit.error(e);
      return audit;
    }

  }

}
