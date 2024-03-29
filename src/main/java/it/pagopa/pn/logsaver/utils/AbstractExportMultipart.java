package it.pagopa.pn.logsaver.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
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
  protected T currentFileOut;
  protected Path currentPathFile;



  protected abstract void setCurrentFileOut(Path fileOut) throws IOException;

  protected abstract void addLogFile(File filePath) throws IOException;

  protected abstract void closeCurrentFile() throws IOException;


  public List<Path> export() {
    log.info("Creating files for folder {}", folderIn.toString());

    try {
      currentPathFile = newFileOutPathPart(folderOut, patternFileOut, 1);
      setCurrentFileOut(currentPathFile);
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

	  /* In caso di assenza log si crea un file Readme.md con la descrizione della causa */
	  if (pathIn.listFiles().length == 0) {
        log.trace("log file not found for path {} ", pathIn.getPath());
		List<String> lines = Arrays.asList("Log file not found");
		Path file = Paths.get(pathIn.getPath() + File.separator + "Readme.md");
		Files.write(file, lines, StandardCharsets.UTF_8);
	  }
	  
	  for (File filePath : pathIn.listFiles()) {
        log.trace("export file {} ", filePath.getPath());
        if (filePath.isDirectory()) {
          exportFolder(filePath);
        } else {
          if (fileSize(currentPathFile, filePath) > maxSize.toBytes()) {
            closeCurrentFile();
            currentPathFile = newFileOutPathPart(folderOut, patternFileOut, outFileList.size() + 1);
            outFileList.add(currentPathFile);
            setCurrentFileOut(currentPathFile);
          }
          addLogFile(filePath);
        }
      }
  }



  protected long fileSize(Path pathFile, File nextFile) throws IOException {
    long currentSize =
        pathFile.toFile().exists()
            ? Files.readAttributes(pathFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                .size()
            : -1L;
    return currentSize + Files
        .readAttributes(nextFile.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
        .size();
  }

  private static Path newFileOutPathPart(Path zipPathout, String patternFileOut, int nPart) {
    String fileName = String.format(patternFileOut, nPart);
    return zipPathout.resolve(fileName);
  }

}
