//
// package gov.nih.nci.evs.api.support.es;
//
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
//
// import org.elasticsearch.action.get.MultiGetItemResponse;
// import org.elasticsearch.action.get.MultiGetResponse;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.data.elasticsearch.core.MultiGetResultMapper;
//
// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.ObjectMapper;
//
// import gov.nih.nci.evs.api.model.Concept;
// import gov.nih.nci.evs.api.model.IncludeParam;
//

// **************************************************************************************************
// THIS CLASS IS DEPRECATED AND NO LONGER REQUIRED AFTER UPGRADING TO
// spring-data-elasticsearch:4.2.12
// KEEPING CODE FOR REFERENCE
// **************************************************************************************************

/// **
// * The multiGet elasticsearch result mapper.
// *
// * @author Arun
// */
// public class EVSConceptMultiGetResultMapper extends BaseResultMapper
//    implements MultiGetResultMapper {
//
//  /** The Constant log. */
//  @SuppressWarnings("unused")
//  private static final Logger logger =
//      LoggerFactory.getLogger(EVSConceptMultiGetResultMapper.class);
//
//  /** the include param *. */
//  private IncludeParam ip;
//
//  /**
//   * Instantiates a {@link EVSConceptMultiGetResultMapper} from the specified
//   * parameters.
//   *
//   * @param ip the ip
//   */
//  public EVSConceptMultiGetResultMapper(IncludeParam ip) {
//    this.ip = ip;
//  }
//
//  /**
//   * see superclass *.
//   *
//   * @param <T> the
//   * @param responses the responses
//   * @param clazz the clazz
//   * @return the list
//   */
//  @SuppressWarnings("unchecked")
//  @Override
//  public <T> List<T> mapResults(MultiGetResponse responses, Class<T> clazz) {
//    List<Concept> concepts = new ArrayList<>();
//    ObjectMapper mapper = new ObjectMapper();
//    for (MultiGetItemResponse response : responses) {
//      if (!response.getResponse().isExists() || response.getResponse().isSourceEmpty())
//        continue;
//      Concept concept = null;
//      if (ip == null) {
//        try {
//          concept = mapper.readValue(response.getResponse().getSourceAsString(), Concept.class);
//        } catch (JsonProcessingException e) {
//          throw new RuntimeException("Error while processing multiGet results: " +
// e.getMessage());
//        }
//      } else {
//        Map<String, Object> sourceMap = response.getResponse().getSource();
//        applyIncludeParam(sourceMap, ip);
//        concept = mapper.convertValue(sourceMap, Concept.class);
//      }
//      concepts.add(concept);
//    }
//
//    return (List<T>) concepts;
//  }
// }
