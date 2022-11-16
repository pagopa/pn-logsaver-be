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
import org.apache.commons.io.IOUtils;
import org.springframework.util.unit.DataSize;
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
  private static final long METADATA_EMPTY = 1500L;
  private final Retention retention;
  private PdfWriter writer;
  private long fileSize = 0L;
  private String nextEntry;
  private final LocalDate logDate;

  public PdfExportMultipart(@NonNull Path folderIn, @NonNull DataSize maxSize,
      @NonNull Path folderOut, @NonNull String patternFileOut, @NonNull Retention retention,
      @NonNull LocalDate logDate) {
    super(folderIn, maxSize, folderOut, patternFileOut);
    this.retention = retention;
    this.logDate = logDate;
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
  protected void setFileOut(Path fileOut) throws IOException {
    this.fileSize = FILE_SIZE_EMPTY;

    this.currentFileOut = new Document();
    writer = PdfWriter.getInstance(this.currentFileOut,
        Files.newOutputStream(fileOut, StandardOpenOption.APPEND, StandardOpenOption.CREATE));
    this.currentFileOut.addTitle(TITLE);
    this.currentFileOut.addSubject(String.format(SUBJECT, retention.getText(), logDate.toString()));
    this.currentFileOut.addCreator(CREATOR);
    this.currentFileOut.addAuthor(CREATOR);
    this.currentFileOut.addProducer();
    this.currentFileOut.addCreationDate();
    this.currentFileOut.open();
    this.currentFileOut.add(new Paragraph(PARAGRAPH));

  }

  @Override
  protected void addLogFile(File filePath) throws IOException {
    this.fileSize += nextEntry.length() + filePath.getName().length() + METADATA_EMPTY;
    currentFileOut.addHeader(filePath.getName(), nextEntry);
    writer.flush();
  }

  @Override
  protected long fileSize(Path pathFile, File nextFile) throws IOException {
    this.nextEntry = handleXmlContent(nextFile.toPath(), retention, logDate);
    return fileSize + nextEntry.length() + nextFile.getName().length() + METADATA_EMPTY;
  }

  @Override
  protected void closeCurrentFile() throws IOException {
    log.info(currentPathFile.getFileName().toString() + " END Size " + fileSize);
    currentFileOut.close();
  }


}
