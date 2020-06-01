package gov.nih.nci.evs.api.util;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.ConceptMinimal;

public class ElasticObjectUtils {
  public static byte[] serialize(Object obj) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsBytes(obj);
  }

  public static List<ConceptMinimal> deserializeConceptMinimalList(byte[] data) throws IOException, ClassNotFoundException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(data, new TypeReference<List<ConceptMinimal>>(){});
  }
}
