package gov.nih.nci.evs.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMap;
import gov.nih.nci.evs.api.model.ConceptMapResultList;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  private String baseUrl = "";

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
   * Test mapsets
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsets() throws Exception {
    MvcResult result = null;
    String content = null;
    result = mvc.perform(get(baseUrl)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    List<Concept> metadataMappings =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });

    // test /mapset
    assert (metadataMappings.size() > 0);
    assert (metadataMappings.stream()
        .map(Concept::getName)
        .noneMatch(name -> name == null || name.isEmpty()));
    assert (metadataMappings.stream()
        .map(Concept::getCode)
        .noneMatch(code -> code == null || code.isEmpty()));
    // properties should be empty since not included
    assert (metadataMappings.stream()
        .map(Concept::getProperties)
        .allMatch(properties -> properties == null || properties.isEmpty()));

    // /mapset with include params
    result =
        mvc.perform(get(baseUrl).param("include", "properties"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    metadataMappings =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assert (metadataMappings.size() > 0);
    assert (metadataMappings.stream()
        .map(Concept::getName)
        .noneMatch(name -> name == null || name.isEmpty()));
    assert (metadataMappings.stream()
        .map(Concept::getCode)
        .noneMatch(code -> code == null || code.isEmpty()));
    assert (metadataMappings.stream()
        .map(Concept::getProperties)
        .noneMatch(properties -> properties == null || properties.isEmpty()));
  }

  /**
   * Test mapsets with code params
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithCode() throws Exception {
    MvcResult result = null;
    String content = null;
    result = mvc.perform(get(baseUrl)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // test mapset/{code} - downloadOnly = false, include param properties
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping").param("include", "properties"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    Concept singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);
    assert (singleMetadataMap != null);
    assert (singleMetadataMap.getName() != null
        && singleMetadataMap.getName().equals("GO_to_NCIt_Mapping"));
    assert (singleMetadataMap.getCode() != null
        && singleMetadataMap.getCode().equals("GO_to_NCIt_Mapping"));
    assert (singleMetadataMap.getProperties().stream()
        .filter(
            property ->
                property.getType().equals("downloadOnly") && property.getValue().equals("false"))
        .findFirst()
        .isPresent());
    assert (singleMetadataMap.getProperties().stream()
        .filter(property -> property.getType().equals("welcomeText") && property.getValue() != null)
        .findFirst()
        .isPresent());
    assert (singleMetadataMap.getProperties() != null);

    // test mapset/{code} - downloadOnly = true, mapsetLink available, include param maps +
    // properties
    result =
        mvc.perform(get(baseUrl + "/ICDO_TO_NCI_TOPOGRAPHY").param("include", "maps,properties"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);
    assert (singleMetadataMap != null);
    assert (singleMetadataMap.getName() != null
        && singleMetadataMap.getName().equals("ICDO_TO_NCI_TOPOGRAPHY"));
    assert (singleMetadataMap.getCode() != null
        && singleMetadataMap.getCode().equals("ICDO_TO_NCI_TOPOGRAPHY"));
    assert (singleMetadataMap.getProperties().stream()
        .filter(
            property ->
                property.getType().equals("downloadOnly") && "true".equals(property.getValue()))
        .findFirst()
        .isPresent());
    assert (singleMetadataMap.getProperties().stream()
        .filter(
            property ->
                property.getType().equals("mapsetLink")
                    && property.getValue() != null
                    && property.getValue().endsWith(".txt"))
        .findFirst()
        .isPresent());
    // should be no maps indexed because of ftp storage
    assert (singleMetadataMap.getMaps().isEmpty());

    // test mapset/{code} - downloadOnly = true, mapsetLink = null, include param maps + properties
    result =
        mvc.perform(get(baseUrl + "/NCIt_Maps_To_GDC").param("include", "maps,properties"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);
    assert (singleMetadataMap != null);
    assert (singleMetadataMap.getName() != null
        && singleMetadataMap.getName().equals("NCIt_Maps_To_GDC"));
    assert (singleMetadataMap.getCode() != null
        && singleMetadataMap.getCode().equals("NCIt_Maps_To_GDC"));
    assert (!singleMetadataMap.getVersion().contains("ncit_"));
    assert (singleMetadataMap.getProperties().stream()
        .filter(
            property ->
                property.getType().equals("downloadOnly") && "true".equals(property.getValue()))
        .findFirst()
        .isPresent());
    assert (singleMetadataMap.getProperties().stream()
        .filter(property -> property.getType().equals("mapsetLink"))
        .findFirst()
        .isPresent());
    assert (singleMetadataMap.getMaps() != null);

    // test mapset/{code} - invalid code param
    result = mvc.perform(get(baseUrl + "/noMap")).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    assert (content.length() == 0);
  }

  /**
   * Test mapset maps with code params
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsWithCodeForMaps() throws Exception {
    MvcResult result = null;
    String content = null;
    result = mvc.perform(get(baseUrl)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();

    // test mapset/{code}/maps
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    ConceptMapResultList mapList =
        new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getTotal() > 0);
    assert (mapList.getMaps().size() == 10);
    ConceptMap tenFromZero = mapList.getMaps().get(9);

    // testing fromRecord and pageSize
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps?fromRecord=9&pageSize=23"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getTotal() > 0);
    assert (mapList.getMaps().size() == 23);
    ConceptMap tenFromTen = mapList.getMaps().get(0);
    assert (tenFromTen.equals(tenFromZero));

    // test mapset/{code}/maps, fromRecord off page size
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps?fromRecord=1&pageSize=10"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getTotal() > 0);
    ConceptMap tenFromOne = mapList.getMaps().get(8);
    assert (tenFromTen.equals(tenFromOne));

    // test mapset/{code}/maps, fromRecord past the end
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps?fromRecord=1000"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps() == null);

    // test mapset/{code}/maps, term with zero matches
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps?term=XXXXXXX"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps() == null);

    // test mapset/{code}/maps, term with non-zero matches
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps?term=act"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() != 0);
    assert (mapList.getMaps().stream()
            .flatMap(map -> Stream.of(map.getSourceName(), map.getTargetName())))
        .anyMatch(name -> name.contains("act"));
    ConceptMap sixFromZero = mapList.getMaps().get(5);

    // test mapset/{code}/maps, term with non-zero matches, trim spacing
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps?term=   act   "))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() != 0);
    assert (mapList.getMaps().stream()
            .flatMap(map -> Stream.of(map.getSourceName(), map.getTargetName())))
        .anyMatch(name -> name.contains("act"));

    // test mapset/{code}/maps, term with non-zero matches and different fromRecord/pageSize
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps?term=act&fromRecord=3&pageSize=12"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() == 12);
    assert (mapList.getMaps().stream()
            .flatMap(map -> Stream.of(map.getSourceName(), map.getTargetName())))
        .anyMatch(name -> name.contains("act"));
    ConceptMap sixFromThree = mapList.getMaps().get(2);
    assert (sixFromThree.equals(sixFromZero));

    // test mapset/{code}/maps, code with non-zero matches
    result =
        mvc.perform(get(baseUrl + "/GO_to_NCIt_Mapping/maps?term=C17087"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() != 0);
    assert (mapList.getMaps().stream()
            .flatMap(map -> Stream.of(map.getSourceCode(), map.getTargetCode())))
        .anyMatch(name -> name.contains("C17087"));

    // test SNOMED mapping in mapset/{code}/maps, check for existence
    result =
        mvc.perform(get(baseUrl + "/SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() != 0);

    // test SNOMED mapping in mapset/{code}/maps, term search
    result =
        mvc.perform(
                get(baseUrl + "/SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps?term=AIDS"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() != 0);
    assert (mapList.getMaps().get(0).getSourceName().equals("AIDS"));

    // test SNOMED mapping in mapset/{code}/maps, code search
    result =
        mvc.perform(
                get(
                    baseUrl
                        + "/SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps?term=62479008"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() != 0);
    assert (mapList.getMaps().get(0).getSourceCode().equals("62479008"));

    // test SNOMED mapping in mapset/{code}/maps, sort by name
    result =
        mvc.perform(
                get(
                    baseUrl
                        + "/SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps?sort=sourceName&ascending=true"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() > 1);
    assert (mapList
            .getMaps()
            .get(0)
            .getSourceName()
            .compareTo(mapList.getMaps().get(1).getSourceName())
        < 0);

    // test SNOMED mapping in mapset/{code}/maps, sort by code desc
    result =
        mvc.perform(
                get(
                    baseUrl
                        + "/SNOMEDCT_US_2020_09_01_to_ICD10CM_2021_Mappings/maps?sort=sourceCode&ascending=false"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps().size() > 1);
    assert (mapList
            .getMaps()
            .get(0)
            .getSourceCode()
            .compareTo(mapList.getMaps().get(1).getSourceCode())
        > 0);
  }

  /**
   * Test mapsets with retired concepts
   *
   * @throws Exception the exception
   */
  @Test
  public void testMapsetsNoRetiredConcepts() throws Exception {
    MvcResult result = null;
    String content = null;
    result = mvc.perform(get(baseUrl)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();

    result =
        mvc.perform(get(baseUrl + "/NCIt_Maps_To_ICDO3/maps?term=C4303"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    ConceptMapResultList mapList =
        new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps() == null
        || mapList.getMaps().stream()
            .noneMatch(
                map ->
                    "C34801".equals(map.getSourceCode()) || "C34801".equals(map.getTargetCode())));

    result =
        mvc.perform(get(baseUrl + "/NCIt_Maps_To_GDC/maps?term=C50583"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps() == null
        || mapList.getMaps().stream()
            .noneMatch(
                map -> "C2915".equals(map.getSourceCode()) || "C2915".equals(map.getTargetCode())));

    result =
        mvc.perform(get(baseUrl + "/NCIt_Maps_To_MedDRA/maps?term=C34801"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, ConceptMapResultList.class);
    assert (mapList.getMaps() == null
        || mapList.getMaps().stream()
            .noneMatch(
                map ->
                    "C34801".equals(map.getSourceCode()) || "C34801".equals(map.getTargetCode())));
  }
}
