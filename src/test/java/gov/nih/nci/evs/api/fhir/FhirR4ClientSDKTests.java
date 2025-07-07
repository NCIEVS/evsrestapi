package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.util.EVSUtils;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit FhirR4Tests. Tests the functionality of the FHIR R4 endpoint ValueSet specifically the
 * general operations.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FhirR4ClientSDKTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4ClientSDKTests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** The Parser. */
  private static IParser parser;

  /** The application properties. */
  @Autowired ApplicationProperties applicationProperties;

  /** Map to store name-raw pairs from Postman collection. */
  private Map<String, String> postmanNameToRawMap;

  /** Sets the up once. */
  @BeforeAll
  public static void setUpOnce() {
    // Instantiate parser
    parser = FhirContext.forR4().newJsonParser();
  }

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @BeforeEach
  public void setUp() throws Exception {
    // the object mapper
    ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    // Initialize the map
    postmanNameToRawMap = new HashMap<>();

    // Parse Postman collection and extract raw values
    parsePostmanCollectionAndExtractRawUrlValues(objectMapper);
  }

  /**
   * Parse the Postman collection JSON file and extract name-url pairs.
   *
   * @param objectMapper the Jackson ObjectMapper
   * @throws IOException if file reading fails
   */
  private void parsePostmanCollectionAndExtractRawUrlValues(ObjectMapper objectMapper)
      throws Exception {
    // Load the Postman collection JSON file from evsrestapi-client-SDK
    final String uri = applicationProperties.getSdkBaseUri();
    URL url = new URL(uri + "/fhir-examples/EVSRESTAPI-FHIR-R4.postman_collection.json");
    EVSUtils.getValueFromFile(
        uri + "/fhir-examples/EVSRESTAPI-FHIR-R4.postman_collection.json", "postman");
    URLConnection connection = url.openConnection();

    // Optional: Set User-Agent header (some servers require this)
    connection.setRequestProperty("User-Agent", "Java Application");

    @SuppressWarnings("resource")
    JsonNode rootNode = objectMapper.readTree(connection.getInputStream());

    // Extract all name-raw pairs
    extractNameRawPairs(rootNode);

    // Print all extracted name-raw pairs to console
    System.out.println("=== Extracted Name-Raw Pairs from Postman Collection ===");
    System.out.println("Total name-raw pairs found: " + postmanNameToRawMap.size());
    System.out.println();

    int counter = 1;
    for (Map.Entry<String, String> entry : postmanNameToRawMap.entrySet()) {
      System.out.println(counter + ". Name: \"" + entry.getKey() + "\"");
      System.out.println("   Raw:  \"" + entry.getValue() + "\"");
      System.out.println();
      counter++;
    }

    System.out.println("=== End of Name-Raw Pairs ===");
  }

  /**
   * Recursively extract name-raw pairs from the JSON node. Looks for objects with 'name' field and
   * finds the corresponding 'raw' field in request.url.raw.
   *
   * @param node the JSON node to search
   */
  private void extractNameRawPairs(JsonNode node) {
    if (node == null) {
      return;
    }

    if (node.isObject()) {
      // Check if this object has a 'name' field and a 'request' field
      if (node.has("name") && node.has("request")) {
        JsonNode nameNode = node.get("name");
        JsonNode requestNode = node.get("request");

        if (nameNode.isTextual() && requestNode.isObject()) {
          String nameValue = nameNode.asText();

          // Look for the raw URL in request.url.raw
          if (requestNode.has("url")) {
            JsonNode urlNode = requestNode.get("url");
            if (urlNode.isObject() && urlNode.has("raw")) {
              JsonNode rawNode = urlNode.get("raw");
              if (rawNode.isTextual()) {
                String rawValue = rawNode.asText();

                if (!nameValue.isEmpty() && !rawValue.isEmpty()) {
                  // Convert name to camelCase while preserving uppercase letters
                  String camelCaseName = convertToCamelCase(nameValue);
                  postmanNameToRawMap.put(camelCaseName, rawValue);
                }
              }
            }
          }
        }
      }

      // Recursively process all fields in the object
      node.fields()
          .forEachRemaining(
              entry -> {
                extractNameRawPairs(entry.getValue());
              });
    } else if (node.isArray()) {
      // Recursively process all elements in the array
      for (JsonNode arrayElement : node) {
        extractNameRawPairs(arrayElement);
      }
    }
  }

  /**
   * Convert a string to camelCase while preserving existing uppercase letters.
   *
   * @param input the input string to convert
   * @return the camelCase version of the string
   */
  private String convertToCamelCase(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    // Split on whitespace, punctuation, and special characters but preserve the delimiters info
    String[] words = input.split("[\\s\\-_.$]+");
    StringBuilder camelCase = new StringBuilder();

    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      if (word.isEmpty()) {
        continue;
      }

      if (i == 0) {
        // First word: lowercase the first character unless the whole word is uppercase
        if (isAllUpperCase(word)) {
          // Keep acronyms like "NCI" as uppercase
          camelCase.append(word);
        } else {
          // Make first character lowercase, preserve the rest
          camelCase.append(Character.toLowerCase(word.charAt(0)));
          if (word.length() > 1) {
            camelCase.append(word.substring(1));
          }
        }
      } else {
        // Subsequent words: uppercase the first character unless the whole word is uppercase
        if (isAllUpperCase(word)) {
          // Keep acronyms like "NCI" as uppercase
          camelCase.append(word);
        } else {
          // Make first character uppercase, preserve the rest
          camelCase.append(Character.toUpperCase(word.charAt(0)));
          if (word.length() > 1) {
            camelCase.append(word.substring(1));
          }
        }
      }
    }

    return camelCase.toString();
  }

  /**
   * Check if a word is all uppercase (indicating it's likely an acronym).
   *
   * @param word the word to check
   * @return true if the word is all uppercase letters
   */
  private boolean isAllUpperCase(String word) {
    if (word == null || word.isEmpty()) {
      return false;
    }

    for (char c : word.toCharArray()) {
      if (Character.isLetter(c) && !Character.isUpperCase(c)) {
        return false;
      }
    }

    // Make sure there's at least one letter
    return word.chars().anyMatch(Character::isLetter);
  }

  /**
   * Generate dynamic test cases for each Postman collection endpoint.
   *
   * @return Stream of DynamicTest instances
   */
  @TestFactory
  public Stream<DynamicTest> generatePostmanEndpointTests() {
    return postmanNameToRawMap.entrySet().stream()
        .map(
            entry ->
                DynamicTest.dynamicTest(
                    entry.getKey(), // Test name (camelCase name)
                    () -> executeEndpointTest(entry.getKey(), entry.getValue()) // Test execution
                    ));
  }

  /**
   * Execute a test for a specific endpoint.
   *
   * @param testName the name of the test (for logging/debugging)
   * @param rawEndpoint the raw endpoint URL from Postman collection
   * @throws Exception if the test fails
   */
  private void executeEndpointTest(String testName, String rawEndpoint) throws Exception {
    // Replace {{baseUrl}} with actual base URL
    String baseUrl = "http://localhost:" + port;
    String endpoint = rawEndpoint.replace("{{baseUrl}}", baseUrl);

    System.out.println("Executing test: " + testName);
    System.out.println("Endpoint: " + endpoint);

    try {
      // Act
      String content = this.restTemplate.getForObject(endpoint, String.class);

      // Parse the response as a generic FHIR Resource
      if (content != null && !content.isEmpty()) {

        try {
          // Parse as generic Resource - this will work for any valid FHIR resource
          Resource resource = (Resource) parser.parseResource(content);
          System.out.println("Successfully parsed as FHIR Resource for test: " + testName);
          System.out.println("Resource type: " + resource.getResourceType());

          // Basic assertion that applies to all FHIR resources
          assertNotNull(resource, "FHIR Resource should not be null for " + testName);

          // Generic validations for all FHIR resources
          assertNotNull(
              resource.getResourceType(), "Resource type should not be null for " + testName);
          assertNotNull(resource.getMeta(), "Resource meta should not be null for " + testName);
          if (resource.getMeta().hasVersionId()) {
            assertNotNull(
                resource.getMeta().getVersionId(),
                "Version ID should not be null if present for " + testName);
          }
          if (resource.getMeta().hasLastUpdated()) {
            assertNotNull(
                resource.getMeta().getLastUpdated(),
                "Last updated should not be null if present for " + testName);
          }

          // Handle specific resource types with type-specific validations
          switch (resource.getResourceType()) {
            case Parameters:
              Parameters params = (Parameters) resource;
              System.out.println("Processing as Parameters for test: " + testName);

              // Only check the result parameter if it exists
              if (params.getParameter("result") != null) {
                assertTrue(
                    ((BooleanType) params.getParameter("result").getValue()).getValue(),
                    "Result parameter should be true for " + testName);
                System.out.println("Result parameter validated as true for test: " + testName);
              } else {
                System.out.println(
                    "No 'result' parameter found - skipping result validation for test: "
                        + testName);
              }
              break;

            case CodeSystem:
              CodeSystem codeSystem = (CodeSystem) resource;
              System.out.println("Processing as CodeSystem for test: " + testName);
              assertNotNull(codeSystem);
              assertNotNull(codeSystem.getId());
              assertNotNull(codeSystem.getIdPart());
              assertNotNull(codeSystem.getPublisher());
              assertNotNull(codeSystem.getUrl());
              assertNotNull(codeSystem.getName());
              assertNotNull(codeSystem.getVersion());
              assertNotNull(codeSystem.getTitle());

              // Additional CodeSystem validations
              assertNotNull(
                  codeSystem.getStatus(), "CodeSystem status should not be null for " + testName);
              if (codeSystem.hasCount()) {
                assertTrue(
                    codeSystem.getCount() >= 0,
                    "CodeSystem count should be non-negative for " + testName);
              }
              if (codeSystem.hasConcept()) {
                assertFalse(
                    codeSystem.getConcept().isEmpty(),
                    "CodeSystem concepts should not be empty if present for " + testName);
                // Validate first concept has required fields
                var firstConcept = codeSystem.getConcept().get(0);
                assertNotNull(
                    firstConcept.getCode(), "Concept code should not be null for " + testName);
                assertNotNull(
                    firstConcept.getDisplay(),
                    "Concept display should not be null for " + testName);
              }
              if (codeSystem.hasDate()) {
                assertNotNull(
                    codeSystem.getDate(), "Date should not be null if present for " + testName);
              }
              if (codeSystem.hasDescription()) {
                assertFalse(
                    codeSystem.getDescription().isEmpty(),
                    "Description should not be empty if present for " + testName);
              }
              break;

            case Bundle:
              Bundle bundle = (Bundle) resource;
              System.out.println("Processing as Bundle for test: " + testName);
              // Act
              List<Resource> resourceList =
                  bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
              assertFalse(resourceList.isEmpty());

              // Additional Bundle validations
              assertNotNull(bundle.getType(), "Bundle type should not be null for " + testName);
              assertTrue(bundle.hasEntry(), "Bundle should have entries for " + testName);
              assertTrue(
                  bundle.getTotal() >= 0, "Bundle total should be non-negative for " + testName);
              if (bundle.hasLink()) {
                bundle
                    .getLink()
                    .forEach(
                        link -> {
                          assertNotNull(
                              link.getRelation(),
                              "Bundle link relation should not be null for " + testName);
                          assertNotNull(
                              link.getUrl(), "Bundle link URL should not be null for " + testName);
                        });
              }

              for (Resource rs : resourceList) {
                System.out.println("  resource = " + parser.encodeResourceToString(rs));
                ResourceType rType = rs.getResourceType();
                assertNotNull(rs);
                assertNotNull(rs.getId());
                assertNotNull(rs.getIdPart());

                // Generic validations for all resources in bundle
                assertNotNull(rs.getMeta(), "Resource meta should not be null for " + testName);
                if (rs.getMeta().hasLastUpdated()) {
                  assertNotNull(
                      rs.getMeta().getLastUpdated(),
                      "Last updated should not be null if present for " + testName);
                }

                if (rType == ResourceType.CodeSystem) {
                  CodeSystem css = (CodeSystem) rs;
                  assertNotNull(css.getPublisher());
                  assertNotNull(css.getUrl());
                  assertNotNull(css.getName());
                  assertNotNull(css.getVersion());
                  assertNotNull(css.getTitle());
                  assertNotNull(
                      css.getStatus(), "CodeSystem status should not be null for " + testName);
                } else if (rType == ResourceType.ConceptMap) {
                  ConceptMap cm = (ConceptMap) rs;
                  assertNotNull(cm.getPublisher());
                  assertNotNull(cm.getUrl());
                  assertNotNull(cm.getName());
                  assertNotNull(cm.getVersion());
                  assertNotNull(cm.getTitle());
                  assertNotNull(
                      cm.getStatus(), "ConceptMap status should not be null for " + testName);
                } else if (rType == ResourceType.ValueSet) {
                  ValueSet vs = (ValueSet) rs;
                  assertNotNull(vs.getPublisher());
                  assertNotNull(vs.getUrl());
                  assertNotNull(vs.getName());
                  assertNotNull(vs.getVersion());
                  assertNotNull(vs.getTitle());
                  assertNotNull(
                      vs.getStatus(), "ValueSet status should not be null for " + testName);
                }
              }
              break;

            case ValueSet:
              System.out.println("Processing as ValueSet for test: " + testName);
              ValueSet valueset = (ValueSet) resource;
              assertNotNull(valueset);
              assertNotNull(valueset.getId());
              assertNotNull(valueset.getIdPart());
              assertNotNull(valueset.getPublisher());
              assertNotNull(valueset.getUrl());
              assertNotNull(valueset.getName());
              assertNotNull(valueset.getVersion());
              assertNotNull(valueset.getTitle());

              // Additional ValueSet validations
              assertNotNull(
                  valueset.getStatus(), "ValueSet status should not be null for " + testName);
              if (valueset.hasCompose()) {
                assertNotNull(
                    valueset.getCompose(),
                    "ValueSet compose should not be null if present for " + testName);
                if (valueset.getCompose().hasInclude()) {
                  valueset
                      .getCompose()
                      .getInclude()
                      .forEach(
                          include -> {
                            if (include.hasSystem()) {
                              assertNotNull(
                                  include.getSystem(),
                                  "Include system should not be null if present for " + testName);
                            }
                          });
                }
              }
              if (valueset.hasExpansion()) {
                assertNotNull(
                    valueset.getExpansion(),
                    "ValueSet expansion should not be null if present for " + testName);
                assertTrue(
                    valueset.getExpansion().getTotal() >= 0,
                    "Expansion total should be non-negative for " + testName);
              }
              if (valueset.hasDate()) {
                assertNotNull(
                    valueset.getDate(), "Date should not be null if present for " + testName);
              }
              if (valueset.hasDescription()) {
                assertFalse(
                    valueset.getDescription().isEmpty(),
                    "Description should not be empty if present for " + testName);
              }
              break;

            case ConceptMap:
              System.out.println("Processing as ConceptMap for test: " + testName);
              ConceptMap conceptMap = (ConceptMap) resource;
              assertNotNull(conceptMap);
              assertNotNull(conceptMap.getId());
              assertNotNull(conceptMap.getIdPart());
              assertNotNull(conceptMap.getPublisher());
              assertNotNull(conceptMap.getUrl());
              assertNotNull(conceptMap.getName());
              assertNotNull(conceptMap.getVersion());
              assertNotNull(conceptMap.getTitle());

              // Additional ConceptMap validations
              assertNotNull(
                  conceptMap.getStatus(), "ConceptMap status should not be null for " + testName);
              if (conceptMap.hasSource()) {
                assertNotNull(
                    conceptMap.getSource(), "Source should not be null if present for " + testName);
              }
              if (conceptMap.hasTarget()) {
                assertNotNull(
                    conceptMap.getTarget(), "Target should not be null if present for " + testName);
              }
              if (conceptMap.hasGroup()) {
                conceptMap
                    .getGroup()
                    .forEach(
                        group -> {
                          if (group.hasSource()) {
                            assertNotNull(
                                group.getSource(),
                                "Group source should not be null if present for " + testName);
                          }
                          if (group.hasTarget()) {
                            assertNotNull(
                                group.getTarget(),
                                "Group target should not be null if present for " + testName);
                          }
                        });
              }
              if (conceptMap.hasDate()) {
                assertNotNull(
                    conceptMap.getDate(), "Date should not be null if present for " + testName);
              }
              if (conceptMap.hasDescription()) {
                assertFalse(
                    conceptMap.getDescription().isEmpty(),
                    "Description should not be empty if present for " + testName);
              }
              break;

            case OperationOutcome:
              OperationOutcome operationOutcome = (OperationOutcome) resource;
              System.out.println("Processing as OperationOutcome for test: " + testName);

              // Log the operation outcome details
              if (operationOutcome.hasIssue()) {
                System.out.println(
                    "OperationOutcome has " + operationOutcome.getIssue().size() + " issue(s):");
                for (int i = 0; i < operationOutcome.getIssue().size(); i++) {
                  var issue = operationOutcome.getIssue().get(i);
                  System.out.println(
                      "  Issue "
                          + (i + 1)
                          + ": "
                          + issue.getSeverity()
                          + " - "
                          + issue.getCode()
                          + " - "
                          + issue.getDiagnostics());
                }
              }

              // Fail the test when OperationOutcome is returned
              fail(
                  "Test failed for "
                      + testName
                      + ": Received OperationOutcome instead of expected resource. "
                      + "OperationOutcome indicates an error or operational issue occurred.");
              break;

            default:
              System.out.println(
                  "Processing as generic FHIR resource ("
                      + resource.getResourceType()
                      + ") for test: "
                      + testName);
              // Any other FHIR resource type - basic validation only
              break;
          }

          System.out.println("Test passed: " + testName);

        } catch (Exception e) {
          throw new Exception(
              "Failed to parse response as FHIR Resource for "
                  + testName
                  + ". Error: "
                  + e.getMessage(),
              e);
        }

      } else {
        throw new Exception("Empty response received for test: " + testName);
      }

    } catch (Exception e) {
      System.err.println("Test failed for " + testName + ": " + e.getMessage());
      throw new Exception("Test failed for endpoint " + testName + ": " + e.getMessage(), e);
    }
  }
}
