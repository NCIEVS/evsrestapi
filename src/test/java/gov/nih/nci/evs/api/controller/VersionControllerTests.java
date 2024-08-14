package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.support.ApplicationVersion;
import gov.nih.nci.evs.api.util.ConceptUtils;

/** Integration tests for VersionController. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class VersionControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(VersionControllerTests.class);

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** The base url. */
  private String baseUrl = "/api/v1";

  /** Sets the up. */
  @Before
  public void setUp() {
    /*
     * Configure the JacksonTester object
     */
    this.objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  /**
   * Test version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testVersion() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = baseUrl + "/version";
    log.info("Testing url - " + url);

    // Test a basic term search
    result = this.mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotEmpty();
    final ApplicationVersion data = new ObjectMapper().readValue(content, ApplicationVersion.class);
    final String buildGradleVersion = getBuildGradleVersion();
    assertThat(data.getVersion()).isEqualTo(buildGradleVersion);
  }

  /**
   * Test rewrite.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRewrite() throws Exception {
    String url = null;
    MvcResult result = null;
    String evsExploreUrl = null;

    final String baseEvsExploreUrl = "https://evsexplore.semantics.cancer.gov/evsexplore";

    final Map<String, String> urlMap =
        ConceptUtils.asMap(
            // Unrelated URL
            "https://www.google.com",
            baseEvsExploreUrl + "/welcome",

            // Base URL
            "https://ncit.nci.nih.gov",
            baseEvsExploreUrl + "/welcome?terminology=multi",
            "https://ncit.nci.nih.gov/",
            baseEvsExploreUrl + "/welcome?terminology=multi",
            "https://nciterms.nci.nih.gov",
            baseEvsExploreUrl + "/welcome?terminology=multi",
            "https://nciterms.nci.nih.gov/",
            baseEvsExploreUrl + "/welcome?terminology=multi",
            "https://ncim.nci.nih.gov",
            baseEvsExploreUrl + "/welcome?terminology=ncim",
            "https://ncim.nci.nih.gov/",
            baseEvsExploreUrl + "/welcome?terminology=ncim",
            "http://nciws-p1086-c.nci.nih.gov:8081/",
            baseEvsExploreUrl + "/welcome?terminology=multi",

            // ncitbrowser url
            "https://ncit.nci.nih.gov/ncitbrowser/",
            baseEvsExploreUrl + "/welcome",
            "https://nciterms.nci.nih.gov/ncitbrowser/",
            baseEvsExploreUrl + "/welcome",

            // Search results page
            "https://ncit.nci.nih.gov/ncitbrowser/pages/message.jsf",
            baseEvsExploreUrl + "/welcome",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/message.jsf",
            baseEvsExploreUrl + "/welcome",

            // Start URL
            "https://nciterms.nci.nih.gov/ncitbrowser/start.jsf",
            baseEvsExploreUrl + "/welcome?terminology=multi",

            // Subsets
            "https://ncit.nci.nih.gov/ncitbrowser/ajax?action=create_src_vs_tree",
            baseEvsExploreUrl + "/subsets/ncit",
            // "NCI Thesaurus Subsets"
            "https://ncit.nci.nih.gov/ncitbrowser/pages/subset.jsf",
            baseEvsExploreUrl + "/subsets/ncit",
            // Subsets - specific
            "https://ncit.nci.nih.gov/ncitbrowser/ajax?action=create_src_vs_tree&vsd_uri=http://evs.nci.nih.gov/valueset/CTRP/C116977",
            baseEvsExploreUrl + "/subset/ncit/C116977",

            // Mappings
            "https://ncit.nci.nih.gov/ncitbrowser/pages/mapping_search.jsf?nav_type=mappings&b=0&m=0",
            baseEvsExploreUrl + "/mappings",
            // Mappings variation
            "https://ncit.nci.nih.gov/ncitbrowser/pages/mapping_search.jsf?b=0&nav_type=mappings",
            baseEvsExploreUrl + "/mappings",

            // Mappings specific
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=GO_to_NCIt_Mapping&version=1.1",
            baseEvsExploreUrl + "/mappings/GO_to_NCIt_Mapping",

            // TODO:
            // ICD-10_to_MedDRA_Mapping
            // MA_to_NCIt_Mapping
            // NCIt_to_ChEBI_Mapping
            // NCIt_to_HGNC_Mapping
            // PDQ_2016_07_31_TO_NCI_2016_10E
            // SNOMEDCT_US_2023_09_01_TO_ICD10CM_2023
            // SNOMEDCT_US_2023_09_01_TO_ICD10_2016

            // Mappings bogus
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=abcdef12345&version=1.1",
            baseEvsExploreUrl + "/welcome",

            // Termform
            "https://ncitermform.nci.nih.gov/",
            baseEvsExploreUrl + "/termform",

            // Terminology bogus
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ABCDEF&version=2018_05_07",
            baseEvsExploreUrl + "/welcome",

            // MED-RT specific version
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MED-RT&version=2018_05_07",
            baseEvsExploreUrl + "/welcome?terminology=medrt",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MED-RT&version=2018_05_07",
            baseEvsExploreUrl + "/welcome?terminology=medrt",

            // MED-RT specific concept
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=MED-RT&version=2023_09_05&ns=MED-RT&code=N0000191625&key=1741397173&b=1&n=null",
            baseEvsExploreUrl + "/concept/medrt/N0000191625",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=MED-RT&version=2023_09_05&code=N0000191625&ns=MED-RT&type=synonym&key=1741397173&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/medrt/N0000191625",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=N0000191625&ns=MED-RT&ontology_display_name=MED-RT&version=2023_09_05",
            baseEvsExploreUrl + "/hierarchy/medrt/N0000191625"

            // CanMED -> canmed
            // ChEBI -> chebi
            // CTCAE -> ctcae5
            // CTCAE_v5 -> ctcae5
            // DUO -> duo
            // GO -> go
            // HGNC -> hgnc
            // HL7 -> hl7v30
            // ICD-10-CM -> icd10cm
            // ICD-10 -> icd10
            // ICD-9-CM -> icd9cm
            // LOINC -> lnc
            // MA -> ma
            // MedDRA -> mdr
            // MGEDOntology.owl -> mged
            // NDFRT -> ndfrt
            // obi -> obi
            // obib -> obib
            // PDQ -> pdq
            // RadLex -> radlex
            // SNOMED%20Clinical%20Terms%20US%20Edition - snomedct_us
            // UMLS_SemNet -> umlssemnet
            // Zebrafish -> zebrafish

            // TODO: other terminologies

            // TODO: concept details (any of the tabs, any terminology)

            // TODO: ncim concept details
            // end
            );

    url = baseUrl + "/rewrite";

    for (final String lexEvsUrlBase : urlMap.keySet()) {
      // Create http/https variants, create http/https variants
      final List<String> variants = new ArrayList<>();
      variants.add(lexEvsUrlBase);
      if (lexEvsUrlBase.startsWith("https")) {
        // Add http variant
        variants.add(lexEvsUrlBase.replaceFirst("https", "http"));
        // Add stage variant
        variants.add(lexEvsUrlBase.replaceFirst("https://([a-z]+)", "https://$1-stage"));
        // Add http stage variant
        variants.add(lexEvsUrlBase.replaceFirst("https://([a-z]+)", "http://$1-stage"));
      }
      for (final String lexEvsUrl : variants) {
        log.info("Testing url - " + url + "?url=" + lexEvsUrl);
        result =
            this.mvc
                .perform(get(url).param("url", lexEvsUrl))
                .andExpect(status().isOk())
                .andReturn();
        evsExploreUrl = result.getResponse().getContentAsString();
        // log.info("    content = " + evsExploreUrl);
        assertThat(evsExploreUrl).isNotEmpty();
        //      final String evsExploreUrl = new ObjectMapper().readValue(content, String.class);
        final String urlMapUrl =
            (lexEvsUrl.contains("-stage") && lexEvsUrl.contains("nci"))
                ? urlMap.get(lexEvsUrlBase).replaceFirst("evsexplore", "evsexplore-stage")
                : urlMap.get(lexEvsUrlBase);
        log.info("      lexEvsUrl = " + lexEvsUrl);
        log.info("      mapUrl = " + urlMapUrl);
        log.info("      evsExploreUrl = " + evsExploreUrl);
        // Use this to extract a map
        System.out.println("YYY\t" + lexEvsUrl + "\t" + evsExploreUrl);
        assertThat(evsExploreUrl).isEqualTo(urlMapUrl);
      }
    }
  }

  /**
   * Returns the builds the gradle version.
   *
   * @return the builds the gradle version
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  private String getBuildGradleVersion() throws Exception {
    final LineIterator iter = FileUtils.lineIterator(new File("build.gradle"), "UTF-8");

    while (iter.hasNext()) {
      final String line = iter.next();
      if (line.matches("^version\\s+=\\s+\".*")) {
        return line.replaceFirst("version\\s+=\\s+", "").replaceAll("\"", "");
      }
    }

    return null;
  }
}
