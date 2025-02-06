package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.MappingResultList;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** mapset tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MapsetControllerTests {

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc mvc;

  /** The base url. */
  private String baseUrl;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** Sets the up. */
  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/mapset";
  }

  /**
   * Test mapsets with no params passed
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsNoParams() throws Exception {
    // Arrange

    // Act
    MvcResult result = mvc.perform(get(baseUrl)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    List<Concept> metadataMappings =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    // Assert
    assertTrue(metadataMappings.size() > 0);
    assertTrue(
        metadataMappings.stream()
            .map(Concept::getName)
            .noneMatch(name -> name == null || name.isEmpty()));
    assertTrue(
        metadataMappings.stream()
            .map(Concept::getCode)
            .noneMatch(code -> code == null || code.isEmpty()));
    assertTrue(
        metadataMappings.stream()
            .map(Concept::getProperties)
            .allMatch(properties -> properties == null || properties.isEmpty()));
  }

  /**
   * Test mapsets with include params
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithIncludeParams() throws Exception {
    // Arrange

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl).param("include", "properties"))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    List<Concept> metadataMappings =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    // Assert
    assertFalse(metadataMappings.isEmpty());
    assertTrue(
        metadataMappings.stream()
            .map(Concept::getName)
            .noneMatch(name -> name == null || name.isEmpty()));
    assertTrue(
        metadataMappings.stream()
            .map(Concept::getCode)
            .noneMatch(code -> code == null || code.isEmpty()));
    assertTrue(
        metadataMappings.stream()
            .map(Concept::getProperties)
            .noneMatch(properties -> properties == null || properties.isEmpty()));
  }

  /**
   * Test mapsets with code params, download = false, include param properties
   *
   * @throws Exception
   */
  @Test
  public void testMapsetsWithCodeAndIncludeParams() throws Exception {
    // Arrange
    String params = "properties";
    String terminology = "GO_to_NCIt_Mapping";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + terminology).param("include", params))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    Concept singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);

    // Assert
    assertNotNull(singleMetadataMap);
    assertNotNull(singleMetadataMap.getName());
    assertNotNull(singleMetadataMap.getCode());
    assertEquals(terminology, singleMetadataMap.getName());
    assertEquals(terminology, singleMetadataMap.getCode());
    assertNotNull(singleMetadataMap.getProperties());
    assertTrue(
        singleMetadataMap.getProperties().stream()
            .anyMatch(
                property ->
                    property.getType().equals("downloadOnly")
                        && property.getValue().equals("false")));
    assertTrue(
        singleMetadataMap.getProperties().stream()
            .anyMatch(
                property ->
                    property.getType().equals("welcomeText") && property.getValue() != null));
  }

  /**
   * Test mapsets with code params, download = true, include param maps + properties
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithCodeDownloadTrueIncludeParams() throws Exception {
    // Arrange
    String params = "maps,properties";
    String terminology = "ICDO_TO_NCI_TOPOGRAPHY";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + terminology).param("include", params))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    Concept singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);

    // Assert
    assertNotNull(singleMetadataMap);
    assertNotNull(singleMetadataMap.getName());
    assertNotNull(singleMetadataMap.getCode());
    assertNotNull(singleMetadataMap.getProperties());
    assertEquals(terminology, singleMetadataMap.getName());
    assertEquals(terminology, singleMetadataMap.getCode());
    // should be no maps indexed because of ftp storage
    assertTrue(singleMetadataMap.getMaps().isEmpty());
    assertTrue(
        singleMetadataMap.getProperties().stream()
            .anyMatch(
                property ->
                    property.getType().equals("downloadOnly")
                        && "true".equals(property.getValue())));
    assertTrue(
        singleMetadataMap.getProperties().stream()
            .anyMatch(
                property ->
                    property.getType().equals("mapsetLink")
                        && property.getValue() != null
                        && property.getValue().endsWith(".txt")));
  }

  /**
   * Test mapsets with code params, download = true, mapsetLink = null, and include param maps +
   * properties
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithCodeDownloadTrueMapsetLinkNull() throws Exception {
    // Arrange
    String params = "maps,properties";
    String terminology = "NCIt_Maps_To_GDC";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + terminology).param("include", params))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    Concept singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);

    // Assert
    assertNotNull(singleMetadataMap);
    assertNotNull(singleMetadataMap.getName());
    assertNotNull(singleMetadataMap.getCode());
    assertNotNull(singleMetadataMap.getProperties());
    assertNotNull(singleMetadataMap.getMaps());
    assertEquals(terminology, singleMetadataMap.getName());
    assertEquals(terminology, singleMetadataMap.getCode());
    assertFalse(singleMetadataMap.getVersion().contains("ncit_"));
    assertTrue(
        singleMetadataMap.getProperties().stream()
            .anyMatch(
                property ->
                    property.getType().equals("downloadOnly")
                        && "true".equals(property.getValue())));
    assertTrue(
        singleMetadataMap.getProperties().stream()
            .anyMatch(
                property ->
                    property.getType().equals("mapsetLink") && property.getValue() == null));
  }

  /**
   * Test mapsets with invalid code params
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithInvalidCode() throws Exception {
    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/noMap")).andExpect(status().isNotFound()).andReturn();

    String content = result.getResponse().getContentAsString();

    // Assert
    assertEquals(0, content.length());
  }

  /**
   * Test mapsets return maps with valid code params and default param values
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithMapsCodeUsingDefaults() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertTrue(mapList.getTotal() > 0);
    assertEquals(10, mapList.getMaps().size());
  }

  /**
   * Test mapsets return maps with valid code params and fromRecord = 9 and pageSize is 23
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithMapsCodeFromRecordAndPageSize() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";
    String params = "?fromRecord=9&pageSize=23";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    Mapping tenFromZero = mapList.getMaps().get(9);

    // testing fromRecord and pageSize
    result = mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    Mapping tenFromTen = mapList.getMaps().get(0);

    // Assert
    assertTrue(mapList.getTotal() > 0);
    assertEquals(23, mapList.getMaps().size());
    assertEquals(tenFromTen, tenFromZero);
  }

  /**
   * Test mapsets return maps with valid code params and fromRecord 1 and pageSize of 10
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithMapsCodeFromRecordOffPageSize() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";
    String params = "?fromRecord=1&pageSize=10";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    Mapping tenFromZero = mapList.getMaps().get(9);

    // testing fromRecord off page size
    result = mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    Mapping tenFromOne = mapList.getMaps().get(8);

    // Assert
    assertTrue(mapList.getTotal() > 0);
    assertEquals(tenFromOne, tenFromZero);
  }

  /**
   * Test mapsets return maps with valid code params and fromRecord 1000
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithMapsCodeFromRecordPastTheEnd() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";
    String params = "?fromRecord=1000";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertNull(mapList.getMaps());
  }

  /**
   * Test mapsets returns zero matches with an invalid term passed
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithMapsCodeWithZeroMatches() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";
    String params = "?term=XXXXXX";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertNull(mapList.getMaps());
  }

  /**
   * Test mapsets returns non-zero matches with a valid term passed
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithMapsCodeWithNonZeroMatches() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";
    String params = "?term=act";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
    assertTrue(
        mapList.getMaps().stream()
            .flatMap(map -> Stream.of(map.getSourceName(), map.getTargetName()))
            .anyMatch(name -> name.contains("act")));
  }

  /**
   * Test mapsets returns non-zero matches with a valid term passed with leading and trailing spaces
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithMapsCodeWithNonZeroMatchesTrimSpacing() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";
    String params = "?term=   act   ";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
    assertTrue(
        mapList.getMaps().stream()
            .flatMap(map -> Stream.of(map.getSourceName(), map.getTargetName()))
            .anyMatch(name -> name.contains("act")));
  }

  /**
   * Test mapsets returns non-zero matches with a valid term passed with fromRecord and pageSize
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithMapsCodeWithNonZeroMatchesAndFromRecordAndPageSize() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";
    String params = "?term=act";
    String params2 = "?term=act&fromRecord=3&pageSize=12";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    Mapping sixFromZero = mapList.getMaps().get(5);

    result =
        mvc.perform(get(baseUrl + "/" + path + params2)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    Mapping sixFromThree = mapList.getMaps().get(2);

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
    assertEquals(12, mapList.getMaps().size());
    assertTrue(
        mapList.getMaps().stream()
            .flatMap(map -> Stream.of(map.getSourceName(), map.getTargetName()))
            .anyMatch(name -> name.contains("act")));
    assertEquals(sixFromZero, sixFromThree);
  }

  @Test
  public void testMapsetsWithMapsCodeSuccess() throws Exception {
    // Arrange
    String path = "GO_to_NCIt_Mapping/maps";
    String params = "?term=C17087";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
    assertTrue(
        mapList.getMaps().stream()
            .flatMap(map -> Stream.of(map.getSourceCode(), map.getTargetCode()))
            .anyMatch(name -> name.contains("C17087")));
  }

  /**
   * Test mapsets with SNOMED mapping exists
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithSNOMEDMappingExists() throws Exception {
    // Arrange
    String path = "SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
  }

  /**
   * Test mapsets with SNOMED mapping and term exists
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithSNOMEDMappingANDTermExists() throws Exception {
    // Arrange
    String path = "SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps";
    String params = "?term=AIDS";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
    assertEquals("AIDS", mapList.getMaps().get(0).getSourceName());
  }

  /**
   * Test mapsets with SNOMED mapping and code exists
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithSNOMEDMappingsANDCodeExists() throws Exception {
    // Arrange
    String path = "SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps";
    String params = "?term=62479008";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
    assertEquals("62479008", mapList.getMaps().get(0).getSourceCode());
  }

  /**
   * Test mapsets with SNOMED mapping and ascending sort is set to true.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithSNOMEDANDSortAscendingTrue() throws Exception {
    // Arrange
    String path = "SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps";
    String params = "?sort=sourceName&ascending=true";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    List<String> sortedNames =
        mapList.getMaps().stream()
            .map(Mapping::getSourceName)
            .sorted()
            .collect(Collectors.toList());
    List<String> unsortedNames =
        mapList.getMaps().stream().map(Mapping::getSourceName).collect(Collectors.toList());

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
    assertEquals(sortedNames, unsortedNames);
  }

  /**
   * Test mapsets with SNOMED mapping and ascending sort is set to false.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithSNOMEDANDSortDescendingTrue() throws Exception {
    // Arrange
    String path = "SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps";
    String params = "?sort=sourceName&ascending=false";

    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    List<String> sortedNames =
        mapList.getMaps().stream()
            .map(Mapping::getSourceName)
            .sorted((a, b) -> b.compareTo(a))
            .collect(Collectors.toList());
    List<String> unsortedNames =
        mapList.getMaps().stream().map(Mapping::getSourceName).collect(Collectors.toList());

    // Assert
    assertFalse(mapList.getMaps().isEmpty());
    assertEquals(sortedNames, unsortedNames);
  }

  @Test
  public void testNciMapsetDownload() throws Exception {

    // Verify we can get the mapset
    String path = "NCIt_Maps_To_GDC";
    String params = "?include=properties";
    MvcResult result =
        mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    assertNotNull(content);

    // Verify we can get the mappings and there are 10
    path = "NCIt_Maps_To_GDC/maps";
    params = "?pageSize=10&fromRecord=0";
    result = mvc.perform(get(baseUrl + "/" + path + params)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertNotNull(content);
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);
    assertEquals(10, mapList.getMaps().size());
  }

  /**
   * Test mapsets with retired concepts
   *
   * @throws Exception the exception
   */
  @ParameterizedTest
  @CsvFileSource(
      resources = "/mapsetControllerParamTestSamples/retired_concepts.csv",
      numLinesToSkip = 1)
  public void testMapsetsNoRetiredConcepts(String path, String params) throws Exception {
    // Act
    MvcResult result =
        mvc.perform(get(baseUrl + path + params)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    MappingResultList mapList = new ObjectMapper().readValue(content, MappingResultList.class);

    // Assert
    assertNull(mapList.getMaps());
  }
}
