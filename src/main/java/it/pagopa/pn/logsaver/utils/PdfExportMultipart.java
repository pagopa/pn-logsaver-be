package it.pagopa.pn.logsaver.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.commons.io.IOUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PdfExportMultipart extends AbstractExportMultipart<Document> {

  private static final String START_XML_AUDIT =
      "<audit date=\"%s\" fileName=\"%s\" retention=\"%s\"><![CDATA[";
  private static final byte[] END_XML_AUDIT = "]]></audit>".getBytes();

  private static final String TITLE = "PagoPa - Piattaforma Notifiche";
  private static final String SUBJECT = "File di Audit - Retention %s del %s";
  private static final String CREATOR = "pn-log-saver";
  private static final String PARAGRAPH =
      "Questo pdf contiente i file di audit della Piattaforma Notifiche.";
  private static final long FILE_SIZE_EMPTY = 1029L;
  private static final long METADATA_EMPTY = 2L;
  private final Retention retention;
  private PdfWriter writer;
  private long fileSize = 0L;

  private final LocalDate logDate;

  public PdfExportMultipart(@NonNull Path folderIn, @NonNull DataSize maxSize,
      @NonNull Path folderOut, @NonNull String patternFileOut, @NonNull Retention retention,
      @NonNull LocalDate logDate) {
    super(folderIn, maxSize, folderOut, patternFileOut);
    this.retention = retention;
    this.logDate = logDate;
  }



  public static void main(String[] args) {
    String pattern = "'all-audit-log-10y-'yyyy-MM-dd'_part%d".concat(".pdf'");
    String fileName = LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));
    Path in = Path.of("/tmp/10y");
    Path out = Path.of("/tmp/");
    PdfExportMultipart export = new PdfExportMultipart(in, DataSize.of(1000, DataUnit.KILOBYTES),
        out, fileName, Retention.AUDIT10Y, LocalDate.now());

    export.export();
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
      throw new UncheckedIOException("Error read file: " + e.getMessage(), e);
    }
  }

  @Override
  protected Document newOutputStream(Path fileOut) throws IOException {
    this.fileSize = FILE_SIZE_EMPTY;
    return newDocument(fileOut);
  }

  protected Document newDocument(Path fileOut) throws IOException {
    Document document = new Document();
    writer = PdfWriter.getInstance(document,
        Files.newOutputStream(fileOut, StandardOpenOption.APPEND, StandardOpenOption.CREATE));

    document.addTitle(TITLE);
    document.addSubject(String.format(SUBJECT, retention.getText(), logDate.toString()));
    document.addCreator(CREATOR);
    document.addAuthor(CREATOR);
    document.addProducer();
    document.addCreationDate();

    document.open();
    document.add(new Paragraph(PARAGRAPH));
    return document;
  }

  @Override
  protected void addFile(File filePath) throws IOException {
    String xmlContent = handleXmlContent(filePath.toPath(), retention, logDate);
    this.fileSize += xmlContent.length() + filePath.getName().length() + METADATA_EMPTY;
    currentFile.addHeader(filePath.getName(), xmlContent);
    writer.flush();
  }

  @Override
  protected long fileSize(Path pathFile) throws IOException {
    log.info(pathFile.getFileName().toString() + " Size " + fileSize);
    return fileSize;
  }

  @Override
  protected void closeCurrentFile() throws IOException {
    log.info(currentPathFile.getFileName().toString() + " END Size " + fileSize);
    currentFile.close();
  }


}
