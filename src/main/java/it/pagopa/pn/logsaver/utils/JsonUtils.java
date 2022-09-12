package it.pagopa.pn.logsaver.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtils {

  public static List<JsonNode> parseJsonStream(byte[] jsonStream) {

    ObjectMapper mapper = new ObjectMapper();
    List<JsonNode> ret = new ArrayList<>();
    try (MappingIterator<JsonNode> it = mapper.readerFor(JsonNode.class).readValues(jsonStream)) {
      ret.addAll(it.readAll());
    } catch (IOException e) {

      throw new RuntimeException(e);
    }
    return ret;
  }



  public static Map<String, Object> parseJson(byte[] jsonStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> ret =
        mapper.readValue(jsonStream, new TypeReference<Map<String, Object>>() {});
    return ret;
  }

}
