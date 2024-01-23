package it.pagopa.pn.logsaver.utils;

import it.pagopa.pn.logsaver.services.FileCompleteListener;
import java.io.File;
import java.io.FilenameFilter;
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
import java.util.Optional;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
abstract class AbstractExportMultipart<T> {
  protected double compressionFactor = 1.0;
  @NonNull
  protected Path folderIn;
  @NonNull
  private DataSize maxSize;

  private List<Path> outFileList = new ArrayList<>();
  @NonNull
  private Path folderOut;
  @NonNull
  private String patternFileOut;
  private final FileCompleteListener fileCompleteListener;
  protected T currentFileOut;
  protected Path currentPathFile;



  protected abstract void setCurrentFileOut(Path fileOut) throws IOException;

  protected abstract void addLogFile(File filePath) throws IOException;

  protected abstract void closeCurrentFile() throws IOException;


  public List<Path> export() {
    log.info("Creating files for folder {}", folderIn.toString());

    try {
      currentPathFile = newFileOutPathPart(folderOut, patternFileOut);
      setCurrentFileOut(currentPathFile);//Apre stream in scrittura
      if (!outFileList.contains(currentPathFile)) {
        outFileList.add(currentPathFile);
      }
      exportFolder(folderIn.toFile());
      closeCurrentFile();
      return outFileList;
    } catch (Exception e) {
      log.error("Error creating files for folder {}", folderIn.toString());
      throw new FileSystemException("", e);
    }
  }

  public void init(){
    try {
      currentPathFile = newFileOutPathPart(folderOut, patternFileOut, 1);
    } catch (Exception e) {
      log.error("Error creating or opening file ", e);
      throw new FileSystemException("", e);
    }
  }

  public void close(){
      try {
          closeCurrentFile();
      } catch (IOException e) {
        log.error("Error closing currentFile", e);
        throw new RuntimeException(e);
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
          if (fileSize(currentPathFile) > maxSize.toBytes()) {
            closeCurrentFile();
            fileCompleteListener.notify(currentPathFile);
            currentPathFile = newFileOutPathPart(folderOut, patternFileOut);
            outFileList.add(currentPathFile);
            setCurrentFileOut(currentPathFile);
          }
          addLogFile(filePath);
        }
      }
  }


  protected long fileSize(Path pathFile) throws IOException {
    return pathFile.toFile().exists() ? Files.readAttributes(pathFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
        .size()
        : -1L;
  }
  private static Path newFileOutPathPart(Path zipPathout, String patternFileOut, int nPart) {
    String fileName = String.format(patternFileOut, nPart);
    return zipPathout.resolve(fileName);
  }
  private Path newFileOutPathPart(Path zipPathout, String patternFileOut) {

    final int start = patternFileOut.indexOf("%03d");
    File dir = zipPathout.toFile();
    Optional<Integer> maxPart=Arrays.stream(dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.matches(patternFileOut.replace("%03d","[0-9]+"));
      }
    })).map(file -> Integer.valueOf( file.getName().substring(start, start + 3))
    ).max(Integer::compareTo);

    int maxValue = maxPart.isPresent() ? maxPart.get() : 0;

      try {
        Path path = newFileOutPathPart(zipPathout, patternFileOut, maxValue);
        if (maxValue > 0 && fileSize(path) < maxSize.toBytes()){
          return path;
        }else {
          if (maxValue>0){
            fileCompleteListener.notify(path);
          }
          return newFileOutPathPart(zipPathout, patternFileOut, maxValue + 1);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
  }
}
