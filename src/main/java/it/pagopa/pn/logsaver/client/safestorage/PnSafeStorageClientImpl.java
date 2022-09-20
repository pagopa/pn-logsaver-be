package it.pagopa.pn.logsaver.client.safestorage;


import java.net.URI;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import it.pagopa.pn.logsaver.exceptions.ExternalException;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.ApiClient;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.springbootcfg.PnSafeStorageConfigs;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PnSafeStorageClientImpl implements PnSafeStorageClient {
  public static final String MEDIATYPE_PDF = "application/pdf";
  public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
  public static final String SAVED = "SAVED";
  public static final String PN_AAR = "PN_AAR";

  private final FileUploadApi fileUploadApi;
  private final PnSafeStorageConfigs cfg;
  private final RestTemplate restTemplate;

  public PnSafeStorageClientImpl(RestTemplate rest, PnSafeStorageConfigs cfg) {
    ApiClient newApiClient = new ApiClient(rest);
    newApiClient.setBasePath(cfg.getSafeStorageBaseUrl());

    this.fileUploadApi = new FileUploadApi(newApiClient);
    this.cfg = cfg;
    this.restTemplate = rest;
  }


  @Override
  public AuditStorage uploadFile(AuditStorage itemUpd) {


    String sha256 = FilesUtils.computeSha256(itemUpd.filePath());

    try {
      log.info("Send fileCreationRequest for file {}", itemUpd.filePath().toString());
      FileCreationResponse res = createFile(sha256);

      log.info("Send fileContent to recevide url {}", res.getUploadUrl());
      this.uploadContent(res, sha256, null);

      log.info("File sent successfully. SafeStorage key {}", res.getKey());

      return itemUpd.uploadKey(res.getKey());

    } catch (Exception e) {
      log.error("Exception on upload file {}", itemUpd.filePath().toString());
      return itemUpd.sendingError(true);
    }

  }


  private FileCreationResponse createFile(String sha256) {
    FileCreationRequest fileCreationRequest = new FileCreationRequest();;
    fileCreationRequest.setContentType(MEDIATYPE_PDF);
    fileCreationRequest.setDocumentType(PN_LEGAL_FACTS);
    fileCreationRequest.setStatus(SAVED);
    try {

      return fileUploadApi.createFile(this.cfg.getSafeStorageCxId(), "SHA-256", sha256,
          fileCreationRequest);

    } catch (Exception ee) {
      log.error("Exception on createFile file: ", ee);
      throw new ExternalException("Create file error", ee);
    }
  }

  private void uploadContent(FileCreationResponse fileCreationResponse, String sha256,
      Path resource) {

    try {

      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("Content-type", MEDIATYPE_PDF);
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


}
