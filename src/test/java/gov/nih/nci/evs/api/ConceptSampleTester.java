
package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;

/**
 * Test harness for content samples.
 */
@AutoConfigureMockMvc
public class ConceptSampleTester {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ConceptSampleTester.class);

    /** The base url. */
    private String baseUrl = "/api/v1/concept/";

    /** The base metadata url. */
    private String baseMetadataUrl = "/api/v1/metadata/";

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
     * @param mvc  the mvc
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
        final List<Terminology> list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
            // n/a
        });
        terminology = list.get(0);
    }

    /**
     * Perform metadata tests.
     *
     * @param term      the term
     * @param sampleMap the sample map
     * @param mvc       the mvc
     * @throws Exception the exception
     */
    public void performMetadataTests(final String term, final Map<String, List<SampleRecord>> sampleMap,
            final MockMvc mvc) throws Exception {
        String url = baseMetadataUrl;
        MvcResult result = null;
        testMvc = mvc;
        String content = null;
        lookupTerminology(term, testMvc);

        // get associations
        url = baseMetadataUrl + term + "/associations?include=minimal";
        result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<String> associations = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        }).stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        // get qualifiers
        url = baseMetadataUrl + term + "/qualifiers?include=minimal";
        result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<Concept> qualifiers = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        });
        List<Concept> remodeledQualifiers = qualifiers.stream()
                .filter(x -> x.getProperties().stream().anyMatch(y -> y.getType().equals("remodeled")))
                .collect(Collectors.toList());
        qualifiers = qualifiers.stream()
                .filter(x -> x.getProperties().stream().noneMatch(y -> y.getType().equals("remodeled")))
                .collect(Collectors.toList());

        List<String> qualifiersString = qualifiers.stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        List<String> remodeledQualifierString = remodeledQualifiers.stream()
                .filter(x -> x.getProperties().stream().anyMatch(y -> y.getType().equals("remodeled")))
                .collect(Collectors.toList()).stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        for (String prop : remodeledQualifierString) {
            if (!terminology.getMetadata().isRemodeledQualifier(prop)) {
                errors.add("Qualifier " + prop + " listed as remodeled, but isn't");
            }
        }

        // get roles
        url = baseMetadataUrl + term + "/roles?include=minimal";
        result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<String> roles = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        }).stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        // get synonym term types
        url = baseMetadataUrl + term + "/termTypes?include=minimal";
        result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<String> termTypes = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        }).stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        // get synonym sources
        url = baseMetadataUrl + term + "/synonymSources?include=minimal";
        result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<String> synonymSources = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        }).stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        // get definition types
        url = baseMetadataUrl + term + "/definitionTypes?include=minimal";
        result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<String> definitionTypes = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        }).stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        // get definition sources
        url = baseMetadataUrl + term + "/definitionSources?include=minimal";
        result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<String> definitionSources = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        }).stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        // get properties
        url = baseMetadataUrl + term + "/properties?include=properties";
        result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<Concept> properties = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        });
        List<Concept> remodeledProperties = properties.stream()
                .filter(x -> x.getProperties().stream().anyMatch(y -> y.getType().equals("remodeled")))
                .collect(Collectors.toList());

        properties = properties.stream()
                .filter(x -> x.getProperties().stream().noneMatch(y -> y.getType().equals("remodeled")))
                .collect(Collectors.toList());

        List<String> propertiesString = properties.stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        List<String> remodeledPropertyString = remodeledProperties.stream()
                .filter(x -> x.getProperties().stream().anyMatch(y -> y.getType().equals("remodeled")))
                .collect(Collectors.toList()).stream().map(entry -> entry.getCode()).collect(Collectors.toList());

        for (String prop : remodeledPropertyString) {
            if (!terminology.getMetadata().isRemodeledProperty(prop)) {
                errors.add("Property " + prop + " listed as remodeled, but isn't");
            }
        }

        for (final Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
            url = baseUrl + term + "/" + entry.getKey() + "?include=full";
            log.info("Testing url - " + url);
            result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
            content = result.getResponse().getContentAsString();
            log.info(" content = " + content);
            assertThat(content).isNotNull();

            for (final SampleRecord sample : entry.getValue()) {
                // TODO
                String sampleKey = sample.getKey();
                String synDefProperty = null;

                if (sample.getKey().contains("~")) {
                    sampleKey = sample.getKey().split("~")[1];
                    if (sample.getKey().split("~").length > 2) {
                        synDefProperty = sample.getKey().split("~")[2];
                    }
                }
                if (sampleKey.startsWith(term + ":")) {
                    sampleKey = sampleKey.replace(term + ":", ""); // temp fix for classpaths #1
                }

                if (associations.contains(sampleKey)) {
                    url = baseMetadataUrl + term + "/association/" + sampleKey;
                    if (mvc.perform(get(url)).andExpect(status().isOk()) != null) {
                        associations.remove(sampleKey);
                    } else {
                        errors.add("Association error: " + sampleKey + " does not exist in " + term + " associations");
                    }
                } else if (qualifiers.contains(sampleKey)) {
                    url = baseMetadataUrl + term + "/qualifier/" + sampleKey;
                    if (mvc.perform(get(url)).andExpect(status().isOk()) != null) {
                        qualifiers.remove(sampleKey);
                    } else {
                        errors.add("Qualifier error: " + sampleKey + " does not exist in " + term + " qualifiers");
                    }
                } else if (roles.contains(sampleKey)) {
                    url = baseMetadataUrl + term + "/role/" + sampleKey;
                    if (mvc.perform(get(url)).andExpect(status().isOk()) != null) {
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
                    if (mvc.perform(get(url)).andExpect(status().isOk()) != null) {
                        definitionTypes.remove(sampleKey);
                    } else {
                        errors.add("Definition Type error: " + sampleKey + " does not exist in " + term
                                + " definition types");
                    }
                } else if (propertiesString.contains(sampleKey)) {
                    url = baseMetadataUrl + term + "/property/" + sampleKey;
                    if (mvc.perform(get(url)).andExpect(status().isOk()) != null) {
                        propertiesString.remove(sampleKey);
                    } else {
                        errors.add("Property error: " + sampleKey + " does not exist in " + term + " properties");
                    }
                } else if (propertiesString.contains(term + ":" + sampleKey)) { // some terms actually do need the
                    // term name
                    String termNameAndTerm = term + ":" + sampleKey;
                    url = baseMetadataUrl + term + "/property/" + termNameAndTerm;
                    if (mvc.perform(get(url)).andExpect(status().isOk()) != null) {
                        propertiesString.remove(termNameAndTerm);
                    } else {
                        errors.add("Property error: " + termNameAndTerm + " does not exist in " + term + " properties");
                    }
                }
            }
        }
        if (errors.size() > 0) {
            log.error("METADATA ERRORS FOUND IN SAMPLING FOR TERMINOLOGY " + terminology.getName() + ". SEE LOG BELOW");
            for (final String err : errors) {
                log.error(err);
            }
        } else {
            log.info("No metadata errors found for terminology " + terminology.getName());
        }
        if (associations.size() > 0)
            log.info("Associations not covered in sampling: " + Arrays.toString(associations.toArray()));
        if (qualifiers.size() > 0)
            log.info("Qualifiers not covered in sampling: " + Arrays.toString(qualifiers.toArray()));
        if (roles.size() > 0)
            log.info("Roles not covered in sampling: " + Arrays.toString(roles.toArray()));
        if (termTypes.size() > 0)
            log.info("Synonym term types not covered in sampling: " + Arrays.toString(termTypes.toArray()));
        if (synonymSources.size() > 0)
            log.info("Synonym sources not covered in sampling: " + Arrays.toString(synonymSources.toArray()));
        if (definitionSources.size() > 0)
            log.info("Definition sources not covered in sampling: " + Arrays.toString(definitionSources.toArray()));
        if (definitionTypes.size() > 0)
            log.info("Definition types not covered in sampling: " + Arrays.toString(definitionTypes.toArray()));
        if (properties.size() > 0)
            log.info("Properties not covered in sampling: " + Arrays.toString(properties.toArray()));
    }

    /**
     * Perform content tests.
     *
     * @param term      the term
     * @param sampleMap the sample map
     * @param mvc       the mvc
     * @throws Exception the exception
     */
    public void performContentTests(final String term, final Map<String, List<SampleRecord>> sampleMap,
            final MockMvc mvc)
            throws Exception {
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
                        errors.add(
                                "ERROR: Wrong terminology code " + sample.getValue() + " of " + terminology.getName());
                    }
                } else if (key.equals(terminology.getMetadata().getPreferredName())) {
                    if (!checkPreferredName(concept, sample)) {
                        errors.add(
                                "ERROR: Wrong terminology preferred name code " + sample.getKey() + " of "
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
                    if (concept.getParents().size() != Integer.parseInt(key.substring("parent-count".length()))) {
                        errors.add("ERROR: concept " + sample.getCode() + " has " + concept.getParents().size()
                                + " parents, "
                                + "stated number " + key.substring("parent-count".length()));
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
                                + " children, "
                                + "stated number " + sample.getValue());
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
            log.error("SAMPLING ERRORS FOUND IN SAMPLING FOR TERMINOLOGY " + terminology.getName() + ". SEE LOG BELOW");
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
     * @param sample  the sample
     * @return true, if successful
     */
    private boolean checkParent(final Concept concept, final SampleRecord sample) {
        return concept.getParents().stream().filter(o -> o.getCode().equals(sample.getValue())).findAny().isPresent();
    }

    /**
     * Check children.
     *
     * @param concept the concept
     * @param sample  the sample
     * @return true, if successful
     */
    private boolean checkChildren(final Concept concept, final SampleRecord sample) {
        return concept.getChildren().stream().filter(o -> o.getCode().equals(sample.getValue())).findAny().isPresent();
    }

    /**
     * Check code.
     *
     * @param concept the concept
     * @param sample  the sample
     * @return true, if successful
     */
    private boolean checkCode(final Concept concept, final SampleRecord sample) {
        return sample.getKey().equals(terminology.getMetadata().getCode());
    }

    /**
     * Check preferred name.
     *
     * @param concept the concept
     * @param sample  the sample
     * @return true, if successful
     */
    private boolean checkPreferredName(final Concept concept, final SampleRecord sample) {
        return sample.getKey().equals(terminology.getMetadata().getPreferredName());
    }

    /**
     * Check synonym.
     *
     * @param concept the concept
     * @param sample  the sample
     * @return true, if successful
     */
    private boolean checkSynonym(final Concept concept, final SampleRecord sample) {
        return concept.getSynonyms().stream().filter(o -> o.getName().equals(sample.getValue())).findAny().isPresent();
    }

    /**
     * Check definition.
     *
     * @param concept the concept
     * @param sample  the sample
     * @return true, if successful
     */
    private boolean checkDefinition(final Concept concept, final SampleRecord sample) {
        return concept.getDefinitions().stream().filter(o -> o.getDefinition().equals(sample.getValue())).findAny()
                .isPresent();
    }

    /**
     * Check term type.
     *
     * @param concept the concept
     * @param sample  the sample
     * @return true, if successful
     */
    private boolean checkTermType(final Concept concept, final SampleRecord sample) {
        return concept.getSynonyms().stream().filter(o -> o.getTermType().equals(sample.getValue())).findAny()
                .isPresent();
    }

    /**
     * Check qualifier.
     *
     * @param concept the concept
     * @param sample  the sample
     * @return true, if successful
     * @throws Exception the exception
     */
    private boolean checkQualifier(final Concept concept, final SampleRecord sample) throws Exception {
        final String qualKey = sample.getKey().split("-", 2)[1].split("~")[0];
        final String propertyKey = sample.getKey().split("-", 2)[1].split("~")[1];
        final int propertyValueLength = sample.getValue().split("~").length;

        if (propertyValueLength == 2) {

            final String qualValue = sample.getValue().split("~")[0];
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
     * @param concept       the concept
     * @param sample        the sample
     * @param qualKey       the qual key
     * @param qualValue     the qual value
     * @param propertyKey   the property key
     * @param propertyValue the property value
     * @return true, if successful
     * @throws Exception the exception
     */
    private boolean checkOther(final Concept concept, final SampleRecord sample, final String qualKey,
            final String qualValue, final String propertyKey, final String propertyValue) throws Exception {
        String url = "/api/v1/metadata/" + terminology.getTerminology() + "/property/" + qualKey + "?include=minimal";
        MvcResult result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        String content = result.getResponse().getContentAsString();
        log.info(" content = " + content);
        if (content.equals("[]")) {
            return false;
        }
        final Concept otherProperty = new ObjectMapper().readValue(content, Concept.class);

        url = "/api/v1/metadata/" + terminology.getTerminology() + "/qualifier/" + propertyKey + "?include=minimal";
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
     * @param concept       the concept
     * @param sample        the sample
     * @param qualKey       the qual key
     * @param propertyKey   the property key
     * @param qualValue     the qual value
     * @param propertyValue the property value
     * @return true, if successful
     * @throws Exception the exception
     */
    private boolean checkSynonymMetadata(final Concept concept, final SampleRecord sample, final String qualKey,
            final String propertyKey, final String qualValue, final String propertyValue) throws Exception {
        if (propertyKey.equals(terminology.getMetadata().getSynonymTermType())) {
            return concept.getSynonyms().stream()
                    .filter(
                            o -> o.getName().equals(qualValue) && o.getTermType() != null
                                    && o.getTermType().equals(propertyValue))
                    .findAny().isPresent();
        } else if (propertyKey.equals(terminology.getMetadata().getSynonymSource())) {
            return concept.getSynonyms().stream()
                    .filter(o -> o.getName().equals(qualValue) && o.getSource() != null
                            && o.getSource().equals(propertyValue))
                    .findAny().isPresent();
        } else if (propertyKey.equals(terminology.getMetadata().getSynonymCode())) {
            return concept.getSynonyms().stream()
                    .filter(o -> o.getName().equals(qualValue) && o.getCode() != null
                            && o.getCode().equals(propertyValue))
                    .findAny().isPresent();
        } else if (propertyKey.equals(terminology.getMetadata().getSynonymSubSource())) {
            return concept.getSynonyms().stream()
                    .filter(
                            o -> o.getName().equals(qualValue) && o.getSubSource() != null
                                    && o.getSubSource().equals(propertyValue))
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

            url = "/api/v1/metadata/" + terminology.getTerminology() + "/qualifier/" + propertyKey + "?include=minimal";
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
     * @param concept       the concept
     * @param sample        the sample
     * @param propertyKey   the property key
     * @param qualValue     the qual value
     * @param propertyValue the property value
     * @return true, if successful
     */
    private boolean checkDefinitionMetadata(final Concept concept, final SampleRecord sample, final String propertyKey,
            final String qualValue, final String propertyValue) {
        if (propertyKey.equals(terminology.getMetadata().getDefinitionSource())) {
            return concept.getDefinitions().stream()
                    .filter(o -> o.getDefinition().equals(qualValue) && o.getSource().equals(propertyValue)).findAny()
                    .isPresent();
        } else {
            return concept.getDefinitions().stream()
                    .filter(o -> o.getDefinition().equals(qualValue)
                            && o.getQualifiers().stream().filter(p -> p.getValue().equals(propertyValue)).findAny()
                                    .isPresent())
                    .findAny().isPresent();
        }
    }

    /**
     * Check maps.
     *
     * @param concept       the concept
     * @param sample        the sample
     * @param propertyKey   the property key
     * @param qualValue     the qual value
     * @param propertyValue the property value
     * @return true, if successful
     */
    private boolean checkMaps(final Concept concept, final SampleRecord sample, final String propertyKey,
            final String qualValue, final String propertyValue) {
        if (propertyKey.equals(terminology.getMetadata().getMapRelation())) {
            return concept.getMaps().stream()
                    .filter(o -> o.getTargetName().equals(qualValue) && o.getType().equals(propertyValue)).findAny()
                    .isPresent();

        } else if (propertyKey.equals(terminology.getMetadata().getMapTarget())) {
            return concept.getMaps().stream()
                    .filter(o -> o.getTargetName().equals(qualValue) && o.getTargetCode().equals(propertyValue))
                    .findAny()
                    .isPresent();

        } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTermType())) {
            return concept.getMaps().stream()
                    .filter(o -> o.getTargetName().equals(qualValue) && o.getTargetTermType().equals(propertyValue))
                    .findAny()
                    .isPresent();

        } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTerminology())) {
            return concept.getMaps().stream()
                    .filter(o -> o.getTargetName().equals(qualValue) && o.getTargetTerminology().equals(propertyValue))
                    .findAny()
                    .isPresent();

        } else if (propertyKey.equals(terminology.getMetadata().getMapTargetTerminologyVersion())) {
            return concept.getMaps().stream()
                    .filter(o -> o.getTargetName().equals(qualValue)
                            && o.getTargetTerminologyVersion().equals(propertyValue))
                    .findAny().isPresent();
        }
        return false;
    }

    /**
     * Check Associations.
     *
     * @param concept       the concept
     * @param sample        the sample
     * @param qualKey       the qual key
     * @param propertyKey   the property key
     * @param propertyValue the property value
     * @return true, if successful
     */
    private boolean checkAssociations(final Concept concept, final SampleRecord sample, final String qualKey,
            final String propertyKey, final String propertyValue) {

        return concept.getAssociations().stream()
                .filter(o -> o.getType().equals(qualKey) && o.getQualifiers().stream()
                        .filter(q -> q.getType().equals(propertyKey) && q.getValue().equals(propertyValue)).findAny()
                        .isPresent())
                .findAny().isPresent();
    }

    /**
     * Check role.
     *
     * @param concept the concept
     * @param sample  the sample
     * @return true, if successful
     * @throws Exception the exception
     */
    private boolean checkRole(final Concept concept, final SampleRecord sample) throws Exception {
        final String role = sample.getKey().split("~")[1];
        final String url = "/api/v1/metadata/" + terminology.getTerminology() + "/role/" + role + "?include=minimal";
        final MvcResult result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        final String content = result.getResponse().getContentAsString();
        log.info(" content = " + content);
        final Concept minMatchedRole = new ObjectMapper().readValue(content, Concept.class);
        final Role conceptMatchedRole = concept.getRoles().stream()
                .filter(o -> o.getRelatedCode().equals(sample.getValue())
                        && o.getType().equals(minMatchedRole.getName()))
                .findAny().orElse(null);
        return conceptMatchedRole != null && conceptMatchedRole.getType().contentEquals(minMatchedRole.getName());
    }

    /**
     * Perform paths subtree and roots tests.
     *
     * @param term      the term
     * @param sampleMap the sample map
     * @param mvc       the mvc
     * @throws Exception the exception
     */
    public void performPathsSubtreeAndRootsTests(final String term, final Map<String, List<SampleRecord>> sampleMap,
            final MockMvc mvc) throws Exception {

        MvcResult result = null;
        testMvc = mvc;
        lookupTerminology(term, testMvc);
        String parentCode1 = null;
        String parentCode2 = null;
        for (List<SampleRecord> values : sampleMap.values()) {
            for (SampleRecord property : values) {
                if (property.getKey().equals("parent-count1")) {
                    parentCode1 = property.getCode();
                } else if (property.getKey().equals("parent-count2")) {
                    parentCode2 = property.getCode();
                }
            }
            if (parentCode1 != null && parentCode2 != null)
                break;
        }

        // roots testing
        String url = "/api/v1/concept/" + terminology.getTerminology() + "/roots";
        result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        String content = result.getResponse().getContentAsString();
        List<Concept> roots = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
            // n/a
        });
        List<String> rootCodes = roots.stream().map(entry -> entry.getCode()).collect(Collectors.toList());
        if (terminology.getMetadata().getHierarchy() == true && roots.size() == 0) {
            errors.add("ERROR: roots could not be found in hierarchy temrinology " + term);
        } else if (terminology.getMetadata().getHierarchy() == false && roots.size() > 0) {
            errors.add("ERROR: roots found in non-hierarchy temrinology " + term);
        }

        // pathsToRoot testing
        url = "/api/v1/concept/" + terminology.getTerminology() + "/" + parentCode1 + "/pathsToRoot?include=minimal";
        result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        String ancestorCode = null;
        List<String> reverseToRootPath = null;
        List<List<Concept>> pathsToRoot = new ObjectMapper().readValue(content,
                new TypeReference<List<List<Concept>>>() {
                    // n/a
                });
        if (pathsToRoot.size() < 1) {
            errors.add("ERROR: no paths to root found for non-root concept " + parentCode1 + " in terminology " + term);

        } else {
            for (List<Concept> path : pathsToRoot) {
                if (!rootCodes.contains(path.get(path.size() - 1).getCode())) {
                    errors.add("ERROR: path too root for concept " + parentCode1 + " ends in non-root concept "
                            + path.get(path.size() - 1).getCode() + " in terminology " + term);
                }
                // hold an intermediate code for pathToAncestor
                if (path.size() > 2 && ancestorCode == null) {
                    ancestorCode = path.get(path.size() - 2).getCode();
                }
                // hold a reverse root path for pathsFromRoot
                if (path.size() > 1 && reverseToRootPath == null) {
                    reverseToRootPath = path.stream().map(entry -> entry.getCode()).collect(Collectors.toList());
                    Collections.reverse(reverseToRootPath);
                }
            }
        }

        // pathsFromRoot testing
        url = "/api/v1/concept/" + terminology.getTerminology() + "/" + parentCode1 + "/pathsFromRoot?include=minimal";
        result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<String> fromRootPath = null;
        Boolean reversePathFound = false;
        List<List<Concept>> pathsFromRoot = new ObjectMapper().readValue(content,
                new TypeReference<List<List<Concept>>>() {
                    // n/a
                });
        if (pathsFromRoot.size() < 1) {
            errors.add(
                    "ERROR: no paths from root found for non-root concept " + parentCode1 + " in terminology " + term);
        } else {
            for (List<Concept> path : pathsFromRoot) {
                if (!rootCodes.contains(path.get(0).getCode())) {
                    errors.add("ERROR: path from root for concept " + parentCode1 + " starts in non-root concept "
                            + path.get(0).getCode() + " in terminology " + term);
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
                    "ERROR: Chosen reverse path from pathsToRoot not found in pathsFromRoot for concept " + parentCode1
                            + " in terminology " + term);
        }
        if (parentCode2 != null) {
            url = "/api/v1/concept/" + terminology.getTerminology() + "/" + parentCode2
                    + "/pathsFromRoot?include=minimal";
            result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
            content = result.getResponse().getContentAsString();
            pathsFromRoot = new ObjectMapper().readValue(content, new TypeReference<List<List<Concept>>>() {
                // n/a
            });
            if (pathsFromRoot.size() < 1) {
                errors.add("ERROR: no paths from root found for non-root concept " + parentCode2 + " in terminology "
                        + term);
            }
        }

        // pathsToAncestor testing
        url = "/api/v1/concept/" + terminology.getTerminology() + "/" + parentCode1 + "/pathsToAncestor/" + ancestorCode
                + "?include=minimal";
        result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<List<Concept>> pathsToAncestor = new ObjectMapper().readValue(content,
                new TypeReference<List<List<Concept>>>() {
                    // n/a
                });
        for (List<Concept> path : pathsToAncestor) {
            if (!path.get(0).getCode().equals(parentCode1)) {
                errors.add("ERROR: path to ancestor " + ancestorCode + " for concept " + parentCode1
                        + " starts with different concept from stated " + path.get(0).getCode() + " in terminology "
                        + term);
            }
            if (!path.get(path.size() - 1).getCode().equals(ancestorCode)) {
                errors.add("ERROR: path to ancestor " + ancestorCode + " for concept " + parentCode1
                        + " ends in different concept from stated " + path.get(path.size() - 1).getCode()
                        + " in terminology "
                        + term);
            }
        }

        // subtree testing
        url = "/api/v1/concept/" + terminology.getTerminology() + "/" + parentCode1 + "/subtree";
        result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<HierarchyNode> subtree = new ObjectMapper().readValue(content, new TypeReference<List<HierarchyNode>>() {
            // n/a
        });
        for (HierarchyNode root : subtree) {
            if (!rootCodes.contains(root.getCode())) {
                errors.add("ERROR: non-root found at top level of subtree call for concept " + parentCode1
                        + " in terminology "
                        + term);
            }
        }

        // subtree/children testing
        url = "/api/v1/concept/" + terminology.getTerminology() + "/" + parentCode1 + "/subtree";
        result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        content = result.getResponse().getContentAsString();
        List<HierarchyNode> children = new ObjectMapper().readValue(content, new TypeReference<List<HierarchyNode>>() {
            // n/a
        });
        if (children.size() < 1) {

        }

        if (errors.size() > 0) {
            log.error("SAMPLING ERRORS FOUND IN SAMPLING FOR TERMINOLOGY " + terminology.getName() + ". SEE LOG BELOW");
            for (final String err : errors) {
                log.error(err);
            }
        } else {
            log.info("No sampling errors found for terminology " + terminology.getName()
                    + " in paths, subtree, and root testing.");
        }

    }

    /**
     * Perform search tests.
     *
     * @param term      the term
     * @param sampleMap the sample map
     * @param mvc       the mvc
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
     * @param term      the term
     * @param sampleMap the sample map
     * @param mvc       the mvc
     */
    public void performSubsetsTests(final String term, final Map<String, List<SampleRecord>> sampleMap,
            final MockMvc mvc) {
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
     * @param term      the term
     * @param sampleMap the sample map
     * @param mvc       the mvc
     */
    public void performAssociationEntryTests(final String term, final Map<String, List<SampleRecord>> sampleMap,
            final MockMvc mvc) {
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
