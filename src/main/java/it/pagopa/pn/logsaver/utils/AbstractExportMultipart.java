package it.pagopa.pn.logsaver.utils;

import java.io.ByteArrayInputStream;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import org.apache.commons.io.IOUtils;
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
  private int partsOpened = 0;
  private final Set<String> writtenEntryNames = new HashSet<>();
  @NonNull
  private Path folderOut;
  @NonNull
  private String patternFileOut;
  protected T currentFileOut;
  protected Path currentPathFile;

  private Consumer<Path> onPartClosed;
  private BiConsumer<Path, Throwable> onPartDiscarded;



  protected abstract void setCurrentFileOut(Path fileOut) throws IOException;

  protected abstract void addLogFile(File filePath) throws IOException;

  protected abstract void addLogEntry(String entryName, InputStream content) throws IOException;

  protected abstract void closeCurrentFile() throws IOException;

  protected abstract long currentPartSize() throws IOException;

  protected abstract long estimatedEntrySize(String entryName, byte[] data)
      throws IOException;

  protected abstract boolean isPartEmpty();

  public void setOnPartClosed(Consumer<Path> onPartClosed) {
    this.onPartClosed = onPartClosed;
  }

  public void setOnPartDiscarded(BiConsumer<Path, Throwable> onPartDiscarded) {
    this.onPartDiscarded = onPartDiscarded;
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
          if (exceedsMaxSize(estimatedEntrySize(filePath.getName(),
              Files.readAllBytes(filePath.toPath())))) {
            closeCurrentFile();
            currentFileOut = null;
            ensureCurrentPartOpen();
          }
          addLogFile(filePath);
        }
      }
  }



  private void ensureCurrentPartOpen() throws IOException {
    if (currentFileOut == null) {
      currentPathFile = newFileOutPathPart(folderOut, patternFileOut, ++partsOpened);
      setCurrentFileOut(currentPathFile);
      outFileList.add(currentPathFile);
      writtenEntryNames.clear();
    }
  }

  public void append(String entryName, InputStream content) {
    String reservedEntryName = null;
    try {
      byte[] data = IOUtils.toByteArray(content);
      ensureCurrentPartOpen();
      if (exceedsMaxSize(estimatedEntrySize(entryName, data))) {
        finalizeCurrentPart();
        ensureCurrentPartOpen();
      }
      reservedEntryName = uniqueEntryName(entryName);
      addLogEntry(reservedEntryName, new ByteArrayInputStream(data));
    } catch (Exception e) {
      if (reservedEntryName != null) {
        discardCurrentPart(e);
      }
      log.error("Error appending entry {} to folder {}", entryName, folderOut, e);
      throw new FileSystemException("Error appending entry " + entryName, e);
    }
  }

  private void discardCurrentPart(Throwable cause) {
    Path discarded = currentPathFile;
    try {
      closeCurrentFile();
    } catch (Exception e) {
      log.warn("Cannot close the compromised part {}: {}", discarded, e.getMessage());
    }
    currentFileOut = null;
    outFileList.remove(discarded);
    writtenEntryNames.clear();
    if (onPartDiscarded != null) {
      try {
        onPartDiscarded.accept(discarded, cause);
      } catch (Exception e) {
        log.warn("Cannot notify the discarded part {}: {}", discarded, e.getMessage());
      }
    }
    try {
      Files.deleteIfExists(discarded);
    } catch (IOException e) {
      log.warn("Cannot delete the compromised part {}: {}", discarded, e.getMessage());
    }
  }

  private String uniqueEntryName(String entryName) {
    if (writtenEntryNames.add(entryName)) {
      return entryName;
    }
    String candidate;
    int suffix = 2;
    do {
      candidate = entryName + "~" + suffix++;
    } while (!writtenEntryNames.add(candidate));
    log.warn("Entry {} already present in folder {}, stored as {}. "
        + "The same source file has been processed more than once.", entryName, folderOut,
        candidate);
    return candidate;
  }

  public void closeStream() {
    try {
      if (currentFileOut != null) {
        finalizeCurrentPart();
      }
    } catch (Exception e) {
      log.error("Error closing stream for folder {}", folderOut, e);
      throw new FileSystemException("Error closing stream for folder " + folderOut, e);
    }
  }

  private boolean exceedsMaxSize(long estimatedEntrySize) throws IOException {
    return !isPartEmpty() && currentPartSize() + estimatedEntrySize > maxSize.toBytes();
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
