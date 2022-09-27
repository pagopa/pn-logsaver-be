package it.pagopa.pn.logsaver.services.impl.functions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.functions.ExportAudit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExportAuditPdf implements ExportAudit {
  private static final String START_XML_AUDIT =
      "<audit date=\"%s\" fileName=\"%s\" retention=\"%s\"><![CDATA[";
  private static final byte[] END_XML_AUDIT = "]]></audit>".getBytes();

  private static final String TITLE = "PagoPa - Piattaforma Notifiche";
  private static final String SUBJECT = "File di Audit - Retention %s del %s";
  private static final String CREATOR = "pn-log-saver";
  private static final String PARAGRAPH =
      "Questo pdf contiente i file di audit della Piattaforma Notifiche.";


  @Override
  public void export(Path folder, Path file, Retention retention, LocalDate logDate) {
    log.info("Creating pdf {} for folder {}", file.getFileName().toString(), folder.toString());
    try (Document document = new Document();) {
      PdfWriter writer = PdfWriter.getInstance(document,
          Files.newOutputStream(file, StandardOpenOption.APPEND, StandardOpenOption.CREATE));

      document.addTitle(TITLE);
      document.addSubject(String.format(SUBJECT, retention.getText(), logDate.toString()));
      document.addCreator(CREATOR);
      document.addAuthor(CREATOR);
      document.addProducer();
      document.addCreationDate();


      Stream.of(folder.toFile().listFiles()).forEach(filePath -> {
        document.addHeader(filePath.getName(),
            handleXmlContent(filePath.toPath(), retention, logDate));
        writer.flush();
      });

      document.open();
      document.add(new Paragraph(PARAGRAPH));
    } catch (IOException de) {
      log.error("Error create pdf {} for folder {}", file.getFileName().toString(),
          folder.toString());
      throw new FileSystemException("Error create pdf.", de);
    }
  }

  private String handleXmlContent(Path filePath, Retention retention, LocalDate logDate) {
    try {
      InputStream start = new ByteArrayInputStream(String.format(START_XML_AUDIT,
          logDate.toString(), filePath.getFileName().toString(), retention.name()).getBytes());
      InputStream content = new ByteArrayInputStream(Files.readAllBytes(filePath));
      InputStream xml = new SequenceInputStream(start,
          new SequenceInputStream(content, new ByteArrayInputStream(END_XML_AUDIT)));

      return IOUtils.toString(xml, Charset.defaultCharset());
    } catch (IOException e) {
      log.error("Error create xml metadata pdf {} for retention {}",
          filePath.getFileName().toString(), retention.name());
      throw new FileSystemException("Error read file: " + e.getMessage());
    }
  }


}
