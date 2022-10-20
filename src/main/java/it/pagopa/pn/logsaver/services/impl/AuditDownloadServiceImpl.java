package it.pagopa.pn.logsaver.services.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.DailyAuditDownloadable;
import it.pagopa.pn.logsaver.model.DailyDownloadResult;
import it.pagopa.pn.logsaver.model.DailyDownloadResult.DailyDownloadResultBuilder;
import it.pagopa.pn.logsaver.model.DailyDownloadResultList;
import it.pagopa.pn.logsaver.services.AuditDownloadService;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class AuditDownloadServiceImpl implements AuditDownloadService {
  private static final String DOWNLOAD_PATH_PATTERN = "logsaver-download/%s/";

  private final S3BucketClient clientS3;
  private final StorageService storageService;

  @Override
  public DailyDownloadResultList downloadAudits(LocalDate from, LocalDate to,
      String destinationFolder) {
    String destFolder = StringUtils.isEmpty(destinationFolder)
        ? String.format(DOWNLOAD_PATH_PATTERN, DateUtils.dateTime())
        : destinationFolder;
    List<DailyDownloadResult> files = storageService.getAuditFile(from, to).stream()//
        .map(audits -> this.dowloadDailyAudit(audits, destFolder))//
        .collect(Collectors.toList());
    return DailyDownloadResultList.builder().results(files).build();
  }

  private DailyDownloadResult dowloadDailyAudit(DailyAuditDownloadable dailyFiles, String destFolder) {
    DailyDownloadResultBuilder resBuilder = DailyDownloadResult.builder();
    try {
      log.info("Download files for date {}", dailyFiles.logDate().toString());

      dailyFiles.audits().stream().filter(audit -> StringUtils.isNoneEmpty(audit.downloadUrl()))//
          .map(audits -> audits.destinationFolder(destFolder)).forEach(auditFile -> storageService
              .dowloadAuditFile(auditFile, this::dowloadAuditFileConsumer));

      writeDailyReport(dailyFiles.audits(), destFolder);

      return resBuilder.audit(dailyFiles).build();

    } catch (Exception e) {
      log.error("Error download audit for day " + dailyFiles.logDate().toString(), e);
      return resBuilder.error(e).build();
    }
  }

  public AuditDownloadReference dowloadAuditFileConsumer(AuditDownloadReference auditFile) {
    String destFolder = auditFile.destinationFolder().concat(auditFile.fileName());
    log.info("Upload file to {}", destFolder);
    clientS3.uploadContent(destFolder, auditFile.content(), auditFile.size().longValue(),
        auditFile.checksum());

    return auditFile;

  }


  private void writeDailyReport(List<AuditDownloadReference> audits, String destFolder) {
    if (CollectionUtils.isEmpty(audits)) {
      return;
    }
    byte[] csvBytes = handleReportCsv(audits);
    InputStream cvs = new ByteArrayInputStream(csvBytes);
    String fileName = LocalDate.now().toString().concat("-results.csv");
    clientS3.uploadContent(destFolder.concat(fileName), cvs, csvBytes.length, null);

  }


  private byte[] handleReportCsv(List<AuditDownloadReference> audits) {

    return audits.stream()
        .map(audit -> StringUtils.join(";", audit.logDate().toString(), audit.status().name(),
            audit.fileName(), audit.getErrorMessage()))
        .collect(Collectors.joining("\n")).getBytes();

  }

}
