
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.nci.evs.api.properties.ElasticServerProperties;
import gov.nih.nci.evs.api.support.FilterCriteriaElasticFields;
import gov.nih.nci.evs.api.support.SearchCriteria;

/**
 * Reference implementation of {@link ElasticSearchService}. Includes hibernate
 * tags for MEME database.
 */
@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

  /** The Constant log. */
  private static final Logger log =
      LoggerFactory.getLogger(ElasticSearchServiceImpl.class);

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

  /* see superclass */
  @SuppressWarnings("unchecked")
  public String elasticsearch(
    FilterCriteriaElasticFields filterCriteriaElasticFields)
    throws IOException, HttpClientErrorException {
    String responseStr = "";

    // construct the query
    String query =
        elasticQueryBuilder.constructQuery(filterCriteriaElasticFields);

    // get the server url
    String url = elasticServerProperties.getUrl();

    // Call the elastic search url
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE,
        MediaType.APPLICATION_JSON_UTF8_VALUE);
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
          log.debug(
              "statusCode.toString() is equal to HttpStatus.BAD_REQUEST.toString()");
          String errorDescription =
              (String) responseMap.get("error_description");
          log.debug("errorDescription = " + errorDescription);
          String message = (String) responseMap.get("message");
          log.debug("message = " + message);
          log.debug("throw HttpClientErrorException");
          throw new HttpClientErrorException(statusCode, message);

        } else if ((statusCode.toString()
            .equals(HttpStatus.NOT_FOUND.toString()))) {
          log.debug(
              "statusCode.toString() is equal to HttpStatus.NOT_FOUND.toString()");
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
    responseStr =
        constructReturnResponse(responseStr, filterCriteriaElasticFields);

    return responseStr;
  }

  /**
   * Construct return response.
   *
   * @param responseStr the response str
   * @param searchCriteria the filter criteria elastic fields
   * @return the string
   * @throws JsonProcessingException the json processing exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private String constructReturnResponse(String responseStr,
    SearchCriteria searchCriteria) throws JsonProcessingException, IOException {
    String returnStr = "";

    // convert to Tree Node for manipulation
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(responseStr);

    // get the total hits
    JsonNode totalHits = jsonNode.path("hits").path("total");
    log.info("XXX = " + jsonNode);
    // adding the highlights to the concepts
    List<JsonNode> sources = jsonNode.findValues("_source");
    // TODO:
    // List<JsonNode> highlights = jsonNode.findValues("highlight");
    // int count = 0;
    // for (JsonNode node : sources) {
    // ((ObjectNode) node).set("highlight", highlights.get(count));
    // count++;
    // }

    // create a new node
    JsonNode rootNode = mapper.createObjectNode();

    // add pagesize and from
    ObjectNode newRootNode =
        ((ObjectNode) rootNode).put("from", searchCriteria.getFromRecord());
    newRootNode = newRootNode.put("size", searchCriteria.getPageSize());
    newRootNode = (ObjectNode) newRootNode.set("total", totalHits);

    // adding the array for concept hits
    ArrayNode newArrayNode = ((ObjectNode) newRootNode).putArray("hits");

    for (JsonNode node : sources) {
      newArrayNode.add(node);
    }

    Object json = mapper.readValue(newRootNode.toString(), Object.class);
    returnStr =
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

    return returnStr;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  public String search(SearchCriteria searchCriteria)
    throws IOException, HttpClientErrorException {
    String responseStr = "";

    // construct the query
    String query = elasticQueryBuilder.constructQuery(searchCriteria);

    // get the server url
    String url = elasticServerProperties.getUrl();

    // Call the elastic search url
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE,
        MediaType.APPLICATION_JSON_UTF8_VALUE);
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
          log.debug(
              "statusCode.toString() is equal to HttpStatus.BAD_REQUEST.toString()");
          String errorDescription =
              (String) responseMap.get("error_description");
          log.debug("errorDescription = " + errorDescription);
          String message = (String) responseMap.get("message");
          log.debug("message = " + message);
          log.debug("throw HttpClientErrorException");
          throw new HttpClientErrorException(statusCode, message);

        } else if ((statusCode.toString()
            .equals(HttpStatus.NOT_FOUND.toString()))) {
          log.debug(
              "statusCode.toString() is equal to HttpStatus.NOT_FOUND.toString()");
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
    responseStr = constructReturnResponse(responseStr, searchCriteria);

    return responseStr;
  }

  /**
   * Construct return response.
   *
   * @param responseStr the response str
   * @param filterCriteriaElasticFields the filter criteria elastic fields
   * @return the string
   * @throws JsonProcessingException the json processing exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private String constructReturnResponse(String responseStr,
    FilterCriteriaElasticFields filterCriteriaElasticFields)
    throws JsonProcessingException, IOException {
    String returnStr = "";

    // convert to Tree Node for manipulation
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(responseStr);

    if (filterCriteriaElasticFields.getFormat()
        .equalsIgnoreCase("cleanWithHighlights")) {

      // get the total hits
      JsonNode totalHits = jsonNode.path("hits").path("total");

      // adding the highlights to the concepts
      List<JsonNode> sources = jsonNode.findValues("_source");
      List<JsonNode> highlights = jsonNode.findValues("highlight");
      int count = 0;
      for (JsonNode node : sources) {
        ((ObjectNode) node).set("highlight", highlights.get(count));
        count++;
      }

      // create a new node
      JsonNode rootNode = mapper.createObjectNode();

      // add pagesize and from
      ObjectNode newRootNode = ((ObjectNode) rootNode).put("from",
          filterCriteriaElasticFields.getFromRecord());
      newRootNode =
          newRootNode.put("size", filterCriteriaElasticFields.getPageSize());
      newRootNode = (ObjectNode) newRootNode.set("total", totalHits);

      // adding the array for concept hits
      ArrayNode newArrayNode = ((ObjectNode) newRootNode).putArray("hits");

      for (JsonNode node : sources) {
        newArrayNode.add(node);
      }

      Object json = mapper.readValue(newRootNode.toString(), Object.class);
      returnStr =
          mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } else if (filterCriteriaElasticFields.getFormat()
        .equalsIgnoreCase("clean")) {

      // get the total hits
      JsonNode totalHits = jsonNode.path("hits").path("total");

      // get the concepts hits
      List<JsonNode> sources = jsonNode.findValues("_source");

      // create a new node
      JsonNode rootNode = mapper.createObjectNode();

      // add pagesize and from
      ObjectNode newRootNode = ((ObjectNode) rootNode).put("from",
          filterCriteriaElasticFields.getFromRecord());
      newRootNode =
          newRootNode.put("size", filterCriteriaElasticFields.getPageSize());
      newRootNode = (ObjectNode) newRootNode.set("total", totalHits);

      // adding the array for hits
      ArrayNode newArrayNode = ((ObjectNode) newRootNode).putArray("hits");

      for (JsonNode node : sources) {
        newArrayNode.add(node);
      }

      Object json = mapper.readValue(newRootNode.toString(), Object.class);
      returnStr =
          mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } else {
      ObjectNode newNode = ((ObjectNode) jsonNode).put("from",
          filterCriteriaElasticFields.getFromRecord());
      newNode = ((ObjectNode) newNode).put("size",
          filterCriteriaElasticFields.getPageSize());
      Object json = mapper.readValue(newNode.toString(), Object.class);
      returnStr =
          mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }
    return returnStr;
  }

}
