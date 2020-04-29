
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.properties.ElasticServerProperties;

/**
 * Reference implementation of {@link ElasticSearchService}. Includes hibernate
 * tags for MEME database.
 */
@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(ElasticSearchServiceImpl.class);

  /** The elastic query builder. */
  @Autowired
  ElasticQueryBuilder elasticQueryBuilder;

  /** The rest template. */
  @Autowired
  RestTemplate restTemplate;

  /** The elastic server properties. */
  @Autowired
  ElasticServerProperties elasticServerProperties;

  /** The object mapper. */
  private static ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Construct return response.
   *
   * @param responseStr the response str
   * @param searchCriteria the filter criteria elastic fields
   * @return the string
   * @throws JsonProcessingException the json processing exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unused")
  private ConceptResultList constructReturnResponse(String responseStr,
    SearchCriteria searchCriteria) throws JsonProcessingException, IOException {
    String returnStr = "";

    // convert to Tree Node for manipulation
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(responseStr);

    // get the total hits
    JsonNode totalHits = jsonNode.path("hits").path("total");

    final ConceptResultList result = new ConceptResultList();
    result.setTotal(totalHits.asInt());
    result.setParameters(searchCriteria);

    // Iterate over hits
    for (final JsonNode node : jsonNode.get("hits").get("hits")) {
      final JsonNode source = node.get("_source");
      final Concept concept = new Concept(source.get("Code").asText());

      final JsonNode highlightNode = node.get("highlight");
      // Only if there are highlights
      if (highlightNode != null) {
        final Iterator<Map.Entry<String, JsonNode>> fields = highlightNode.fields();
        final Set<String> highlights = new HashSet<>();
        while (fields.hasNext()) {
          final JsonNode values = fields.next().getValue();
          for (final JsonNode value : values) {
            highlights.add(value.asText());
          }
        }
        for (final String highlight : highlights) {
          concept.getHighlights().put(highlight.replaceAll("<em>", "").replaceAll("</em>", ""),
              highlight);
        }
      }
      result.getConcepts().add(concept);

    }

    return result;
  }

  /**
   * Inits the settings.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws HttpClientErrorException the http client error exception
   */
  public void initSettings() throws IOException, HttpClientErrorException {
    // get the server url
    String url = elasticServerProperties.getUrl();

    // Call the elastic search url
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    HttpEntity<String> requestbody =
        new HttpEntity<>("{\"max_result_window\":500000}", httpHeaders);

    restTemplate.exchange(url.replace("concept/_search", "_settings"), HttpMethod.PUT, requestbody,
        String.class);
  }

  /**
   * Search.
   *
   * @param searchCriteria the search criteria
   * @return the concept result list
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws HttpClientErrorException the http client error exception
   */
  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public ConceptResultList search(SearchCriteria searchCriteria)
    throws IOException, HttpClientErrorException {
    String responseStr = "";

    // construct the query
    String query = elasticQueryBuilder.constructQuery(searchCriteria);

    // get the server url
    String url = elasticServerProperties.getUrl();

    // Call the elastic search url
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    HttpEntity<String> requestbody = new HttpEntity<>(query, httpHeaders);

    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.POST, requestbody, String.class);
    // log.debug("response = " + response);
    HttpStatus statusCode = response.getStatusCode();
    log.debug("statusCode = " + statusCode);
    String responseBody = response.getBody();
    // log.debug("responseBody = " + responseBody);

    if (responseBody != null) {
      Map<String, Object> responseMap =
          (Map<String, Object>) objectMapper.readValue(responseBody, Map.class);
      Object error = responseMap.get("error");
      log.debug("error = " + error);
      // error handling
      if (error != null) {
        if ((statusCode.toString().equals(HttpStatus.BAD_REQUEST.toString()))) {
          log.debug("statusCode.toString() is equal to HttpStatus.BAD_REQUEST.toString()");
          String errorDescription = (String) responseMap.get("error_description");
          log.debug("errorDescription = " + errorDescription);
          String message = (String) responseMap.get("message");
          log.debug("message = " + message);
          log.debug("throw HttpClientErrorException");
          throw new HttpClientErrorException(statusCode, message);

        } else if ((statusCode.toString().equals(HttpStatus.NOT_FOUND.toString()))) {
          log.debug("statusCode.toString() is equal to HttpStatus.NOT_FOUND.toString()");
          String message = (String) responseMap.get("message");
          log.debug("message = " + message);
          log.debug("throw HttpClientErrorException");
          throw new HttpClientErrorException(statusCode, message);

        } else {
          String message = (String) responseMap.get("message");
          log.debug("message = " + message);
          log.debug("throw HttpClientErrorException");
          throw new HttpClientErrorException(statusCode, message);
        }

      } else {
        // get the response
        responseStr = response.getBody();
      }
    }

    // construct return response
    return constructReturnResponse(responseStr, searchCriteria);

  }

}
