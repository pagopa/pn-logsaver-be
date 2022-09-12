package it.pagopa.pn.logsaver;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonStreamParser;
import com.google.gson.JsonSyntaxException;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class LogSaverApplication2 {


  public static void main(String[] args) {
    // String fileName = FilenameUtils.getBaseName(
    // "logs/ecs/pnDelivery/2022/07/11/12/pn-pnDelivery-ecs-delivery-stream-1-2022-07-11-12-56-15-53414149-a1c3-4053-bb1a-318423ee8ddf");

    // String tt = Paths.get("s3:////tp/fff/", "/test", "prova/", "ciao").getFileName().toString();

    // String dateSuffix =
    // LocalDate.now().format(DateTimeFormatter.ofPattern("s/ecs/s/yyyy/MM/dd"));
    // System.out.print(dateSuffix);

    // TransferManagerUtils.
    // JsonToken.

    // parseTest();


    Gson gson = new Gson();
    try {


      parseTest();

    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JsonIOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JsonSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  private static void parseTest() throws FileNotFoundException {


    Iterator<JsonElement> pa = new JsonStreamParser(new FileReader(
        "/tmp/logparser/20220711/10y/pn-pnDelivery-ecs-delivery-stream-1-2022-07-11-12-56-15-53414149-a1c3-4053-bb1a-318423ee8ddf"));
    // Flux<JsonElement> ret =


    Iterable<JsonElement> iterable = getIterableFromIterator(pa);

    Flux<String> ret = Flux.fromIterable(iterable).map(JsonElement::toString).doOnNext(node -> {

      FilesUtils.writeFile(node.getBytes(), "test", Path.of("/tmp/logparser/"));
    });

    Mono.when(ret).block();
  }

  public static <T> Iterable<T> getIterableFromIterator(Iterator<T> iterator) {
    return () -> iterator;
  }


  private static void parseTest2() {
    ObjectMapper mapper = new ObjectMapper();
    Flux<JsonNode> ret = getBuffer().flatMap(buffer -> {
      InputStream in = buffer.asInputStream();
      JsonNode node = null;
      try {
        node = mapper.readTree(in);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return Flux.just(node);
    });

    ret.doOnNext(node -> {

      FilesUtils.writeFile(node.toPrettyString().getBytes(), "test", Path.of("/tmp/logparser/"));
    });

    Mono.when(ret).block();
  }



  private static Flux<DataBuffer> getBuffer() {
    return DataBufferUtils.read(Path.of(
        "/tmp/logparser/20220711/10y/pn-pnDelivery-ecs-delivery-stream-1-2022-07-11-12-56-15-53414149-a1c3-4053-bb1a-318423ee8ddf"),
        new DefaultDataBufferFactory(), 1024, StandardOpenOption.READ);
  }

}

