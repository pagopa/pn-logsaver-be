package it.pagopa.pn.logsaver.client.safestorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.UnaryOperator;
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
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse.UploadMethodEnum;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.springbootcfg.PnSafeStorageConfigs;

@ExtendWith(MockitoExtension.class)
class PnSafeStorageClientImplTest {

  @Mock
  private FileUploadApi fileUploadApi;

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
    cfg = new PnSafeStorageConfigs();
    cfg.setSafeStorageBaseUrl("http://localhost");
    cfg.setSafeStorageDocTypesPdf(Map.of("10y", "PN_LOGS_PDF_AUDIT10Y", "5y", "PN_LOGS_PDF_AUDIT5Y",
        "120d", "PN_LOGS_PDF_TEMP"));
    cfg.setSafeStorageDocTypesZip(Map.of("10y", "PN_LOGS_ARCHIVE_AUDIT10Y", "5y",
        "PN_LOGS_ARCHIVE_AUDIT5Y", "120d", "PN_LOGS_ARCHIVE_TEMP"));
    cfg.setSafeStorageCxId("1234");
    Method init = ReflectionUtils.findMethod(PnSafeStorageConfigs.class, "initConf");
    ReflectionUtils.makeAccessible(init);
    ReflectionUtils.invokeMethod(init, cfg);

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
  void downloadFileInfo() throws IOException {

    FileDownloadResponse respCF = new FileDownloadResponse();
    respCF.setKey("updKey");
    respCF.setContentType("application/pdf");
    respCF.setDocumentStatus("SAVED");
    respCF.setDocumentType("PN_LOGS_PDF_AUDIT10Y");
    FileDownloadInfo info = new FileDownloadInfo();
    info.setUrl("http://");
    respCF.setDownload(info);


    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(respCF));


    AuditDownloadReference req = AuditDownloadReference.builder().exportType(ExportType.PDF_SIGNED)
        .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).status(AuditStorageStatus.SENT)
        .uploadKey("updKey").build();

    AuditDownloadReference res = client.downloadFileInfo(req);

    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertEquals("http://", res.downloadUrl());
  }

  @Test
  void downloadFileInfo_InternalServerError() throws IOException {

    FileDownloadResponse respCF = new FileDownloadResponse();
    respCF.setKey("updKey");
    respCF.setContentType("application/pdf");
    respCF.setDocumentStatus("SAVED");
    respCF.setDocumentType("PN_LOGS_PDF_AUDIT10Y");
    FileDownloadInfo info = new FileDownloadInfo();
    info.setUrl("http://");
    respCF.setDownload(info);


    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.notFound().build());


    AuditDownloadReference req = AuditDownloadReference.builder().exportType(ExportType.PDF_SIGNED)
        .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).status(AuditStorageStatus.SENT)
        .uploadKey("updKey").build();

    AuditDownloadReference res = client.downloadFileInfo(req);

    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertEquals(RestClientException.class, res.error().getClass());
  }


  @Test
  void downloadFile() throws IOException {
    AuditDownloadReference mock = AuditDownloadReference.builder().exportType(ExportType.PDF_SIGNED)
        .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).status(AuditStorageStatus.SENT)
        .uploadKey("updKey").build();
    when(restTemplate.execute(any(URI.class), any(HttpMethod.class), any(), any()))
        .thenReturn(mock);

    AuditDownloadReference req = AuditDownloadReference.builder().exportType(ExportType.PDF_SIGNED)
        .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).status(AuditStorageStatus.SENT)
        .downloadUrl("https://test.it/").uploadKey("updKey").build();

    AuditDownloadReference res = client.downloadFile(req, UnaryOperator.identity());

    verify(restTemplate, times(1)).execute(any(URI.class), any(HttpMethod.class), any(), any());
    assertNotNull(res);

  }


  @Test
  void downloadFile_Error() throws IOException {

    AuditDownloadReference req = AuditDownloadReference.builder().exportType(ExportType.PDF_SIGNED)
        .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).status(AuditStorageStatus.SENT)
        .uploadKey("updKey").build();

    AuditDownloadReference res = client.downloadFile(req, UnaryOperator.identity());

    assertNotNull(res);
    assertNotNull(res.error());
  }
}
