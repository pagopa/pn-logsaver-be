package it.pagopa.pn.logsaver.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
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
  protected T currentFile;
  protected Path currentPathFile;



  protected abstract T newOutputStream(Path fileOut) throws IOException;

  protected abstract void addFile(File filePath) throws IOException;

  protected abstract void closeCurrentFile() throws IOException;


  public List<Path> export() {
    log.info("Creating files for folder {}", folderIn.toString());

    try {
      currentPathFile = newFileOutPathPart(folderOut, patternFileOut, 1);
      currentFile = newOutputStream(currentPathFile);
      outFileList.add(currentPathFile);
      exportFolder(folderIn.toFile());
      closeCurrentFile();
      return outFileList;
    } catch (Exception e) {
      log.error("Error creating files for folder {}", folderIn.toString());
      throw new FileSystemException("", e);
    }
  }



  private void exportFolder(File pathIn) throws IOException {

    for (File filePath : pathIn.listFiles()) {
      log.trace("export file {} ", filePath.getPath());
      if (filePath.isDirectory()) {
        exportFolder(filePath);
      } else {

        if (fileSize(currentPathFile) > maxSize.toBytes()) {
          closeCurrentFile();
          currentPathFile = newFileOutPathPart(folderOut, patternFileOut, outFileList.size() + 1);
          outFileList.add(currentPathFile);
          currentFile = newOutputStream(currentPathFile);
        }
        addFile(filePath);

      }
    }
  }



  protected long fileSize(Path pathFile) throws IOException {
    return pathFile.toFile().exists()
        ? Files.readAttributes(pathFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
            .size()
        : -1L;
  }

  private static Path newFileOutPathPart(Path zipPathout, String patternFileOut, int nPart) {
    String fileName = String.format(patternFileOut, nPart);
    return zipPathout.resolve(fileName);
  }

}
