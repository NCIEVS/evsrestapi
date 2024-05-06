package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.AssociationEntryResultList;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Test harness for content samples. */
@AutoConfigureMockMvc
public class ConceptSampleTester {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ConceptSampleTester.class);

  /** The base url. */
  private String baseUrl = "/api/v1/concept/";

  /** The base metadata url. */
  private String baseMetadataUrl = "/api/v1/metadata/";

  /** The term url. */
  // private String termUrl = "/api/v1/metadata/terminologies";

  /** The license key. */
  private String licenseKey = "notblank";

  /** The test mvc. */
  private MockMvc testMvc;

  /** The terminology. */
  private Terminology terminology;

  private TerminologyUtils termUtils = null;

  /** The elasticquery service. */
  private ElasticQueryService esQueryService = null;

  /** The errors. */
  private List<String> errors = new ArrayList<String>();

  /**
   * Instantiates an empty {@link ConceptSampleTester}.
   *
   * @param termUtils the terminology utils
   */
  public ConceptSampleTester(TerminologyUtils termUtils, ElasticQueryService esQueryService) {
    this.termUtils = termUtils;
    this.esQueryService = esQueryService;
  }

  /**
   * Sets the license key.
   *
   * @param licenseKey the license key
   */
  public void setLicenseKey(final String licenseKey) {
    this.licenseKey = licenseKey;
  }

  /**
   * Sets the terminology.
   *
   * @param term the term
   * @throws Exception the exception
   */
  private void lookupTerminology(final String term) throws Exception {

    terminology = termUtils.getIndexedTerminology(term, esQueryService);
  }

  /**
   * Perform metadata tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   * @throws Exception the exception
   */
  public void performMetadataTests(
      final String term, final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc)
      throws Exception {
    String url = baseMetadataUrl;
    MvcResult result = null;
    testMvc = mvc;
    String content = null;
    lookupTerminology(term);

    // get associations
    url = baseMetadataUrl + term + "/associations?include=minimal";
    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<String> associations =
        new ObjectMapper()
                .readValue(
                    content,
                    new TypeReference<List<Concept>>() {
                      // n/a
                    })
                .stream()
                .map(entry -> entry.getCode())
                .collect(Collectors.toList());

    // get qualifiers
    url = baseMetadataUrl + term + "/qualifiers?include=minimal";
    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<Concept> qualifiers =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    List<Concept> remodeledQualifiers =
        qualifiers.stream()
            .filter(x -> x.getProperties().stream().anyMatch(y -> y.getType().equals("remodeled")))
            .collect(Collectors.toList());
    qualifiers =
        qualifiers.stream()
            .filter(x -> x.getProperties().stream().noneMatch(y -> y.getType().equals("remodeled")))
            .collect(Collectors.toList());

    List<String> qualifiersString =
        qualifiers.stream().map(entry -> entry.getCode()).collect(Collectors.toList());

    List<String> remodeledQualifierString =
        remodeledQualifiers.stream()
            .filter(x -> x.getProperties().stream().anyMatch(y -> y.getType().equals("remodeled")))
            .collect(Collectors.toList())
            .stream()
            .map(entry -> entry.getCode())
            .collect(Collectors.toList());

    for (String prop : remodeledQualifierString) {
      if (!terminology.getMetadata().isRemodeledQualifier(prop)) {
        errors.add("Qualifier " + prop + " listed as remodeled, but isn't");
      }
    }

    // get roles
    url = baseMetadataUrl + term + "/roles?include=minimal";
    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<String> roles =
        new ObjectMapper()
                .readValue(
                    content,
                    new TypeReference<List<Concept>>() {
                      // n/a
                    })
                .stream()
                .map(entry -> entry.getCode())
                .collect(Collectors.toList());

    // get synonym term types
    url = baseMetadataUrl + term + "/termTypes?include=minimal";
    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<String> termTypes =
        new ObjectMapper()
                .readValue(
                    content,
                    new TypeReference<List<Concept>>() {
                      // n/a
                    })
                .stream()
                .map(entry -> entry.getCode())
                .collect(Collectors.toList());

    // get synonym sources
    url = baseMetadataUrl + term + "/synonymSources?include=minimal";
    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<String> synonymSources =
        new ObjectMapper()
                .readValue(
                    content,
                    new TypeReference<List<Concept>>() {
                      // n/a
                    })
                .stream()
                .map(entry -> entry.getCode())
                .collect(Collectors.toList());

    // get definition types
    url = baseMetadataUrl + term + "/definitionTypes?include=minimal";
    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<String> definitionTypes =
        new ObjectMapper()
                .readValue(
                    content,
                    new TypeReference<List<Concept>>() {
                      // n/a
                    })
                .stream()
                .map(entry -> entry.getCode())
                .collect(Collectors.toList());

    // get definition sources
    url = baseMetadataUrl + term + "/definitionSources?include=minimal";
    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<String> definitionSources =
        new ObjectMapper()
                .readValue(
                    content,
                    new TypeReference<List<Concept>>() {
                      // n/a
                    })
                .stream()
                .map(entry -> entry.getCode())
                .collect(Collectors.toList());

    // get properties
    url = baseMetadataUrl + term + "/properties?include=properties";
    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<Concept> properties =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    List<Concept> remodeledProperties =
        properties.stream()
            .filter(x -> x.getProperties().stream().anyMatch(y -> y.getType().equals("remodeled")))
            .collect(Collectors.toList());

    properties =
        properties.stream()
            .filter(x -> x.getProperties().stream().noneMatch(y -> y.getType().equals("remodeled")))
            .collect(Collectors.toList());

    List<String> propertiesString =
        properties.stream().map(entry -> entry.getCode()).collect(Collectors.toList());

    List<String> remodeledPropertyString =
        remodeledProperties.stream()
            .filter(x -> x.getProperties().stream().anyMatch(y -> y.getType().equals("remodeled")))
            .collect(Collectors.toList())
            .stream()
            .map(entry -> entry.getCode())
            .collect(Collectors.toList());

    for (String prop : remodeledPropertyString) {
      if (!terminology.getMetadata().isRemodeledProperty(prop)) {
        errors.add("Property " + prop + " listed as remodeled, but isn't");
      }
    }

    for (final Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
      url = baseUrl + term + "/" + entry.getKey() + "?include=full";
      log.info("Testing url - " + url);
      result =
          mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      log.info(" content = " + content);
      assertThat(content).isNotNull();

      for (final SampleRecord sample : entry.getValue()) {

        String sampleKey = sample.getKey();
        String synDefProperty = null;

        if (sample.getKey().contains("~")) {
          sampleKey = sample.getKey().split("~")[1];
          if (sample.getKey().split("~").length > 2) {
            synDefProperty = sample.getKey().split("~")[2];
          }
        }
        if (sampleKey.startsWith(term + ":")) {
          // temp fix for classpaths #1
          sampleKey = sampleKey.replace(term + ":", "");
        }

        if (associations.contains(sampleKey)) {
          url = baseMetadataUrl + term + "/association/" + sampleKey;
          if (mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                  .andExpect(status().isOk())
              != null) {
            associations.remove(sampleKey);
          } else {
            errors.add(
                "Association error: " + sampleKey + " does not exist in " + term + " associations");
          }
        } else if (qualifiersString.contains(sampleKey)) {
          url = baseMetadataUrl + term + "/qualifier/" + sampleKey;
          if (mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                  .andExpect(status().isOk())
              != null) {
            qualifiersString.remove(sampleKey);
          } else {
            errors.add(
                "Qualifier error: " + sampleKey + " does not exist in " + term + " qualifiers");
          }
        } else if (roles.contains(sampleKey)) {
          url = baseMetadataUrl + term + "/role/" + sampleKey;
          if (mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                  .andExpect(status().isOk())
              != null) {
            roles.remove(sampleKey);
          } else {
            errors.add("Role error: " + sampleKey + " does not exist in " + term + " roles");
          }
        } else if (synDefProperty != null && termTypes.contains(synDefProperty)) {
          termTypes.remove(synDefProperty);
        } else if (synDefProperty != null && synonymSources.contains(synDefProperty)) {
          synonymSources.remove(synDefProperty);
        } else if (synDefProperty != null && definitionSources.contains(synDefProperty)) {
          definitionSources.remove(synDefProperty);
        } else if (definitionTypes.contains(sampleKey)) {
          url = baseMetadataUrl + term + "/definitionType/" + sampleKey;
          if (mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                  .andExpect(status().isOk())
              != null) {
            definitionTypes.remove(sampleKey);
          } else {
            errors.add(
                "Definition Type error: "
                    + sampleKey
                    + " does not exist in "
                    + term
                    + " definition types");
          }
        } else if (propertiesString.contains(sampleKey)) {
          url = baseMetadataUrl + term + "/property/" + sampleKey;
          if (mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                  .andExpect(status().isOk())
              != null) {
            propertiesString.remove(sampleKey);
          } else {
            errors.add(
                "Property error: " + sampleKey + " does not exist in " + term + " properties");
          }
        }
        // some terms actually do need the term name
        else if (propertiesString.contains(term + ":" + sampleKey)) {
          String termNameAndTerm = term + ":" + sampleKey;
          url = baseMetadataUrl + term + "/property/" + termNameAndTerm;
          if (mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                  .andExpect(status().isOk())
              != null) {
            propertiesString.remove(termNameAndTerm);
          } else {
            errors.add(
                "Property error: "
                    + termNameAndTerm
                    + " does not exist in "
                    + term
                    + " properties");
          }
        }
      }
    }
    if (errors.size() > 0) {
      log.error(
          "METADATA ERRORS FOUND IN SAMPLING FOR TERMINOLOGY "
              + terminology.getName()
              + ". SEE LOG BELOW");
      for (final String err : errors) {
        log.error(err);
      }
    } else {
      log.info("No metadata errors found for terminology " + terminology.getName());
    }
    if (associations.size() > 0)
      log.info("Associations not covered in sampling: " + Arrays.toString(associations.toArray()));
    if (qualifiersString.size() > 0)
      log.info(
          "Qualifiers not covered in sampling: " + Arrays.toString(qualifiersString.toArray()));
    if (roles.size() > 0)
      log.info("Roles not covered in sampling: " + Arrays.toString(roles.toArray()));
    if (termTypes.size() > 0)
      log.info(
          "Synonym term types not covered in sampling: " + Arrays.toString(termTypes.toArray()));
    if (synonymSources.size() > 0)
      log.info(
          "Synonym sources not covered in sampling: " + Arrays.toString(synonymSources.toArray()));
    if (definitionSources.size() > 0)
      log.info(
          "Definition sources not covered in sampling: "
              + Arrays.toString(definitionSources.toArray()));
    if (definitionTypes.size() > 0)
      log.info(
          "Definition types not covered in sampling: "
              + Arrays.toString(definitionTypes.toArray()));
    if (propertiesString.size() > 0)
      log.info(
          "Properties not covered in sampling: " + Arrays.toString(propertiesString.toArray()));
  }

  /**
   * Perform content tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   * @throws Exception the exception
   */
  public void performContentTests(
      final String term, final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc)
      throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    testMvc = mvc;
    String content = null;
    Concept concept = null;
    lookupTerminology(term);

    for (final Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
      url = baseUrl + term + "/" + entry.getKey() + "?include=full";
      log.info("Testing url - " + url);
      result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      log.info(" content = " + content);
      concept = new ObjectMapper().readValue(content, Concept.class);
      assertThat(content).isNotNull();
      log.info(content);

      url = baseMetadataUrl + term + "/properties?include=properties";
      result =
          mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      List<Concept> properties =
          new ObjectMapper()
              .readValue(
                  content,
                  new TypeReference<List<Concept>>() {
                    // n/a
                  });

      Map<String, String> propertyList =
          properties.stream().collect(Collectors.toMap(Concept::getCode, Concept::getName));

      url = baseMetadataUrl + term + "/associations?include=associations";
      result =
          mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      List<Concept> associations =
          new ObjectMapper()
              .readValue(
                  content,
                  new TypeReference<List<Concept>>() {
                    // n/a
                  });

      Map<String, String> associationsList =
          associations.stream().collect(Collectors.toMap(Concept::getCode, Concept::getName));

      for (final SampleRecord sample : entry.getValue()) {
        final String key = sample.getKey();
        if (!(sample.getValue() == null) && !sample.getValue().isEmpty()) {
          sample.setValue(standardizeText(sample.getValue())); // standardizing text
        }

        if (key.startsWith("rdfs:subClassOf") && !key.contains("~")) {
          if (!checkParent(concept, sample)) {
            errors.add("ERROR: Wrong parent " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (key.equals("rdfs:label")) {
          if (!checkLabel(concept, sample)) {
            errors.add(
                "Incorrectly label of " + sample.getValue() + " for concept " + concept.getCode());
          }
        } else if (key.equals("rdfs:comment")) {
          if (!checkComment(concept, sample)) {
            errors.add(
                "Incorrectly labelled comment of "
                    + sample.getValue()
                    + " for concept "
                    + concept.getCode());
          }
        } else if (key.equals("owl:deprecated")) {
          if (!checkDeprecated(concept)) {
            errors.add(
                "ERROR: Incorrectly labelled code "
                    + sample.getValue()
                    + " of "
                    + terminology.getName()
                    + " as retired/deprecated");
          }
        } else if (key.equals(terminology.getMetadata().getCode())) {
          if (!checkCode(concept, sample)) {
            errors.add(
                "ERROR: Wrong terminology code "
                    + sample.getValue()
                    + " of "
                    + terminology.getName());
          }
        } else if (key.equals(terminology.getMetadata().getPreferredName())) {
          if (!checkPreferredName(concept, sample)) {
            errors.add(
                "ERROR: Wrong terminology preferred name code "
                    + sample.getKey()
                    + " of "
                    + terminology.getName());
          }
        } else if (terminology.getMetadata().getSynonym().contains(key)) {
          if (!checkSynonym(concept, sample)) {
            errors.add("ERROR: Wrong synonym " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (terminology.getMetadata().getDefinition().contains(key)
            || key.contentEquals("DEFINITION")) {
          if (!checkDefinition(concept, sample)) {
            errors.add("ERROR: Wrong definition " + sample.getValue() + " of " + sample.getCode());
          }
        } else if ((key.startsWith("rdfs:subClassOf") || key.startsWith("owl:equivalentClass"))
            && key.contains("~")) {
          if (!checkRole(concept, sample)) {
            errors.add("ERROR: Wrong role " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (key.startsWith("qualifier")) {
          sample.setValue(
              StringEscapeUtils.unescapeHtml4(sample.getValue().replaceAll("&apos;", "'")));
          if (!checkQualifier(concept, sample)) {
            errors.add("ERROR: Bad qualifier " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (key.equals("root")) {
          if (concept.getParents().size() > 0) {
            errors.add("ERROR: root " + sample.getCode() + " has parents");
          }
        } else if (key.startsWith("parent-count")) {
          if (concept.getParents().size()
              != Integer.parseInt(key.substring("parent-count".length()))) {
            errors.add(
                "ERROR: concept "
                    + sample.getCode()
                    + " has "
                    + concept.getParents().size()
                    + " parents, "
                    + "stated number "
                    + key.substring("parent-count".length()));
          }
        } else if (key.startsWith("parent-style")) {
          if (!checkParent(concept, sample)) {
            errors.add(
                "ERROR: incorrect parent relationship: "
                    + key
                    + " not a parent of "
                    + sample.getValue());
          }
        } else if (key.startsWith("child-style")) {
          if (!checkChildren(concept, sample)) {
            errors.add(
                "ERROR: incorrect children relationship: "
                    + key
                    + " not a child of "
                    + sample.getValue());
          }
        } else if (key.equals("max-children")) {
          if (concept.getChildren().size() != Integer.parseInt(sample.getValue())) {
            errors.add(
                "ERROR: concept "
                    + sample.getCode()
                    + " has "
                    + concept.getChildren().size()
                    + " children, "
                    + "stated number "
                    + sample.getValue());
          }
        } else if (key.equals("synonym")) {
          if (!concept.getName().equals(sample.getValue())) {
            errors.add("ERROR: Wrong synonym " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (key.equals("term-type")) {
          if (!checkTermType(concept, sample)) {
            errors.add("ERROR: Wrong term type " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (key.contains("disjointWith")) {
          if (!checkDisjointWith(concept, sample)) {
            errors.add(
                "ERROR: "
                    + sample.getValue()
                    + " stated to be disjoint with "
                    + sample.getCode()
                    + " even though they are not");
          }
        } else if (associationsList.size() > 0 && associationsList.keySet().contains(key)) {
          if (!checkAssociations(concept, sample, associationsList)) {
            errors.add(
                "ERROR: Wrong association ("
                    + sample.getKey()
                    + ") "
                    + sample.getValue()
                    + " of "
                    + sample.getCode());
          }
        } else if (sample.getKey().equals(terminology.getMetadata().getSubsetLink())) {
          if (!checkSubsetLink(concept.getCode(), sample)) {
            errors.add(
                "ERROR: Wrong subset link ("
                    + sample.getKey()
                    + ") "
                    + sample.getValue()
                    + " of "
                    + sample.getCode());
          }
        } else {
          String newSample = sample.getKey();
          if (sample.getKey().startsWith(this.terminology.getTerminology() + ":")) {
            newSample = sample.getKey().split(":")[1];
          }
          String colonSample =
              newSample.contains(":") ? sample.getKey().split(":")[1] : newSample; // properties
          // and
          // colon
          // inconsistency
          if (!checkProperties(concept, newSample, propertyList)
              && newSample.equals(colonSample)
              && (!checkProperties(concept, colonSample, propertyList))) {
            errors.add(
                "ERROR: Wrong property ("
                    + sample.getKey()
                    + ") "
                    + sample.getValue()
                    + " of "
                    + sample.getCode());
          }
        }
      }
    }
    if (errors.size() > 0) {
      log.error(
          "SAMPLING ERRORS FOUND IN SAMPLING FOR TERMINOLOGY "
              + terminology.getName()
              + ". SEE LOG BELOW");
      for (final String err : errors) {
        log.error(err);
      }
      // fail("Sampling errors found");
    } else {
      log.info("No sampling errors found for terminology " + terminology.getName());
    }
  }

  /**
   * Check subset link.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   * @throws Exception
   */
  private boolean checkSubsetLink(final String conceptCode, final SampleRecord sample)
      throws Exception {

    String link = sample.getValue().replaceFirst("EVS/", "").split("\\|")[0];
    if (link.contains(".")) {
      link = link.replaceFirst("(.*)/[^/]+\\.[^/]+", "$1");
    }
    link = terminology.getMetadata().getSubsetPrefix() + link;

    if (terminology.getMetadata().getSubsetLink().isBlank()
        || terminology.getMetadata().getSubsetLink().equals(sample.getKey())) {
      String url =
          "/api/v1/subset/" + terminology.getTerminology() + "/" + conceptCode + "?include=summary";
      MvcResult result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      String content = result.getResponse().getContentAsString();
      Concept concept = new ObjectMapper().readValue(content, Concept.class);
      return concept.getSubsetLink().equals(link);
    }
    return false;
  }

  /**
   * Check parent.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkParent(final Concept concept, final SampleRecord sample) {
    return concept.getParents().stream()
        .filter(o -> o.getCode().equals(sample.getValue()))
        .findAny()
        .isPresent();
  }

  /**
   * Check children.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkChildren(final Concept concept, final SampleRecord sample) {
    return concept.getChildren().stream()
        .filter(o -> o.getCode().equals(sample.getValue()))
        .findAny()
        .isPresent();
  }

  /**
   * Check code.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkCode(final Concept concept, final SampleRecord sample) {
    return sample.getKey().equals(terminology.getMetadata().getCode());
  }

  /**
   * Check deprecated.
   *
   * @param concept the concept
   * @return true, if successful
   */
  private boolean checkDeprecated(final Concept concept) {
    return concept.getProperties().stream()
            .filter(
                p -> p.getType().equals("Concept_Status") && p.getValue().equals("Retired_Concept"))
            .findAny()
            .isPresent()
        || concept.getProperties().stream()
            .filter(p -> p.getType().equals("deprecated") && p.getValue().equals("true"))
            .findAny()
            .isPresent()
        || concept.getProperties().stream()
            .filter(p -> p.getType().equals("Status") && p.getValue().equals("No Longer Used"))
            .findAny()
            .isPresent();
  }

  /**
   * Check preferred name.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkPreferredName(final Concept concept, final SampleRecord sample) {
    return sample.getKey().equals(terminology.getMetadata().getPreferredName());
  }

  /**
   * Check synonym.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkSynonym(final Concept concept, final SampleRecord sample) {
    return concept.getSynonyms().stream()
        .filter(o -> o.getName().equals(sample.getValue()))
        .findAny()
        .isPresent();
  }

  /**
   * Check definition.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkDefinition(final Concept concept, final SampleRecord sample) {
    return concept.getDefinitions().stream()
        .filter(o -> o.getDefinition().strip().equals(sample.getValue()))
        .findAny()
        .isPresent();
  }

  /**
   * Check term type.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkTermType(final Concept concept, final SampleRecord sample) {
    return concept.getSynonyms().stream()
        .filter(o -> o.getTermType().equals(sample.getValue()))
        .findAny()
        .isPresent();
  }

  /**
   * Check Properties.
   *
   * @param concept the concept
   * @param sample the sample
   * @param propertyList the property list
   * @return true, if successful
   */
  private boolean checkProperties(
      final Concept concept, final String sample, final Map<String, String> propertyList) {

    return (propertyList.containsKey(sample) || propertyList.containsValue(sample))
        && concept.getProperties().stream()
            .filter(o -> o.getType().equals(sample) || o.getType().equals(propertyList.get(sample)))
            .findAny()
            .isPresent();
  }

  /**
   * Check association.
   *
   * @param concept the concept
   * @param sample the sample
   * @param associationList the association list
   * @return true, if successful
   */
  private boolean checkAssociations(
      final Concept concept, final SampleRecord sample, final Map<String, String> associationList) {

    return (associationList.containsKey(sample.getKey())
            || associationList.containsValue(sample.getKey()))
        && concept.getAssociations().stream()
            .filter(
                o ->
                    o.getRelatedCode().equals(sample.getValue())
                        || o.getRelatedCode().equals(associationList.get(sample.getValue())))
            .findAny()
            .isPresent();
  }

  /**
   * Check qualifier.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean checkQualifier(final Concept concept, final SampleRecord sample)
      throws Exception {
    sample.setValue(sample.getValue());
    final String qualKey = sample.getKey().split("-", 2)[1].split("~")[0];
    final String propertyKey =
        sample
            .getKey()
            .split("-", 2)[1]
            .split("~")[1]
            .replace(terminology.getTerminology() + ":", "");
    final int propertyValueLength = sample.getValue().split("~").length;

    if (propertyValueLength == 2) {

      final String qualValue = sample.getValue().split("~")[0];
      final String propertyValue = sample.getValue().split("~")[1];

      if (terminology.getMetadata().getSynonym().contains(qualKey)) {
        return checkSynonymMetadata(
            concept, sample, qualKey, propertyKey, qualValue, propertyValue);
      } else if (terminology.getMetadata().getDefinition().contains(qualKey)) {
        return checkDefinitionMetadata(concept, sample, propertyKey, qualValue, propertyValue);
      } else if (terminology.getMetadata().getMap() != null
          && terminology.getMetadata().getMap().equals(qualKey)) {
        return checkMaps(concept, sample, propertyKey, qualValue, propertyValue);
      } else if (qualKey.equals("synonym")) {
        return checkSynonymQualifiers(
            concept, sample, qualKey, propertyKey, qualValue, propertyValue);
      } else {
        return checkOther(concept, sample, qualKey, qualValue, propertyKey, propertyValue);
      }

    } else {

      final String propertyValue = sample.getValue();

      if (!concept.getAssociations().isEmpty()) {
        return checkAssociations(concept, sample, qualKey, propertyKey, propertyValue);
      }

      return false;
    }
  }

  /**
   * Check other.
   *
   * @param concept the concept
   * @param sample the sample
   * @param qualKey the qual key
   * @param qualValue the qual value
   * @param propertyKey the property key
   * @param propertyValue the property value
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean checkOther(
      final Concept concept,
      final SampleRecord sample,
      final String qualKey,
      final String qualValue,
      final String propertyKey,
      final String propertyValue)
      throws Exception {
    String url =
        "/api/v1/metadata/"
            + terminology.getTerminology()
            + "/property/"
            + qualKey
            + "?include=minimal";
    MvcResult result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    if (content.equals("[]")) {
      return false;
    }
    final Concept otherProperty = new ObjectMapper().readValue(content, Concept.class);

    url =
        "/api/v1/metadata/"
            + terminology.getTerminology()
            + "/qualifier/"
            + propertyKey
            + "?include=minimal";
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final Concept otherQualifier = new ObjectMapper().readValue(content, Concept.class);
    return concept.getProperties().stream()
        .filter(
            o ->
                o.getType().equals(otherProperty.getName())
                    || o.getType().equals(otherProperty.getCode())
                        && o.getQualifiers() != null
                        && o.getQualifiers().stream()
                            .filter(
                                p ->
                                    p.getType().equals(otherQualifier.getName())
                                        && p.getValue().equals(propertyValue))
                            .findAny()
                            .isPresent())
        .findAny()
        .isPresent();
  }

  /**
   * Check synonym metadata.
   *
   * @param concept the concept
   * @param sample the sample
   * @param qualKey the qual key
   * @param propertyKey the property key
   * @param qualValue the qual value
   * @param propertyValue the property value
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean checkSynonymMetadata(
      final Concept concept,
      final SampleRecord sample,
      final String qualKey,
      final String propertyKey,
      final String qualValue,
      final String propertyValue)
      throws Exception {
    if (propertyKey.equals(terminology.getMetadata().getSynonymTermType())) {
      return concept.getSynonyms().stream()
          .filter(
              o ->
                  o.getName().equals(qualValue)
                      && o.getTermType() != null
                      && o.getTermType().equals(propertyValue))
          .findAny()
          .isPresent();
    } else if (propertyKey.equals(terminology.getMetadata().getSynonymSource())) {
      return concept.getSynonyms().stream()
          .filter(
              o ->
                  o.getName().equals(qualValue)
                      && o.getSource() != null
                      && o.getSource().equals(propertyValue))
          .findAny()
          .isPresent();
    } else if (propertyKey.equals(terminology.getMetadata().getSynonymCode())) {
      return concept.getSynonyms().stream()
          .filter(
              o ->
                  o.getName().equals(qualValue)
                      && o.getCode() != null
                      && o.getCode().equals(propertyValue))
          .findAny()
          .isPresent();
    } else if (propertyKey.equals(terminology.getMetadata().getSynonymSubSource())) {
      return concept.getSynonyms().stream()
          .filter(
              o ->
                  o.getName().equals(qualValue)
                      && o.getSubSource() != null
                      && o.getSubSource().equals(propertyValue))
          .findAny()
          .isPresent();
    } else {
      String url =
          "/api/v1/metadata/"
              + terminology.getTerminology()
              + "/synonymType/"
              + qualKey
              + "?include=minimal";
      MvcResult result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      String content = result.getResponse().getContentAsString();
      log.info(" content = " + content);
      if (content.equals("[]")) {
        return false;
      }
      final Concept otherProperty = new ObjectMapper().readValue(content, Concept.class);

      url =
          "/api/v1/metadata/"
              + terminology.getTerminology()
              + "/qualifier/"
              + propertyKey
              + "?include=minimal";
      result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      log.info(" content = " + content);
      final Concept otherQualifier = new ObjectMapper().readValue(content, Concept.class);
      return concept.getSynonyms().stream()
          .filter(
              o ->
                  o.getType().equals(otherProperty.getName())
                      && o.getQualifiers() != null
                      && o.getQualifiers().stream()
                          .filter(
                              p ->
                                  p.getType().equals(otherQualifier.getName())
                                      && p.getValue().equals(propertyValue))
                          .findAny()
                          .isPresent())
          .findAny()
          .isPresent();
    }
  }

  /**
   * Check definition metadata.
   *
   * @param concept the concept
   * @param sample the sample
   * @param propertyKey the property key
   * @param qualValue the qual value
   * @param propertyValue the property value
   * @return true, if successful
   */
  private boolean checkDefinitionMetadata(
      final Concept concept,
      final SampleRecord sample,
      final String propertyKey,
      final String qualValue,
      final String propertyValue) {
    if (propertyKey.equals(terminology.getMetadata().getDefinitionSource())) {
      return concept.getDefinitions().stream()
          .filter(o -> o.getDefinition().equals(qualValue) && o.getSource().equals(propertyValue))
          .findAny()
          .isPresent();
    } else {
      return concept.getDefinitions().stream()
          .filter(
              o ->
                  o.getDefinition().equals(qualValue)
                      && o.getQualifiers().stream()
                          .filter(p -> p.getValue().equals(propertyValue))
                          .findAny()
                          .isPresent())
          .findAny()
          .isPresent();
    }
  }

  /**
   * Check maps.
   *
   * @param concept the concept
   * @param sample the sample
   * @param propertyKey the property key
   * @param qualValue the qual value
   * @param propertyValue the property value
   * @return true, if successful
   */
  private boolean checkMaps(
      final Concept concept,
      final SampleRecord sample,
      final String propertyKey,
      final String qualValue,
      final String propertyValue) {
    if (propertyKey.equals(terminology.getMetadata().getMapRelation())) {
      return concept.getMaps().stream()
          .filter(o -> o.getTargetName().equals(qualValue) && o.getType().equals(propertyValue))
          .findAny()
          .isPresent();

    } else if (propertyKey.equals(terminology.getMetadata().getMapTarget())) {
      return concept.getMaps().stream()
          .filter(
              o -> o.getTargetName().equals(qualValue) && o.getTargetCode().equals(propertyValue))
          .findAny()
          .isPresent();

    } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTermType())) {
      return concept.getMaps().stream()
          .filter(
              o ->
                  o.getTargetName().equals(qualValue)
                      && o.getTargetTermType().equals(propertyValue))
          .findAny()
          .isPresent();

    } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTerminology())) {
      return concept.getMaps().stream()
          .filter(
              o ->
                  o.getTargetName().equals(qualValue)
                      && o.getTargetTerminology().equals(propertyValue))
          .findAny()
          .isPresent();

    } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTerminologyVersion())) {
      return concept.getMaps().stream()
          .filter(
              o ->
                  o.getTargetName().equals(qualValue)
                      && o.getTargetTerminologyVersion().equals(propertyValue))
          .findAny()
          .isPresent();
    }
    return false;
  }

  /**
   * Check Associations.
   *
   * @param concept the concept
   * @param sample the sample
   * @param qualKey the qual key
   * @param propertyKey the property key
   * @param propertyValue the property value
   * @return true, if successful
   */
  private boolean checkAssociations(
      final Concept concept,
      final SampleRecord sample,
      final String qualKey,
      final String propertyKey,
      final String propertyValue) {

    return concept.getAssociations().stream()
        .filter(
            o ->
                o.getType().equals(qualKey)
                    && o.getQualifiers().stream()
                        .filter(
                            q ->
                                q.getType().equals(propertyKey)
                                    && q.getValue().equals(propertyValue))
                        .findAny()
                        .isPresent())
        .findAny()
        .isPresent();
  }

  /**
   * Check Synonym Qualifiers.
   *
   * @param concept the concept
   * @param sample the sample
   * @param qualKey the qual key
   * @param propertyKey the property key
   * @param qualValue the qual value
   * @param propertyValue the property value
   * @return true, if successful
   */
  private boolean checkSynonymQualifiers(
      final Concept concept,
      final SampleRecord sample,
      final String qualKey,
      final String propertyKey,
      final String qualValue,
      final String propertyValue) {

    return concept.getSynonyms().stream()
        .filter(
            o ->
                o.getName().equals(qualValue)
                    && o.getQualifiers().stream()
                        .filter(
                            q ->
                                q.getType().equals(propertyKey)
                                    && q.getValue().equals(propertyValue))
                        .findAny()
                        .isPresent())
        .findAny()
        .isPresent();
  }

  /**
   * Check disjoint With.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkDisjointWith(final Concept concept, final SampleRecord sample) {
    return concept.getDisjointWith().stream()
        .filter(
            o ->
                o.getRelatedCode().equals(sample.getValue())
                    || o.getRelatedCode().contentEquals(sample.getValue().replace("_", ":")))
        .findAny()
        .isPresent();
  }

  /**
   * Check label.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkLabel(final Concept concept, final SampleRecord sample) {
    return concept.getName().equals(sample.getValue());
  }

  /**
   * Check design note comment.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkComment(final Concept concept, final SampleRecord sample) {
    return concept.getProperties().stream()
        .filter(
            o ->
                standardizeText(o.getValue()).equals(sample.getValue())
                    && o.getType().contentEquals("rdfs:comment"))
        .findAny()
        .isPresent();
  }

  /**
   * Check role.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean checkRole(final Concept concept, final SampleRecord sample) throws Exception {
    final String role = sample.getKey().split("~")[1];
    final String url =
        "/api/v1/metadata/" + terminology.getTerminology() + "/role/" + role + "?include=minimal";
    final MvcResult result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final Concept minMatchedRole = new ObjectMapper().readValue(content, Concept.class);
    final Role conceptMatchedRole =
        concept.getRoles().stream()
            .filter(
                o ->
                    o.getRelatedCode().equals(sample.getValue())
                        && o.getType().equals(minMatchedRole.getName()))
            .findAny()
            .orElse(null);
    return conceptMatchedRole != null
        && conceptMatchedRole.getType().contentEquals(minMatchedRole.getName());
  }

  /**
   * Perform paths subtree and roots tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   * @throws Exception the exception
   */
  public void performPathsSubtreeAndRootsTests(
      final String term, final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc)
      throws Exception {

    MvcResult result = null;
    testMvc = mvc;
    lookupTerminology(term);
    String parentCode1 = null;
    String parentCode2 = null;
    Boolean hasRoots = false;
    for (List<SampleRecord> values : sampleMap.values()) {
      for (SampleRecord property : values) {
        if (property.getKey().equals("parent-count1")) {
          parentCode1 = property.getCode();
        } else if (property.getKey().equals("parent-count2")) {
          parentCode2 = property.getCode();
        } else if (!hasRoots && property.getKey().equals("root")) {
          hasRoots = true;
        }
      }
      if (parentCode1 != null && parentCode2 != null) break;
    }

    if (parentCode1 == null && parentCode2 == null) {

      log.warn("No parent relationships found for " + terminology.getName());
      return;
    }

    String url;
    String content;
    List<String> rootCodes;
    String ancestorCode;
    if (hasRoots) {
      // roots testing

      url = "/api/v1/concept/" + terminology.getTerminology() + "/roots";
      result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      List<Concept> roots =
          new ObjectMapper()
              .readValue(
                  content,
                  new TypeReference<List<Concept>>() {
                    // n/a
                  });
      rootCodes = roots.stream().map(entry -> entry.getCode()).collect(Collectors.toList());
      if (terminology.getMetadata().getHierarchy() == true && roots.size() == 0) {
        errors.add("ERROR: roots could not be found in hierarchy temrinology " + term);
      } else if (terminology.getMetadata().getHierarchy() == false && roots.size() > 0) {
        errors.add("ERROR: roots found in non-hierarchy temrinology " + term);
      }
      // pathsToRoot testing
      url =
          "/api/v1/concept/"
              + terminology.getTerminology()
              + "/"
              + parentCode1
              + "/pathsToRoot?include=minimal";
      result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      ancestorCode = null;
      List<String> reverseToRootPath = null;
      List<List<Concept>> pathsToRoot =
          new ObjectMapper()
              .readValue(
                  content,
                  new TypeReference<List<List<Concept>>>() {
                    // n/a
                  });
      if (pathsToRoot.size() < 1) {
        errors.add(
            "ERROR: no paths to root found for non-root concept "
                + parentCode1
                + " in terminology "
                + term);

      } else {
        for (List<Concept> path : pathsToRoot) {
          if (!rootCodes.contains(path.get(path.size() - 1).getCode())) {
            errors.add(
                "ERROR: path too root for concept "
                    + parentCode1
                    + " ends in non-root concept "
                    + path.get(path.size() - 1).getCode()
                    + " in terminology "
                    + term);
          }
          // hold an intermediate code for pathToAncestor
          if (path.size() > 2 && ancestorCode == null) {
            ancestorCode = path.get(path.size() - 2).getCode();
          }
          // hold a reverse root path for pathsFromRoot
          if (path.size() > 1 && reverseToRootPath == null) {
            reverseToRootPath =
                path.stream().map(entry -> entry.getCode()).collect(Collectors.toList());
            Collections.reverse(reverseToRootPath);
          }
        }
      }
      // pathsFromRoot testing
      url =
          "/api/v1/concept/"
              + terminology.getTerminology()
              + "/"
              + parentCode1
              + "/pathsFromRoot?include=minimal";
      result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      List<String> fromRootPath = null;
      Boolean reversePathFound = false;
      List<List<Concept>> pathsFromRoot =
          new ObjectMapper()
              .readValue(
                  content,
                  new TypeReference<List<List<Concept>>>() {
                    // n/a
                  });
      if (pathsFromRoot.size() < 1) {
        errors.add(
            "ERROR: no paths from root found for non-root concept "
                + parentCode1
                + " in terminology "
                + term);
      } else {
        for (List<Concept> path : pathsFromRoot) {
          if (!rootCodes.contains(path.get(0).getCode())) {
            errors.add(
                "ERROR: path from root for concept "
                    + parentCode1
                    + " starts in non-root concept "
                    + path.get(0).getCode()
                    + " in terminology "
                    + term);
          }
          // check for reverse of path found in pathsToRoot
          fromRootPath = path.stream().map(entry -> entry.getCode()).collect(Collectors.toList());
          if (fromRootPath.equals(reverseToRootPath)) {
            reversePathFound = true;
          }
        }
      }
      if (!reversePathFound) {
        errors.add(
            "ERROR: Chosen reverse path from pathsToRoot not found in pathsFromRoot for concept "
                + parentCode1
                + " in terminology "
                + term);
      }
      if (parentCode2 != null) {
        url =
            "/api/v1/concept/"
                + terminology.getTerminology()
                + "/"
                + parentCode2
                + "/pathsFromRoot?include=minimal";
        result =
            testMvc
                .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                .andExpect(status().isOk())
                .andReturn();
        content = result.getResponse().getContentAsString();
        pathsFromRoot =
            new ObjectMapper()
                .readValue(
                    content,
                    new TypeReference<List<List<Concept>>>() {
                      // n/a
                    });
        if (pathsFromRoot.size() < 1) {
          errors.add(
              "ERROR: no paths from root found for non-root concept "
                  + parentCode2
                  + " in terminology "
                  + term);
        }
      }
      // pathsToAncestor testing
      url =
          "/api/v1/concept/"
              + terminology.getTerminology()
              + "/"
              + parentCode1
              + "/pathsToAncestor/"
              + ancestorCode
              + "?include=minimal";
      result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      List<List<Concept>> pathsToAncestor =
          new ObjectMapper()
              .readValue(
                  content,
                  new TypeReference<List<List<Concept>>>() {
                    // n/a
                  });
      for (List<Concept> path : pathsToAncestor) {
        if (!path.get(0).getCode().equals(parentCode1)) {
          errors.add(
              "ERROR: path to ancestor "
                  + ancestorCode
                  + " for concept "
                  + parentCode1
                  + " starts with different concept from stated "
                  + path.get(0).getCode()
                  + " in terminology "
                  + term);
        }
        if (!path.get(path.size() - 1).getCode().equals(ancestorCode)) {
          errors.add(
              "ERROR: path to ancestor "
                  + ancestorCode
                  + " for concept "
                  + parentCode1
                  + " ends in different concept from stated "
                  + path.get(path.size() - 1).getCode()
                  + " in terminology "
                  + term);
        }
      }
      // subtree testing
      url = "/api/v1/concept/" + terminology.getTerminology() + "/" + parentCode1 + "/subtree";
      result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      List<HierarchyNode> subtree =
          new ObjectMapper()
              .readValue(
                  content,
                  new TypeReference<List<HierarchyNode>>() {
                    // n/a
                  });
      for (HierarchyNode root : subtree) {
        if (!rootCodes.contains(root.getCode())) {
          errors.add(
              "ERROR: non-root found at top level of subtree call for concept "
                  + parentCode1
                  + " in terminology "
                  + term);
        }
      }
    }

    // subtree/children testing
    url = "/api/v1/concept/" + terminology.getTerminology() + "/" + parentCode1 + "/subtree";
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<HierarchyNode> children =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<HierarchyNode>>() {
                  // n/a
                });
    if (children.size() < 1) {
      // TODO: what's supposed to happen here?!
    }

    if (errors.size() > 0) {
      log.error(
          "SAMPLING ERRORS FOUND IN SAMPLING FOR TERMINOLOGY "
              + terminology.getName()
              + ". SEE LOG BELOW");
      for (final String err : errors) {
        log.error(err);
      }
      fail("Sampling errors found");
    } else {
      log.info(
          "No sampling errors found for terminology "
              + terminology.getName()
              + " in paths, subtree, and root testing.");
    }
  }

  /**
   * Perform search tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   * @throws Exception the exception
   */
  public void performSearchTests(
      final String term, final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc)
      throws Exception {

    MvcResult result = null;
    testMvc = mvc;
    String url = null;
    String content = null;
    Concept testConcept = null;
    lookupTerminology(term);
    for (List<SampleRecord> values : sampleMap.values()) {
      for (SampleRecord property : values) {
        url =
            "/api/v1/concept/"
                + terminology.getTerminology()
                + "/"
                + property.getCode()
                + "?include=minimal";
        result =
            testMvc
                .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                .andExpect(status().isOk())
                .andReturn();
        content = result.getResponse().getContentAsString();
        testConcept =
            new ObjectMapper()
                .readValue(
                    content,
                    new TypeReference<Concept>() {
                      // n/a
                    });
        // make sure we have a 2+ word concept to test with
        if (testConcept.getName().split(" ").length > 1) {
          break;
        }
      }
    }
    ConceptResultList list = null;
    if (testConcept == null) {
      log.error("No valid search concept tests found");
      return;
    }

    final String testCode = testConcept.getCode();
    final String testName = testConcept.getName().toLowerCase();
    final String[] splitTestName = testName.toLowerCase().split(" ");
    url =
        "/api/v1/concept/search?include=minimal&terminology="
            + terminology.getTerminology()
            + "&term="
            + testCode;
    log.info(url);
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() > 0);
    assertThat(
        list.getConcepts().stream()
            .anyMatch(
                o -> o.getName().toLowerCase().equals(testName) && o.getCode().equals(testCode)));

    url =
        "/api/v1/concept/search?include=minimal&terminology="
            + terminology.getTerminology()
            + "&term="
            + testConcept.getCode().toLowerCase();
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() > 0);
    assertThat(
        list.getConcepts().stream()
            .anyMatch(o -> o.getName().equals(testName) && o.getCode().equals(testCode)));

    // testing the types

    url =
        "/api/v1/concept/search?include=minimal&type=match&terminology="
            + terminology.getTerminology()
            + "&term="
            + testConcept.getName();
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() > 0);
    for (Concept conc : list.getConcepts()) {
      assertThat(
          conc.getName().equals(testName)
              || conc.getSynonyms().stream()
                  .anyMatch(o -> o.getName().toLowerCase().equals(testName))
              || conc.getDefinitions().stream()
                  .anyMatch(o -> o.getDefinition().toLowerCase().equals(testName)));
    }

    url =
        "/api/v1/concept/search?include=minimal&type=startsWith&terminology="
            + terminology.getTerminology()
            + "&term="
            + testConcept.getName();
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() > 0);
    for (Concept conc : list.getConcepts()) {
      assertThat(
          conc.getName().startsWith(testName)
              || conc.getSynonyms().stream()
                  .anyMatch(o -> o.getName().toLowerCase().startsWith(testName))
              || conc.getDefinitions().stream()
                  .anyMatch(o -> o.getDefinition().toLowerCase().startsWith(testName)));
    }

    url =
        "/api/v1/concept/search?include=minimal&type=phrase&terminology="
            + terminology.getTerminology()
            + "&term="
            + testConcept.getName();
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() > 0);
    for (Concept conc : list.getConcepts()) {
      assertThat(
          conc.getName().startsWith(testName)
              || conc.getSynonyms().stream()
                  .anyMatch(o -> o.getName().toLowerCase().contains(testName))
              || conc.getDefinitions().stream()
                  .anyMatch(o -> o.getDefinition().toLowerCase().contains(testName)));
    }

    url =
        "/api/v1/concept/search?include=minimal&type=AND&terminology="
            + terminology.getTerminology()
            + "&term="
            + testConcept.getName();
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() > 0);
    for (Concept conc : list.getConcepts()) {
      assertThat(
          Arrays.stream(splitTestName).anyMatch(conc.getName()::contains)
              || conc.getSynonyms().stream()
                  .anyMatch(o -> matchOrAnd(o.getName().toLowerCase(), splitTestName, "AND"))
              || conc.getDefinitions().stream()
                  .anyMatch(
                      o -> matchOrAnd(o.getDefinition().toLowerCase(), splitTestName, "AND")));
    }

    url =
        "/api/v1/concept/search?include=minimal&type=OR&terminology="
            + terminology.getTerminology()
            + "&term="
            + testConcept.getName();
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() > 0);
    for (Concept conc : list.getConcepts()) {
      assertThat(
          Arrays.stream(splitTestName).anyMatch(conc.getName()::contains)
              || conc.getSynonyms().stream()
                  .anyMatch(o -> matchOrAnd(o.getName().toLowerCase(), splitTestName, "OR"))
              || conc.getDefinitions().stream()
                  .anyMatch(o -> matchOrAnd(o.getDefinition().toLowerCase(), splitTestName, "OR")));
    }

    // test fromRecord and page size
    url =
        "/api/v1/concept/search?include=minimal&terminology="
            + terminology.getTerminology()
            + "&term="
            + testConcept.getName()
            + "&pageSize=6";
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() > 0);
    assertThat(list.getConcepts().size()).isLessThanOrEqualTo(6);
    if (list.getConcepts().size() > 10) {
      Concept eleventhConcept = list.getConcepts().get(10);

      url =
          "/api/v1/concept/search?include=minimal&terminology="
              + terminology.getTerminology()
              + "&term="
              + testConcept.getName()
              + "&fromRecord=10";
      result =
          testMvc
              .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      list = new ObjectMapper().readValue(content, ConceptResultList.class);
      assertThat(list.getTotal() > 0);
      assertThat(list.getConcepts().get(0)).isEqualTo(eleventhConcept);
    }

    log.info("No errors found in search tests for " + terminology.getName());
  }

  /**
   * Match or and.
   *
   * @param stringToMatch the string to match
   * @param termStrings the term strings
   * @param orAnd the or and
   * @return true, if successful
   */
  public boolean matchOrAnd(String stringToMatch, String[] termStrings, String orAnd) {
    if (orAnd.equals("AND")) {
      return Arrays.stream(termStrings).allMatch(stringToMatch::contains);
    } else if (orAnd.equals("OR")) {
      return Arrays.stream(termStrings).anyMatch(stringToMatch::contains);
    }
    return false;
  }

  /**
   * Perform subsets tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   * @throws Exception the exception
   */
  public void performSubsetsTests(
      final String term, final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc)
      throws Exception {

    // Instead of only performing on ncit, perform on all but require no entries for non-ncit

    MvcResult result = null;
    testMvc = mvc;
    lookupTerminology(term);

    String url = "/api/v1/subset/" + terminology.getTerminology() + "?include=full";
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    List<Concept> roots =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });

    // Check for "ncit"
    if (term.equals("ncit")) {

      Boolean leafChecked = false;
      Boolean childChecked = false;
      for (Concept root : roots) {
        if (!root.getTerminology().equals("ncit")) {
          errors.add(
              "Subset root code "
                  + root.getCode()
                  + " has terminology "
                  + root.getTerminology()
                  + " instead of ncit");
        }
        if (!childChecked) {
          if (root.getChildren().size() == 0) {
            errors.add("Subset root code " + root.getCode() + " has no children");
          }
          childChecked = true;
        }

        if (!leafChecked) {
          String leafCode = getLeafCode(root);
          if (leafCode == null) {
            errors.add("No leaf found in subset " + root.getCode());
            break;
          }
          url =
              "/api/v1/subset/"
                  + terminology.getTerminology()
                  + "/"
                  + leafCode
                  + "?include=summary";
          result =
              testMvc
                  .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                  .andExpect(status().isOk())
                  .andReturn();
          content = result.getResponse().getContentAsString();
          Concept firstLeaf =
              new ObjectMapper()
                  .readValue(
                      content,
                      new TypeReference<Concept>() {
                        // n/a
                      });
          /*
           * if (firstLeaf.getSubsetLink() == null) { errors.add("Subset leaf " + leafCode +
           * " has no subset link"); }
           */
          String firstLeafDesc =
              firstLeaf.getProperties().stream()
                  .filter(p -> p.getType().equals("Term_Browser_Value_Set_Description"))
                  .collect(Collectors.toList())
                  .get(0)
                  .getValue();
          if (firstLeaf.getProperties().stream()
              .noneMatch(
                  d ->
                      d.getType().equals("Term_Browser_Value_Set_Description")
                          && d.getValue().equals(firstLeafDesc))) {
            errors.add(
                "No matching Term_Browser_Value_Set_Description property found in subset leaf "
                    + firstLeaf.getCode());
          }
          if (firstLeaf.getProperties().stream()
              .anyMatch(p -> p.getType().equals("Value_Set_Location"))) {
            errors.add("Value_Set_Location property found in subset leaf " + firstLeaf.getCode());
          }

          url =
              "/api/v1/subset/"
                  + terminology.getTerminology()
                  + "/"
                  + leafCode
                  + "/members?include=minimal&fromRecord=0";
          result =
              testMvc
                  .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
                  .andExpect(status().isOk())
                  .andReturn();
          content = result.getResponse().getContentAsString();
          List<Concept> firstLeafMembers =
              new ObjectMapper()
                  .readValue(
                      content,
                      new TypeReference<List<Concept>>() {
                        // n/a
                      });
          if (firstLeafMembers.size() < 10) {
            errors.add(
                "Leaf Node " + firstLeaf.getCode() + " has fewer than 10 subsets containing it");
          }
          leafChecked = true;
        }
      }
      if (errors.size() > 0) {
        log.error(
            "SAMPLING ERRORS FOUND IN SUBSET SAMPLING FOR TERMINOLOGY "
                + terminology.getName()
                + ". SEE LOG BELOW");
        for (final String err : errors) {
          log.error(err);
        }
        fail("Sampling errors found");
      } else {
        log.info(
            "No sampling errors found for terminology "
                + terminology.getName()
                + " in subset testing.");
      }
    }

    // Verify non-ncit
    else {
      assertThat(roots).isEmpty();
    }
  }

  /**
   * find leaf in subset concept.
   *
   * @param root the root
   * @return the leaf code
   * @throws Exception
   */
  public String getLeafCode(Concept root) throws Exception {
    String url =
        "/api/v1/subset/"
            + terminology.getTerminology()
            + "/"
            + root.getCode()
            + "?include=summary";
    MvcResult result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    Concept rootSubset =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<Concept>() {
                  // n/a
                });
    if (rootSubset == null || rootSubset.getLeaf() == null) {
      return null;
    }
    if (rootSubset.getLeaf()) {
      return rootSubset.getCode();
    }
    String rootCode = null;
    for (Concept child : rootSubset.getChildren()) {
      rootCode = getLeafCode(child);
      if (rootCode != null) {
        return rootCode;
      }
    }
    return null;
  }

  /**
   * Perform association tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   * @throws Exception the exception
   */
  public void performAssociationEntryTests(
      final String term, final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc)
      throws Exception {
    // Only perform these tests on ncit
    if (!term.equals("ncit")) {
      return;
    }

    MvcResult result = null;
    testMvc = mvc;
    lookupTerminology(term);

    String url = "/api/v1/metadata/" + terminology.getTerminology() + "/associations";
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();

    List<Concept> associations =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    Concept firstAssociation = null;
    for (Concept assoc : associations) {
      if (!assoc.getTerminology().equals("ncit")) {
        errors.add(
            "ncit association " + assoc.getCode() + " has terminology " + assoc.getTerminology());
      } else if (!assoc.getCode().equals("A8")) {
        firstAssociation = assoc;
        break;
      }
    }
    if (firstAssociation == null) {
      errors.add("No associations found for NCIT");
      return;
    }

    url =
        "/api/v1/concept/"
            + terminology.getTerminology()
            + "/associations/"
            + firstAssociation.getCode();
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    AssociationEntryResultList fullFirstAssociationByCode =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<AssociationEntryResultList>() {
                  // n/a
                });

    url =
        "/api/v1/concept/"
            + terminology.getTerminology()
            + "/associations/"
            + firstAssociation.getName();
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    AssociationEntryResultList fullFirstAssociationByName =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<AssociationEntryResultList>() {
                  // n/a
                });

    if (!fullFirstAssociationByCode.equals(fullFirstAssociationByName)) {
      errors.add(
          "search of association by matching code and name "
              + firstAssociation.getCode()
              + "/"
              + firstAssociation.getName()
              + "returned different results");
    }

    url =
        "/api/v1/concept/"
            + terminology.getTerminology()
            + "/associations/"
            + firstAssociation.getCode()
            + "?fromRecord=0&pageSize=2";
    result =
        testMvc
            .perform(get(url).header("X-EVSRESTAPI-License-Key", licenseKey))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    AssociationEntryResultList firstFromRecord =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<AssociationEntryResultList>() {
                  // n/a
                });

    url =
        "/api/v1/concept/"
            + terminology.getTerminology()
            + "/associations/"
            + firstAssociation.getCode()
            + "?fromRecord=1&pageSize=2";
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    AssociationEntryResultList secondFromRecord =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<AssociationEntryResultList>() {
                  // n/a
                });
    assertThat(fullFirstAssociationByCode.getTotal())
        .isEqualTo(fullFirstAssociationByName.getTotal());
    assertThat(
        firstFromRecord
            .getAssociationEntries()
            .get(1)
            .equals(secondFromRecord.getAssociationEntries().get(0)));
  }

  public String standardizeText(String toStandardize) {
    return StringEscapeUtils.unescapeHtml4(toStandardize)
        .replace("&apos;", "'")
        .replaceAll("\\s{2,}", " ")
        .strip();
  }
}
