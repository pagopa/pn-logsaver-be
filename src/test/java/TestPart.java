import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestPart {

    public static void main(String[] args){
        String patternFileOut = "audit-log-10y-2023-03-02_part%03d.zip";
        File dir = new File("C:\\tmp\\input");
        Optional<Long> maxPart=Arrays.stream(dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(patternFileOut.replace("%03d","[0-9]+"));
            }
        })).map(file -> Long.valueOf( file.getName().substring(file.getName().length() -5, file.getName().length() -4))
        ).max(Long::compareTo);

        // Se non c'è creo il primo, altrimenti prendo l'ultimo
//        if (files.length == 0) {
//            return newFileOutPathPart(zipPathout, patternFileOut, 1);
//        } else {
//            return newFileOutPathPart(zipPathout, patternFileOut, files.length);
//        }
        System.out.println(maxPart.get());
        System.out.println("audit-log-10y-2023-03-02_part004.zip".substring(patternFileOut.indexOf("%03d"), patternFileOut.indexOf("%03d")+3));
    }
}
