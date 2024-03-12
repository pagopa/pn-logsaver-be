import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

public class TestZip {

    public static void main(String[] args) throws IOException, ZipException {
        Path fileOut = Path.of("c:\\tmp\\prova.zip");



        for (File file:new File("c:\\tmp\\input").listFiles()) {
//            ZipOutputStream zos;
//            try {
//                zos = new ZipOutputStream(
//                    Files.newOutputStream(fileOut, StandardOpenOption.APPEND));
//            }catch(Exception err){
//                zos = new ZipOutputStream(
//                    Files.newOutputStream(fileOut, StandardOpenOption.CREATE));
//            }
//            ZipEntry ze = new ZipEntry(file.getName());
//            zos.putNextEntry(ze);
//            try (FileInputStream fis = new FileInputStream(file)) {
//                IOUtils.copy(fis, zos);
//                zos.closeEntry();
//            }
//            zos.close();
            ZipFile zipFile = new ZipFile(fileOut.toFile());
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);
            zipFile.addFile(file, parameters);
            zipFile.close();
        }

    }
}
