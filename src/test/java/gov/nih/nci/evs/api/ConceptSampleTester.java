
package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.controller.NciControllerTests;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;

/**
 * Test harness for content samples.
 */
@AutoConfigureMockMvc
public class ConceptSampleTester {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(NciControllerTests.class);

  /** The base url. */
  private String baseUrl = "/api/v1/concept/";

  /** The term url. */
  private String termUrl = "/api/v1/metadata/terminologies";

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  private MockMvc testMvc;

  /** The terminology. */
  private Terminology terminology;

  /** The errors. */
  private List<String> errors = new ArrayList<String>();

  /**
   * Instantiates an empty {@link ConceptSampleTester}.
   */
  public ConceptSampleTester() {
    // n/a
  }

  /**
   * Sets the terminology.
   *
   * @param term the term
   * @param mvc the mvc
   * @throws Exception the exception
   */
  private void lookupTerminology(final String term, final MockMvc mvc) throws Exception {
    String url = termUrl + "?latest=true&terminology=" + term;
    if (term.equals("ncit")) {
      url += "&tag=monthly";
    }

    final MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Terminology> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
          // n/a
        });
    terminology = list.get(0);
  }

  /**
   * Perform metadata tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   * @throws Exception the exception
   */
  public void performMetadataTests(final String term,
    final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc) throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    testMvc = mvc;
    String content = null;
    Concept concept = null;
    lookupTerminology(term, testMvc);

    for (final Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
      url = baseUrl + term + "/" + entry.getKey() + "?include=full";
      log.info("Testing url - " + url);
      result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
      content = result.getResponse().getContentAsString();
      log.info(" content = " + content);
      concept = new ObjectMapper().readValue(content, Concept.class);
      assertThat(content).isNotNull();
      assertThat(concept).isNotNull();
      log.info(content);
      for (final SampleRecord sample : entry.getValue()) {
        // TODO
        // * if association - verify /metadata/{terminology}/association/{code} exists
        // * if qualifier - verify /metadata/{terminology}/qualifier/{code} exists
        // * if role - verify /metadata/{terminology}/role/{code} exists
        // * if synonym term type - verify /metadata/{terminology}/termType/{code} exists
        // * if synonym source - verify /metadata/{terminology}/synonymSource/{code} exists
        // * if definition type - verify /metadata/{terminology}/definitionType/{code} exists
        // * if definition source - verify /metadata/{terminology}/definitionSource/{code} exists
        // * if property - verify /metadata/{terminology}/property/{code} exists

      }
    }
  }

  /**
   * Perform content tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   * @throws Exception the exception
   */
  public void performContentTests(final String term,
    final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc) throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    testMvc = mvc;
    String content = null;
    Concept concept = null;
    lookupTerminology(term, testMvc);

    for (final Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
      url = baseUrl + term + "/" + entry.getKey() + "?include=full";
      log.info("Testing url - " + url);
      result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
      content = result.getResponse().getContentAsString();
      log.info(" content = " + content);
      concept = new ObjectMapper().readValue(content, Concept.class);
      assertThat(content).isNotNull();
      log.info(content);
      for (final SampleRecord sample : entry.getValue()) {
        final String key = sample.getKey();
        if (key.startsWith("refs:subClassOf") && !key.contains("~")) {
          if (!checkParent(concept, sample)) {
            errors.add("ERROR: Wrong parent " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (key.equals(terminology.getMetadata().getCode())) {
          if (!checkCode(concept, sample)) {
            errors.add("ERROR: Wrong terminology code " + sample.getValue() + " of "
                + terminology.getName());
          }
        } else if (key.equals(terminology.getMetadata().getPreferredName())) {
          if (!checkPreferredName(concept, sample)) {
            errors.add("ERROR: Wrong terminology preferred name code " + sample.getKey() + " of "
                + terminology.getName());
          }
        } else if (terminology.getMetadata().getSynonym().contains(key)) {
          if (!checkSynonym(concept, sample)) {
            errors.add("ERROR: Wrong synonym " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (terminology.getMetadata().getDefinition().contains(key)) {
          if (!checkDefinition(concept, sample)) {
            errors.add("ERROR: Wrong synonym " + sample.getValue() + " of " + sample.getCode());
          }
        } else if ((key.startsWith("rdfs:subClassOf") || key.startsWith("owl:equivalentClass"))
            && key.contains("~")) {
          if (!checkRole(concept, sample)) {
            errors.add("ERROR: Wrong role " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (key.startsWith("qualifier")) {
          if (!checkQualifier(concept, sample)) {
            errors.add("ERROR: Bad qualifier " + sample.getValue() + " of " + sample.getCode());
          }
        } else if (key.equals("root")) {
          if (concept.getParents().size() > 0) {
            errors.add("ERROR: root " + sample.getCode() + " has parents");
          }
        } else if (key.startsWith("parent-count")) {
          if (concept.getParents().size() != Integer
              .parseInt(key.substring("parent-count".length()))) {
            errors.add("ERROR: concept " + sample.getCode() + " has " + concept.getParents().size()
                + " parents, " + "stated number " + key.substring("parent-count".length()));
          }
        } else if (key.startsWith("parent-style")) {
          if (!checkParent(concept, sample)) {
            errors.add("ERROR: incorrect parent relationship: " + key + " not a parent of "
                + sample.getValue());
          }
        } else if (key.startsWith("child-style")) {
          if (!checkChildren(concept, sample)) {
            errors.add("ERROR: incorrect children relationship: " + key + " not a child of "
                + sample.getValue());
          }
        } else if (key.equals("max-children")) {
          if (concept.getChildren().size() != Integer.parseInt(sample.getValue())) {
            errors.add("ERROR: concept " + sample.getCode() + " has " + concept.getChildren().size()
                + " children, " + "stated number " + sample.getValue());
          }
        } else if (key.equals("synonym")) {
            if (!concept.getName().equals(sample.getValue())) {
              errors.add("ERROR: Wrong synonym " + sample.getValue() + " of " + sample.getCode());
            }
        } else if (key.equals("term-type")) {
            if (!checkTermType(concept, sample)) {
              errors.add("ERROR: Wrong term type " + sample.getValue() + " of " + sample.getCode());
            }
        } else {
          continue;
        }
      }
    }
    if (errors.size() > 0) {
      log.error("SAMPLING ERRORS FOUND IN SAMPLING FOR TERMINOLOGY " + terminology.getName()
          + ". SEE LOG BELOW");
      for (final String err : errors) {
        log.error(err);
      }
    } else {
      log.info("No sampling errors found for terminology " + terminology.getName());
    }
  }

  /**
   * Check parent.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkParent(final Concept concept, final SampleRecord sample) {
    return concept.getParents().stream().filter(o -> o.getCode().equals(sample.getValue()))
        .findAny().isPresent();
  }

  /**
   * Check children.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkChildren(final Concept concept, final SampleRecord sample) {
    return concept.getChildren().stream().filter(o -> o.getCode().equals(sample.getValue()))
        .findAny().isPresent();
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
    return concept.getSynonyms().stream().filter(o -> o.getName().equals(sample.getValue()))
        .findAny().isPresent();
  }
  
  /**
   * Check term type.
   *
   * @param concept the concept
   * @param sample the sample
   * @return true, if successful
   */
  private boolean checkTermType(final Concept concept, final SampleRecord sample) {
    return concept.getSynonyms().stream().filter(o -> o.getTermType().equals(sample.getValue()))
        .findAny().isPresent();
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
        .filter(o -> o.getDefinition().equals(sample.getValue())).findAny().isPresent();
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
    final String qualKey = sample.getKey().split("-", 2)[1].split("~")[0];
    final String qualValue = sample.getValue().split("~")[0];
    final String propertyKey = sample.getKey().split("-", 2)[1].split("~")[1];
    final String propertyValue = sample.getValue().split("~")[1];
    if (terminology.getMetadata().getSynonym().contains(qualKey)) {
      return checkSynonymMetadata(concept, sample, qualKey, propertyKey, qualValue, propertyValue);
    } else if (terminology.getMetadata().getDefinition().contains(qualKey)) {
      return checkDefinitionMetadata(concept, sample, propertyKey, qualValue, propertyValue);
    } else if (terminology.getMetadata().getMap() != null
        && terminology.getMetadata().getMap().equals(qualKey)) {
      return checkMaps(concept, sample, propertyKey, qualValue, propertyValue);
    } else {
      return checkOther(concept, sample, qualKey, qualValue, propertyKey, propertyValue);
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
  private boolean checkOther(final Concept concept, final SampleRecord sample, final String qualKey,
    final String qualValue, final String propertyKey, final String propertyValue) throws Exception {
    String url = "/api/v1/metadata/" + terminology.getTerminology() + "/property/" + qualKey
        + "?include=minimal";
    MvcResult result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    if (content.equals("[]")) {
      return false;
    }
    final Concept otherProperty = new ObjectMapper().readValue(content, Concept.class);

    url = "/api/v1/metadata/" + terminology.getTerminology() + "/qualifier/" + propertyKey
        + "?include=minimal";
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final Concept otherQualifier = new ObjectMapper().readValue(content, Concept.class);
    return concept.getProperties().stream()
        .filter(o -> o.getType().equals(otherProperty.getName())
            || o.getType().equals(otherProperty.getCode()) && o.getQualifiers() != null
                && o.getQualifiers().stream()
                    .filter(p -> p.getType().equals(otherQualifier.getName())
                        && p.getValue().equals(propertyValue))
                    .findAny().isPresent())
        .findAny().isPresent();
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
  private boolean checkSynonymMetadata(final Concept concept, final SampleRecord sample,
    final String qualKey, final String propertyKey, final String qualValue,
    final String propertyValue) throws Exception {
    if (propertyKey.equals(terminology.getMetadata().getSynonymTermType())) {
      return concept
          .getSynonyms().stream().filter(o -> o.getName().equals(qualValue)
              && o.getTermType() != null && o.getTermType().equals(propertyValue))
          .findAny().isPresent();
    } else if (propertyKey.equals(terminology.getMetadata().getSynonymSource())) {
      return concept.getSynonyms().stream().filter(o -> o.getName().equals(qualValue)
          && o.getSource() != null && o.getSource().equals(propertyValue)).findAny().isPresent();
    } else if (propertyKey.equals(terminology.getMetadata().getSynonymCode())) {
      return concept.getSynonyms().stream().filter(o -> o.getName().equals(qualValue)
          && o.getCode() != null && o.getCode().equals(propertyValue)).findAny().isPresent();
    } else if (propertyKey.equals(terminology.getMetadata().getSynonymSubSource())) {
      return concept
          .getSynonyms().stream().filter(o -> o.getName().equals(qualValue)
              && o.getSubSource() != null && o.getSubSource().equals(propertyValue))
          .findAny().isPresent();
    } else {
      String url = "/api/v1/metadata/" + terminology.getTerminology() + "/synonymType/" + qualKey
          + "?include=minimal";
      MvcResult result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
      String content = result.getResponse().getContentAsString();
      log.info(" content = " + content);
      if (content.equals("[]")) {
        return false;
      }
      final Concept otherProperty = new ObjectMapper().readValue(content, Concept.class);

      url = "/api/v1/metadata/" + terminology.getTerminology() + "/qualifier/" + propertyKey
          + "?include=minimal";
      result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
      content = result.getResponse().getContentAsString();
      log.info(" content = " + content);
      final Concept otherQualifier = new ObjectMapper().readValue(content, Concept.class);
      return concept.getSynonyms().stream()
          .filter(o -> o.getType().equals(otherProperty.getName()) && o.getQualifiers() != null
              && o.getQualifiers().stream()
                  .filter(p -> p.getType().equals(otherQualifier.getName())
                      && p.getValue().equals(propertyValue))
                  .findAny().isPresent())
          .findAny().isPresent();
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
  private boolean checkDefinitionMetadata(final Concept concept, final SampleRecord sample,
    final String propertyKey, final String qualValue, final String propertyValue) {
    if (propertyKey.equals(terminology.getMetadata().getDefinitionSource())) {
      return concept.getDefinitions().stream()
          .filter(o -> o.getDefinition().equals(qualValue) && o.getSource().equals(propertyValue))
          .findAny().isPresent();
    } else {
      return concept.getDefinitions().stream()
          .filter(o -> o.getDefinition().equals(qualValue) && o.getQualifiers().stream()
              .filter(p -> p.getValue().equals(propertyValue)).findAny().isPresent())
          .findAny().isPresent();
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
  private boolean checkMaps(final Concept concept, final SampleRecord sample,
    final String propertyKey, final String qualValue, final String propertyValue) {
    if (propertyKey.equals(terminology.getMetadata().getMapRelation())) {
      return concept.getMaps().stream()
          .filter(o -> o.getTargetName().equals(qualValue) && o.getType().equals(propertyValue))
          .findAny().isPresent();

    } else if (propertyKey.equals(terminology.getMetadata().getMapTarget())) {
      return concept.getMaps().stream()
          .filter(
              o -> o.getTargetName().equals(qualValue) && o.getTargetCode().equals(propertyValue))
          .findAny().isPresent();

    } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTermType())) {
      return concept.getMaps().stream().filter(
          o -> o.getTargetName().equals(qualValue) && o.getTargetTermType().equals(propertyValue))
          .findAny().isPresent();

    } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTerminology())) {
      return concept.getMaps().stream().filter(o -> o.getTargetName().equals(qualValue)
          && o.getTargetTerminology().equals(propertyValue)).findAny().isPresent();

    } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTerminologyVersion())) {
      return concept.getMaps().stream().filter(o -> o.getTargetName().equals(qualValue)
          && o.getTargetTerminologyVersion().equals(propertyValue)).findAny().isPresent();
    }
    return false;
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
    final MvcResult result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final Concept minMatchedRole = new ObjectMapper().readValue(content, Concept.class);
    final Role conceptMatchedRole =
        concept.getRoles().stream().filter(o -> o.getRelatedCode().equals(sample.getValue())
            && o.getType().equals(minMatchedRole.getName())).findAny().orElse(null);
    return conceptMatchedRole != null
        && conceptMatchedRole.getType().contentEquals(minMatchedRole.getName());
  }

  /**
   * Perform paths subtree and roots tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   */
  public void performPathsSubtreeAndRootsTests(final String term,
    final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc) {

    // TODO (Perform a search for "cancer" and pick the first result)
    // 1. /roots (already verified by "root" entries) - so just call and gather
    // the roots (shouldbe non-zero if terminology.getMetadata().isHierarchy()
    // )
    // 2. /pathsToRoot
    // - call and verify that each path starts with this concept and ends with
    // one of the roots
    // 3. /pathsFromRoot
    // - same as #2, but verify the reverse order
    // 4. /pathsToAncestor (use values from #2 to determine how to make this
    // call)
    // - choose an intermediate node from the #2 results and compute paths to
    // that ancestor, and then verify that the paths start with this concept and
    // end with that concept

  }

  /**
   * Perform search tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   */
  public void performSearchTests(final String term, final Map<String, List<SampleRecord>> sampleMap,
    final MockMvc mvc) {

    // TODO
    // code = pick the first code from samples and look it up
    // term = pick the preferred name of that code
    // term2 = remove the shortest word from term
    // term3 = pick the first 2 words
    // word = pick the longest word from term
    // word2 = pick the first
    // 1. Search by code (pick the first code from "samples")
    // 2. Search by lowercase code (pick the first code from "samples")
    // 3. Search by type
    // - contains
    // - match
    // - phrase
    // - startsWith
    // - and
    // - or
    // 4. test fromRecord/pageSize
    //

  }

  /**
   * Perform subsets tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   */
  public void performSubsetsTests(final String term,
    final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc) {
    // Only perform these tests on ncit
    if (!term.equals("ncit")) {
      return;
    }

    // TODO
    // 1. /metadata/{terminology}/subsets
    // - verify that the "Ncit" subsets are first
    // - verify that the first entry has children
    // 2. /metadata/{terminology}/subset/{code} on the first leaf nod code of
    // the first subset
    // - verify that it has a subset link
    // - verify that it has a Term_Browser_Value_Set_Description property (that
    // matches the subset description)
    // - verify that it does not have a Value_Set_Location property
    // 3. call /concept/{terminology}/subsetMembers/{code} on that same code
    // - verify that it has >10 members
  }

  /**
   * Perform association tests.
   *
   * @param term the term
   * @param sampleMap the sample map
   * @param mvc the mvc
   */
  public void performAssociationEntryTests(final String term,
    final Map<String, List<SampleRecord>> sampleMap, final MockMvc mvc) {
    // Only perform these tests on ncit
    if (!term.equals("ncit")) {
      return;
    }

    // TODO
    // 1. Call /metadata/ncit/associations and choose the first association that
    // is not "A8"
    // 2. Call /concept/ncit/associations/{code} with the code from the
    // association chosen in #1
    // 3. Call /concept/ncit/associations/{code} with the value (e.g. label like
    // "Has_CDRH_PARENT") from the association chosen in #1
    // 4. Repeat #2 with fromRecord=0 and pageSize=2
    // - verify #2 results with totalCount matching the count from #2
    // 5. Repeat #2 with fromRecord=1 and pageSize=2
    // - verify the 1st entry matches the 2nd entry from #4

  }

}
