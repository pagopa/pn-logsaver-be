package it.pagopa.pn.logsaver.client.safestorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse.UploadMethodEnum;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.springbootcfg.PnSafeStorageConfigs;

@ExtendWith(MockitoExtension.class)
class PnSafeStorageClientImplTest {

  @Mock
  private FileUploadApi fileUploadApi;
  @Mock
  private PnSafeStorageConfigs cfg;
  @Mock
  private RestTemplate restTemplate;

  private PnSafeStorageClient client;

  @Captor
  private ArgumentCaptor<HttpEntity<Resource>> httpEntity;

  @Captor
  private ArgumentCaptor<RequestEntity<FileCreationRequest>> httpEntityPre;


  @BeforeEach
  public void createService() {
    when(cfg.getSafeStorageBaseUrl()).thenReturn("http://localhost");
    this.client = new PnSafeStorageClientImpl(restTemplate, cfg);
  }

  @Test
  void uploadFile() throws IOException {
    File file = new File("/tmp/test.pdf");

    FileUtils.writeStringToFile(file, "test", Charset.defaultCharset());

    FileCreationResponse respCF = new FileCreationResponse();
    respCF.setKey("KEY");
    respCF.setSecret("SECRET");
    respCF.setUploadMethod(UploadMethodEnum.PUT);
    respCF.setUploadUrl("URL");

    when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), httpEntity.capture(),
        any(Class.class))).thenReturn(ResponseEntity.ok(""));

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(respCF));


    when(cfg.getSafeStorageCxId()).thenReturn("1234");

    AuditStorage req = AuditStorage.builder().exportType(ExportType.PDF_SIGNED)
        .filePath(file.toPath()).logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).build();

    AuditStorage res = client.uploadFile(req);

    assertEquals("application/pdf", httpEntityPre.getValue().getBody().getContentType());
    assertEquals("PN_LOGS_PDF_AUDIT10Y", httpEntityPre.getValue().getBody().getDocumentType());
    assertEquals("SAVED", httpEntityPre.getValue().getBody().getStatus());

    String hash = httpEntityPre.getValue().getHeaders().get("x-checksum-value").get(0);

    assertEquals(hash, httpEntity.getValue().getHeaders().get("x-amz-checksum-sha256").get(0));
    assertEquals("SECRET", httpEntity.getValue().getHeaders().get("x-amz-meta-secret").get(0));

    verify(restTemplate, times(1)).exchange(any(URI.class), any(HttpMethod.class),
        any(HttpEntity.class), any(Class.class));
    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertEquals("KEY", res.uploadKey());

    Files.delete(file.toPath());
  }


  @Test
  void uploadFile_CreateFileInternalServerError() throws IOException {
    File file = new File("/tmp/test.pdf");

    FileUtils.writeStringToFile(file, "test", Charset.defaultCharset());

    FileCreationResponse respCF = new FileCreationResponse();
    respCF.setKey("KEY");
    respCF.setSecret("SECRET");
    respCF.setUploadMethod(UploadMethodEnum.PUT);
    respCF.setUploadUrl("URL");

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.internalServerError().body(""));


    when(cfg.getSafeStorageCxId()).thenReturn("1234");

    AuditStorage req = AuditStorage.builder().exportType(ExportType.PDF_SIGNED)
        .filePath(file.toPath()).logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).build();

    AuditStorage res = client.uploadFile(req);

    assertEquals("application/pdf", httpEntityPre.getValue().getBody().getContentType());
    assertEquals("PN_LOGS_PDF_AUDIT10Y", httpEntityPre.getValue().getBody().getDocumentType());
    assertEquals("SAVED", httpEntityPre.getValue().getBody().getStatus());

    String hash = httpEntityPre.getValue().getHeaders().get("x-checksum-value").get(0);


    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertNotNull(res.error());

    Files.delete(file.toPath());
  }



  @Test
  void uploadFile_UploadContentInternalServerError() throws IOException {
    File file = new File("/tmp/test.pdf");

    FileUtils.writeStringToFile(file, "test", Charset.defaultCharset());

    FileCreationResponse respCF = new FileCreationResponse();
    respCF.setKey("KEY");
    respCF.setSecret("SECRET");
    respCF.setUploadMethod(UploadMethodEnum.POST);
    respCF.setUploadUrl("URL");

    when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), httpEntity.capture(),
        any(Class.class))).thenReturn(ResponseEntity.internalServerError().body(""));

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(respCF));


    when(cfg.getSafeStorageCxId()).thenReturn("1234");

    AuditStorage req = AuditStorage.builder().exportType(ExportType.PDF_SIGNED)
        .filePath(file.toPath()).logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).build();

    AuditStorage res = client.uploadFile(req);

    assertEquals("application/pdf", httpEntityPre.getValue().getBody().getContentType());
    assertEquals("PN_LOGS_PDF_AUDIT10Y", httpEntityPre.getValue().getBody().getDocumentType());
    assertEquals("SAVED", httpEntityPre.getValue().getBody().getStatus());

    String hash = httpEntityPre.getValue().getHeaders().get("x-checksum-value").get(0);

    assertEquals(hash, httpEntity.getValue().getHeaders().get("x-amz-checksum-sha256").get(0));
    assertEquals("SECRET", httpEntity.getValue().getHeaders().get("x-amz-meta-secret").get(0));

    verify(restTemplate, times(1)).exchange(any(URI.class), any(HttpMethod.class),
        any(HttpEntity.class), any(Class.class));
    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertNotNull(res.error());

    Files.delete(file.toPath());
  }


  @Test
  void uploadFile_Exception() throws IOException {
    File file = new File("/tmp/test.pdf");

    FileUtils.writeStringToFile(file, "test", Charset.defaultCharset());

    FileCreationResponse respCF = new FileCreationResponse();
    respCF.setKey("KEY");
    respCF.setSecret("SECRET");
    respCF.setUploadMethod(UploadMethodEnum.POST);
    respCF.setUploadUrl("URL");

    when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), httpEntity.capture(),
        any(Class.class))).thenThrow(RuntimeException.class);

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(respCF));


    when(cfg.getSafeStorageCxId()).thenReturn("1234");

    AuditStorage req = AuditStorage.builder().exportType(ExportType.PDF_SIGNED)
        .filePath(file.toPath()).logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).build();

    AuditStorage res = client.uploadFile(req);

    assertEquals("application/pdf", httpEntityPre.getValue().getBody().getContentType());
    assertEquals("PN_LOGS_PDF_AUDIT10Y", httpEntityPre.getValue().getBody().getDocumentType());
    assertEquals("SAVED", httpEntityPre.getValue().getBody().getStatus());

    String hash = httpEntityPre.getValue().getHeaders().get("x-checksum-value").get(0);

    assertEquals(hash, httpEntity.getValue().getHeaders().get("x-amz-checksum-sha256").get(0));
    assertEquals("SECRET", httpEntity.getValue().getHeaders().get("x-amz-meta-secret").get(0));

    verify(restTemplate, times(1)).exchange(any(URI.class), any(HttpMethod.class),
        any(HttpEntity.class), any(Class.class));
    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertNotNull(res.error());

    Files.delete(file.toPath());
  }
}
