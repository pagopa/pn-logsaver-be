package it.pagopa.pn.logsaver.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
abstract class AbstractExportMultipart<T> {
  @NonNull
  protected Path folderIn;
  @NonNull
  private DataSize maxSize;

  private List<Path> outFileList = new ArrayList<>();
  @NonNull
  private Path folderOut;
  @NonNull
  private String patternFileOut;
  protected T currentFileOut;
  protected Path currentPathFile;

  private Consumer<Path> onPartClosed;



  protected abstract void setCurrentFileOut(Path fileOut) throws IOException;

  protected abstract void addLogFile(File filePath) throws IOException;

  protected abstract void addLogEntry(String entryName, InputStream content) throws IOException;

  protected abstract void closeCurrentFile() throws IOException;

  protected abstract long currentPartSize() throws IOException;

  public void setOnPartClosed(Consumer<Path> onPartClosed) {
    this.onPartClosed = onPartClosed;
  }


  public List<Path> export() {
    log.info("Creating files for folder {}", folderIn.toString());

    try {
      exportFolder(folderIn.toFile());
      if (currentFileOut != null) {
        closeCurrentFile();
      }
      return outFileList;
    } catch (Exception e) {
      log.error("Error creating files for folder {}", folderIn.toString());
      throw new FileSystemException("", e);
    }
  }



  private void exportFolder(File pathIn) throws IOException {

    File[] children = pathIn.listFiles();
    if (children == null) {
      log.warn("listFiles returned null for path {} ", pathIn.getPath());
      return;
    }

	  /* In caso di assenza log si crea un file Readme.md con la descrizione della causa */
	  if (children.length == 0) {
        log.trace("log file not found for path {} ", pathIn.getPath());
		List<String> lines = Arrays.asList("Log file not found");
		Path file = Paths.get(pathIn.getPath() + File.separator + "Readme.md");
		Files.write(file, lines, StandardCharsets.UTF_8);
		children = new File[] {file.toFile()};
	  }

	  Arrays.sort(children, Comparator.comparing(File::getName));

	  for (File filePath : children) {
        log.trace("export file {} ", filePath.getPath());
        if (filePath.isDirectory()) {
          exportFolder(filePath);
        } else {
          ensureCurrentPartOpen();
          addLogFile(filePath);
          if (currentPartSize() > maxSize.toBytes()) {
            closeCurrentFile();
            currentFileOut = null;
          }
        }
      }
  }



  private void ensureCurrentPartOpen() throws IOException {
    if (currentFileOut == null) {
      currentPathFile = newFileOutPathPart(folderOut, patternFileOut, outFileList.size() + 1);
      setCurrentFileOut(currentPathFile);
      outFileList.add(currentPathFile);
    }
  }

  public void append(String entryName, InputStream content) {
    try {
      ensureCurrentPartOpen();
      addLogEntry(entryName, content);
      if (currentPartSize() > maxSize.toBytes()) {
        finalizeCurrentPart();
      }
    } catch (Exception e) {
      log.error("Error appending entry {} to folder {}", entryName, folderOut);
      throw new FileSystemException("", e);
    }
  }

  public void closeStream() {
    try {
      if (currentFileOut != null) {
        finalizeCurrentPart();
      }
    } catch (Exception e) {
      log.error("Error closing stream for folder {}", folderOut);
      throw new FileSystemException("", e);
    }
  }

  private void finalizeCurrentPart() throws IOException {
    closeCurrentFile();
    Path finalized = currentPathFile;
    currentFileOut = null;
    if (onPartClosed != null) {
      onPartClosed.accept(finalized);
    }
  }

  private static Path newFileOutPathPart(Path zipPathout, String patternFileOut, int nPart) {
    String fileName = String.format(patternFileOut, nPart);
    return zipPathout.resolve(fileName);
  }

}
