package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.support.ApplicationVersion;
import gov.nih.nci.evs.api.util.ConceptUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for VersionController. */
@ExtendWith(SpringExtension.class)
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
  @BeforeEach
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
        ConceptUtils.asLinkedMap(
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
            "https://ncit.nci.nih.gov/ncitbrowser",
            baseEvsExploreUrl + "/welcome",
            "https://ncit.nci.nih.gov/ncitbrowser/",
            baseEvsExploreUrl + "/welcome",
            "https://nciterms.nci.nih.gov/ncitbrowser/",
            baseEvsExploreUrl + "/welcome",

            // Search results page
            "https://ncit.nci.nih.gov/ncitbrowser/pages/message.jsf",
            baseEvsExploreUrl + "/welcome",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/message.jsf",
            baseEvsExploreUrl + "/welcome",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf",
            baseEvsExploreUrl + "/welcome",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf",
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
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ICD-10_to_MedDRA_Mapping&version=1.1",
            baseEvsExploreUrl + "/mappings/ICD10_to_MedDRA_Mapping",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MA_to_NCIt_Mapping&version=1.1",
            baseEvsExploreUrl + "/mappings/MA_to_NCIt_Mapping",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=NCIt_to_ChEBI_Mapping&version=1.1",
            baseEvsExploreUrl + "/mappings/NCIt_to_ChEBI_Mapping",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=NCIt_to_HGNC_Mapping&version=1.1",
            baseEvsExploreUrl + "/mappings/NCIt_to_HGNC_Mapping",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=PDQ_2016_07_31_TO_NCI_2016_10E&version=1.1",
            baseEvsExploreUrl + "/mappings/PDQ_2016_07_31_TO_NCI_2016_10E",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=SNOMEDCT_US_2023_09_01_TO_ICD10CM_2023&version=1.1",
            baseEvsExploreUrl + "/mappings/SNOMEDCT_US_2023_09_01_to_ICD10CM_2023_Mappings",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=SNOMEDCT_US_2023_09_01_TO_ICD10_2016&version=1.1",
            baseEvsExploreUrl + "/mappings/SNOMEDCT_US_2023_09_01_to_ICD10_2016_Mappings",

            // Mappings bogus
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=abcdef12345&version=1.1",
            baseEvsExploreUrl + "/welcome",

            // Termform
            "https://ncitermform.nci.nih.gov/",
            baseEvsExploreUrl + "/termform",

            // Terminology bogus
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ABCDEF&version=2018_05_07",
            baseEvsExploreUrl + "/welcome",

            // NCI Thesaurus version
            "https://ncit.nci.nih.gov/ncitbrowser/pages/home.jsf",
            baseEvsExploreUrl + "/welcome",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/home.jsf?version=24.06d",
            baseEvsExploreUrl + "/welcome",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=NCI_Thesaurus&version=24.06d",
            baseEvsExploreUrl + "/welcome?terminology=ncit",
            "https://ncim.nci.nih.gov/ncimbrowser/pages/source_hierarchy.jsf?&sab=NCI",
            baseEvsExploreUrl + "/welcome?terminology=ncit",

            // NCI Thesaurus specific concept
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NCI_Thesaurus&version=24.06d&ns=ncit&code=C3224&key=1150913545&b=1&n=null",
            baseEvsExploreUrl + "/concept/ncit/C3224",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&version=24.06d&code=C3224&ns=ncit&type=synonym&key=1150913545&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/ncit/C3224",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=C3224&ns=ncit&ontology_display_name=NCI_Thesaurus&version=24.06d",
            baseEvsExploreUrl + "/hierarchy/ncit/C3224",

            // CanMED -> canmed
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=CanMED&version=May2024",
            baseEvsExploreUrl + "/welcome?terminology=canmed",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=CanMED&version=May2024",
            baseEvsExploreUrl + "/welcome?terminology=canmed",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Cancer%20Medications%20Enquiry%20Database&version=May2024",
            baseEvsExploreUrl + "/welcome?terminology=canmed",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=CanMED&version=May2024&ns=CanMED&code=HCPCS_DRUG_ANTIBODY_CONJUGATE&key=n234930328&b=1&n=null",
            baseEvsExploreUrl + "/concept/canmed/HCPCS_DRUG_ANTIBODY_CONJUGATE",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=CanMED&version=May2024&code=HCPCS_DRUG_ANTIBODY_CONJUGATE&ns=CanMED&type=synonym&key=n234930328&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/canmed/HCPCS_DRUG_ANTIBODY_CONJUGATE",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=HCPCS_DRUG_ANTIBODY_CONJUGATE&ns=CanMED&ontology_display_name=CanMED&version=May2024",
            baseEvsExploreUrl + "/hierarchy/canmed/HCPCS_DRUG_ANTIBODY_CONJUGATE",

            // ChEBI -> chebi
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ChEBI&version=v235",
            baseEvsExploreUrl + "/welcome?terminology=chebi",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ChEBI&version=v235",
            baseEvsExploreUrl + "/welcome?terminology=chebi",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=ChEBI&version=v235",
            baseEvsExploreUrl + "/welcome?terminology=chebi",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=ChEBI&version=v235&ns=chebi_ontology&code=CHEBI:23888&key=n391060948&b=1&n=null",
            baseEvsExploreUrl + "/concept/chebi/CHEBI:23888",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=ChEBI&version=v235&code=CHEBI:23888&ns=chebi_ontology&type=synonym&key=n391060948&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/chebi/CHEBI:23888",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=CHEBI:23888&ns=chebi_ontology&ontology_display_name=ChEBI&version=v235",
            baseEvsExploreUrl + "/hierarchy/chebi/CHEBI:23888",

            // CTCAE -> ctcae5
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=CTCAE",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=CTCAE",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Common%20Terminology%20Criteria%20for%20Adverse%20Events&version=4.03",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=CTCAE&version=4.03&ns=ctcae&code=E12276&key=n393628870&b=1&n=null",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=CTCAE&version=4.03&code=E12276&ns=ctcae&type=synonym&key=n393628870&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=E12276&ns=ctcae&ontology_display_name=CTCAE&version=4.03",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",

            // CTCAE_v5 -> ctcae5
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=CTCAE_v5&version=5.0",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=CTCAE_v5&version=5.0",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Common%20Terminology%20Criteria%20for%20Adverse%20Events%20version%205&version=5.0",
            baseEvsExploreUrl + "/welcome?terminology=ctcae5",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=CTCAE_v5&version=5.0&ns=ctcae5&code=C143773&key=n1532506791&b=1&n=null",
            baseEvsExploreUrl + "/concept/ctcae5/C143773",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=CTCAE_v5&version=5.0&code=C143773&ns=ctcae5&type=synonym&key=n1532506791&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/ctcae5/C143773",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=C143773&ns=ctcae5&ontology_display_name=CTCAE_v5&version=5.0",
            baseEvsExploreUrl + "/hierarchy/ctcae5/C143773",

            // DUO -> duo
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=DUO&version=2021-02-23",
            baseEvsExploreUrl + "/welcome?terminology=duo",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=DUO&version=2021-02-23",
            baseEvsExploreUrl + "/welcome?terminology=duo",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=The%20Data%20Use%20Ontology&version=2021-02-23",
            baseEvsExploreUrl + "/welcome?terminology=duo",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=DUO&version=2021-02-23&ns=obo&code=DUO_0000031&key=331637820&b=1&n=null",
            baseEvsExploreUrl + "/concept/duo/DUO_0000031",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=DUO&version=2021-02-23&code=DUO_0000031&ns=obo&type=synonym&key=331637820&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/duo/DUO_0000031",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=DUO_0000031&ns=obo&ontology_display_name=DUO&version=2021-02-23",
            baseEvsExploreUrl + "/hierarchy/duo/DUO_0000031",

            // GO -> go
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=GO&version=June2024",
            baseEvsExploreUrl + "/welcome?terminology=go",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=GO&version=June2024",
            baseEvsExploreUrl + "/welcome?terminology=go",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Gene%20Ontology&version=June2024",
            baseEvsExploreUrl + "/welcome?terminology=go",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=GO&version=June2024&ns=gene_ontology&code=GO:0005176&key=n632846066&b=1&n=null",
            baseEvsExploreUrl + "/concept/go/GO:0005176",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=GO&version=June2024&code=GO:0005176&ns=gene_ontology&type=synonym&key=n632846066&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/go/GO:0005176",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=GO:0005176&ns=gene_ontology&ontology_display_name=GO&version=June2024",
            baseEvsExploreUrl + "/hierarchy/go/GO:0005176",

            // HGNC -> hgnc
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=HGNC&version=July2024",
            baseEvsExploreUrl + "/welcome?terminology=hgnc",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=HGNC&version=July2024",
            baseEvsExploreUrl + "/welcome?terminology=hgnc",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=HUGO%20Gene%20Nomenclature%20Committee%20Ontology&version=July2024",
            baseEvsExploreUrl + "/welcome?terminology=hgnc",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=HGNC&version=July2024&ns=HGNC&code=HGNC:14574&key=n669450424&b=1&n=null",
            baseEvsExploreUrl + "/concept/hgnc/HGNC:14574",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=HGNC&version=July2024&code=HGNC:14574&ns=HGNC&type=synonym&key=n669450424&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/hgnc/HGNC:14574",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=HGNC:14574&ns=HGNC&ontology_display_name=HGNC&version=July2024",
            baseEvsExploreUrl + "/hierarchy/hgnc/HGNC:14574",

            // HL7 -> hl7v30
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=HL7&version=V3%20R2.36",
            baseEvsExploreUrl + "/welcome?terminology=hl7v30",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=HL7&version=V3%20R2.36",
            baseEvsExploreUrl + "/welcome?terminology=hl7v30",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=HL7&version=V3%20R2.36&ns=RIM_none&code=10173:M&key=n1890067595&b=1&n=null",
            baseEvsExploreUrl + "/welcome?terminology=hl7v30",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=HL7&version=V3%20R2.36&code=10173:M&ns=RIM_none&type=synonym&key=n1890067595&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/welcome?terminology=hl7v30",

            // ICD-10-CM -> icd10cm
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ICD-10-CM&version=2024",
            baseEvsExploreUrl + "/welcome?terminology=icd10cm",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ICD-10-CM&version=2024",
            baseEvsExploreUrl + "/welcome?terminology=icd10cm",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=International%20Classification%20of%20Disease,%20Tenth%20Revision,%20Clinical%20Modification&version=2024",
            baseEvsExploreUrl + "/welcome?terminology=icd10cm",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=ICD-10-CM&version=2024&ns=ICD-10-CM&code=K65.1&key=1906965616&b=1&n=null",
            baseEvsExploreUrl + "/concept/icd10cm/K65.1",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=ICD-10-CM&version=2024&code=K65.1&ns=ICD-10-CM&type=synonym&key=1906965616&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/icd10cm/K65.1",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=K65.1&ns=ICD-10-CM&ontology_display_name=ICD-10-CM&version=2024",
            baseEvsExploreUrl + "/hierarchy/icd10cm/K65.1",

            // ICD-10 -> icd10
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ICD-10&version=2016",
            baseEvsExploreUrl + "/welcome?terminology=icd10",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ICD-10&version=2016",
            baseEvsExploreUrl + "/welcome?terminology=icd10",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=ICD-10&version=2016",
            baseEvsExploreUrl + "/welcome?terminology=icd10",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=ICD-10&version=2016&ns=ICD-10&code=A06.4&key=501106432&b=1&n=null",
            baseEvsExploreUrl + "/concept/icd10/A06.4",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=ICD-10&version=2016&code=A06.4&ns=ICD-10&type=synonym&key=501106432&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/icd10/A06.4",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=A06.4&ns=ICD-10&ontology_display_name=ICD-10&version=2016",
            baseEvsExploreUrl + "/hierarchy/icd10/A06.4",

            // ICD-9-CM -> icd9cm
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ICD-9-CM&version=2014",
            baseEvsExploreUrl + "/welcome?terminology=icd9cm",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=ICD-9-CM&version=2014",
            baseEvsExploreUrl + "/welcome?terminology=icd9cm",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=International%20Classification%20of%20Diseases,%20Ninth%20Revision,%20Clinical%20Modification&version=2014",
            baseEvsExploreUrl + "/welcome?terminology=icd9cm",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=ICD-9-CM&version=2014&ns=ICD-9-CM&code=324.0&key=n1953343843&b=1&n=null",
            baseEvsExploreUrl + "/concept/icd9cm/324.0",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=ICD-9-CM&version=2014&code=324.0&ns=ICD-9-CM&type=synonym&key=n1953343843&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/icd9cm/324.0",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=324.0&ns=ICD-9-CM&ontology_display_name=ICD-9-CM&version=2014",
            baseEvsExploreUrl + "/hierarchy/icd9cm/324.0",

            // LOINC -> lnc
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=LOINC&version=2_76",
            baseEvsExploreUrl + "/welcome?terminology=lnc",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=LOINC&version=2_76",
            baseEvsExploreUrl + "/welcome?terminology=lnc",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Logical%20Observation%20Identifier%20Names%20and%20Codes&version=2_76",
            baseEvsExploreUrl + "/welcome?terminology=lnc",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=LOINC&version=2_76&ns=LOINC&code=43223-7&key=n1285584985&b=1&n=null",
            baseEvsExploreUrl + "/concept/lnc/43223-7",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=LOINC&version=2_76&code=43223-7&ns=LOINC&type=synonym&key=n1285584985&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/lnc/43223-7",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=43223-7&ns=LOINC&ontology_display_name=LOINC&version=2_76",
            baseEvsExploreUrl + "/hierarchy/lnc/43223-7",

            // MA -> ma
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MA&version=July2016",
            baseEvsExploreUrl + "/welcome?terminology=ma",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MA&version=July2016",
            baseEvsExploreUrl + "/welcome?terminology=ma",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Anatomical%20Dictionary%20for%20the%20Adult%20Mouse&version=July2016",
            baseEvsExploreUrl + "/welcome?terminology=ma",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=MA&version=July2016&ns=adult_mouse_anatomy.gxd&code=MA:0000047&key=n674725692&b=1&n=null",
            baseEvsExploreUrl + "/concept/ma/MA:0000047",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=MA&version=July2016&code=MA:0000047&ns=adult_mouse_anatomy.gxd&type=synonym&key=n674725692&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/ma/MA:0000047",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=MA:0000047&ns=adult_mouse_anatomy.gxd&ontology_display_name=MA&version=July2016",
            baseEvsExploreUrl + "/hierarchy/ma/MA:0000047",

            // MedDRA -> mdr
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MedDRA&version=26_1",
            baseEvsExploreUrl + "/welcome?terminology=mdr",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MedDRA&version=26_1",
            baseEvsExploreUrl + "/welcome?terminology=mdr",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=MedDRA%20(Medical%20Dictionary%20for%20Regulatory%20Activities%20Terminology)&version=26_1",
            baseEvsExploreUrl + "/welcome?terminology=mdr",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=MedDRA&version=26_1&ns=MedDRA&code=10024124&key=1839657810&b=1&n=null",
            baseEvsExploreUrl + "/concept/mdr/10024124",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=MedDRA&version=26_1&code=10024124&ns=MedDRA&type=synonym&key=1839657810&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/mdr/10024124",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=10024124&ns=MedDRA&ontology_display_name=MedDRA&version=26_1",
            baseEvsExploreUrl + "/hierarchy/mdr/10024124",

            // MED-RT specific version
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MED-RT&version=2018_05_07",
            baseEvsExploreUrl + "/welcome?terminology=medrt",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MED-RT&version=2018_05_07",
            baseEvsExploreUrl + "/welcome?terminology=medrt",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Medication%20Reference%20Terminology&version=2023_09_05",
            baseEvsExploreUrl + "/welcome?terminology=medrt",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=MED-RT&version=2023_09_05&ns=MED-RT&code=N0000191625&key=1741397173&b=1&n=null",
            baseEvsExploreUrl + "/concept/medrt/N0000191625",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=MED-RT&version=2023_09_05&code=N0000191625&ns=MED-RT&type=synonym&key=1741397173&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/medrt/N0000191625",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=N0000191625&ns=MED-RT&ontology_display_name=MED-RT&version=2023_09_05",
            baseEvsExploreUrl + "/hierarchy/medrt/N0000191625",

            // MGEDOntology.owl -> mged
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MGEDOntology.owl&version=1.3.1",
            baseEvsExploreUrl + "/welcome?terminology=mged",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=MGEDOntology.owl&version=1.3.1",
            baseEvsExploreUrl + "/welcome?terminology=mged",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=The%20MGED%20Ontology&version=1.3.1",
            baseEvsExploreUrl + "/welcome?terminology=mged",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=MGEDOntology.owl&version=1.3.1&ns=MGEDOntology.owl&code=MO_503&key=n1240311341&b=1&n=null",
            baseEvsExploreUrl + "/concept/mged/MO_503",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=MGEDOntology.owl&version=1.3.1&code=MO_503&ns=MGEDOntology.owl&type=synonym&key=n1240311341&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/mged/MO_503",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=MO_503&ns=MGEDOntology.owl&ontology_display_name=MGEDOntology.owl&version=1.3.1",
            baseEvsExploreUrl + "/hierarchy/mged/MO_503",

            // NDFRT -> ndfrt
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=NDFRT&version=February2018",
            baseEvsExploreUrl + "/welcome?terminology=ndfrt",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=NDFRT&version=February2018",
            baseEvsExploreUrl + "/welcome?terminology=ndfrt",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=National%20Drug%20File%20-%20Reference%20Terminology&version=February2018",
            baseEvsExploreUrl + "/welcome?terminology=ndfrt",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NDFRT&version=February2018&ns=NDFRT&code=N0000145918&key=1887366860&b=1&n=null",
            baseEvsExploreUrl + "/concept/ndfrt/N0000145918",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NDFRT&version=February2018&code=N0000145918&ns=NDFRT&type=synonym&key=1887366860&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/ndfrt/N0000145918",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=N0000145918&ns=NDFRT&ontology_display_name=NDFRT&version=February2018",
            baseEvsExploreUrl + "/hierarchy/ndfrt/N0000145918",

            // obi -> obi
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=obi&version=2022-07-11",
            baseEvsExploreUrl + "/welcome?terminology=obi",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=obi&version=2022-07-11",
            baseEvsExploreUrl + "/welcome?terminology=obi",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Ontology%20for%20Biomedical%20Investigations&version=2022-07-11",
            baseEvsExploreUrl + "/welcome?terminology=obi",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=obi&version=2022-07-11&ns=obo&code=OBI_0002785&key=348941799&b=1&n=null",
            baseEvsExploreUrl + "/concept/obi/OBI_0002785",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=obi&version=2022-07-11&code=OBI_0002785&ns=obo&type=synonym&key=348941799&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/obi/OBI_0002785",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=OBI_0002785&ns=obo&ontology_display_name=obi&version=2022-07-11",
            baseEvsExploreUrl + "/hierarchy/obi/OBI_0002785",

            // obib -> obib
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=obib&version=2021-11-12",
            baseEvsExploreUrl + "/welcome?terminology=obib",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=obib&version=2021-11-12",
            baseEvsExploreUrl + "/welcome?terminology=obib",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Ontology%20for%20Biobanking&version=2021-11-12",
            baseEvsExploreUrl + "/welcome?terminology=obib",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=obib&version=2021-11-12&ns=obo&code=NCIT_C16212&key=n534522588&b=1&n=null",
            baseEvsExploreUrl + "/concept/obib/NCIT_C16212",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=obib&version=2021-11-12&code=NCIT_C16212&ns=obo&type=synonym&key=n534522588&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/obib/NCIT_C16212",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=NCIT_C16212&ns=obo&ontology_display_name=obib&version=2021-11-12",
            baseEvsExploreUrl + "/hierarchy/obib/NCIT_C16212",

            // PDQ -> pdq
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=PDQ&version=2016_07_31",
            baseEvsExploreUrl + "/welcome?terminology=pdq",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=PDQ&version=2016_07_31",
            baseEvsExploreUrl + "/welcome?terminology=pdq",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Physician%20Data%20Query&version=2016_07_31",
            baseEvsExploreUrl + "/welcome?terminology=pdq",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=PDQ&version=2016_07_31&ns=PDQ&code=CDR0000042022&key=n707346191&b=1&n=null",
            baseEvsExploreUrl + "/concept/pdq/CDR0000042022",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=PDQ&version=2016_07_31&ns=PDQ&code=CDR0000042022&key=n707346191&b=1&n=null",
            baseEvsExploreUrl + "/concept/pdq/CDR0000042022",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=CDR0000042022&ns=PDQ&ontology_display_name=PDQ&version=2016_07_31",
            baseEvsExploreUrl + "/hierarchy/pdq/CDR0000042022",

            // RadLex -> radlex
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=RadLex&version=4_1",
            baseEvsExploreUrl + "/welcome?terminology=radlex",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=RadLex&version=4_1",
            baseEvsExploreUrl + "/welcome?terminology=radlex",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Radiology%20Lexicon&version=4_1",
            baseEvsExploreUrl + "/welcome?terminology=radlex",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=RadLex&version=4_1&ns=RadLex&code=RID10312&key=1977627707&b=1&n=null",
            baseEvsExploreUrl + "/concept/radlex/RID10312",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=RadLex&version=4_1&code=RID10312&ns=RadLex&type=synonym&key=1977627707&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/radlex/RID10312",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=RID10312&ns=RadLex&ontology_display_name=RadLex&version=4_1",
            baseEvsExploreUrl + "/hierarchy/radlex/RID10312",

            // SNOMED%20Clinical%20Terms%20US%20Edition - snomedct_us
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=SNOMED%20Clinical%20Terms%20US%20Edition&version=2023_09_01",
            baseEvsExploreUrl + "/welcome?terminology=snomedct_us",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=SNOMED%20Clinical%20Terms%20US%20Edition&version=2023_09_01",
            baseEvsExploreUrl + "/welcome?terminology=snomedct_us",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=SNOMED%20Clinical%20Terms&version=2023_09_01",
            baseEvsExploreUrl + "/welcome?terminology=snomedct_us",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=SNOMED%20Clinical%20Terms%20US%20Edition&version=2023_09_01&ns=SNOMED%20Clinical%20Terms%20US%20Edition&code=71388002&key=n1048834805&b=1&n=null",
            baseEvsExploreUrl + "/concept/snomedct_us/71388002",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=SNOMED%20Clinical%20Terms%20US%20Edition&version=2023_09_01&code=71388002&ns=SNOMED%20Clinical%20Terms%20US%20Edition&type=synonym&key=n1048834805&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/snomedct_us/71388002",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=71388002&ns=SNOMED%20Clinical%20Terms%20US%20Edition&ontology_display_name=SNOMED%20Clinical%20Terms%20US%20Edition&version=2023_09_01",
            baseEvsExploreUrl + "/hierarchy/snomedct_us/71388002",

            // UMLS_SemNet -> umlssemnet
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=UMLS_SemNet&version=3.2",
            baseEvsExploreUrl + "/welcome?terminology=umlssemnet",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=UMLS_SemNet&version=3.2",
            baseEvsExploreUrl + "/welcome?terminology=umlssemnet",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=UMLS_SemNet&version=3.2&ns=UMLS_SemNet&code=T059&key=1951480211&b=1&n=null",
            baseEvsExploreUrl + "/welcome?terminology=umlssemnet",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=UMLS_SemNet&version=3.2&ns=UMLS_SemNet&code=T059&key=1951480211&b=1&n=null",
            baseEvsExploreUrl + "/concept/umlssemnet/T059",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=UMLS_SemNet&version=3.2&code=T059&ns=UMLS_SemNet&type=synonym&key=1951480211&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/umlssemnet/T059",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=T059&ns=UMLS_SemNet&ontology_display_name=UMLS_SemNet&version=3.2",
            baseEvsExploreUrl + "/hierarchy/umlssemnet/T059",

            // Zebrafish -> zebrafish
            "https://ncit.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=Zebrafish&version=August_2019",
            baseEvsExploreUrl + "/welcome?terminology=zfa",
            "https://nciterms.nci.nih.gov/ncitbrowser/pages/vocabulary.jsf?dictionary=Zebrafish&version=August_2019",
            baseEvsExploreUrl + "/welcome?terminology=zfa",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?dictionary=Zebrafish&version=August_2019",
            baseEvsExploreUrl + "/welcome?terminology=zfa",
            "https://ncit.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=Zebrafish&version=August_2019&ns=zebrafish_anatomical_ontology&code=ZFA:0000114&key=842053645&b=1&n=null",
            baseEvsExploreUrl + "/concept/zfa/ZFA:0000114",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=Zebrafish&version=August_2019&code=ZFA:0000114&ns=zebrafish_anatomical_ontology&type=synonym&key=842053645&b=1&n=0&vse=null",
            baseEvsExploreUrl + "/concept/zfa/ZFA:0000114",
            "https://ncit.nci.nih.gov/ncitbrowser/pages/hierarchy.jsf?vih=true&code=ZFA:0000114&ns=zebrafish_anatomical_ontology&ontology_display_name=Zebrafish&version=August_2019",
            baseEvsExploreUrl + "/hierarchy/zfa/ZFA:0000114",

            // NCIm home
            "https://ncim.nci.nih.gov/ncimbrowser",
            baseEvsExploreUrl + "/welcome?terminology=ncim",
            "https://ncim.nci.nih.gov/ncimbrowser/",
            baseEvsExploreUrl + "/welcome?terminology=ncim",
            "https://ncim.nci.nih.gov/ncimbrowser/pages/home.jsf",
            baseEvsExploreUrl + "/welcome?terminology=ncim",
            "https://ncim.nci.nih.gov/ncimbrowser/pages/concept_details.jsf?dictionary=NCI%20Metathesaurus&code=C0025202&type=relationship",
            baseEvsExploreUrl + "/concept/ncim/C0025202",
            "https://ncim.nci.nih.gov/ncimbrowser/ConceptReport.jsp?dictionary=NCI%20Metathesaurus&code=C0025202",
            baseEvsExploreUrl + "/concept/ncim/C0025202"

            // end
            );

    url = baseUrl + "/rewrite";

    final Set<String> seen = new HashSet<>();
    for (final String lexEvsUrlBase : urlMap.keySet()) {
      // skip empty ones while working
      if (lexEvsUrlBase.isEmpty()) {
        continue;
      }
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
        // Ensure no duplicate urls
        if (seen.contains(lexEvsUrl)) {
          throw new Exception("Seen = " + lexEvsUrl);
        }
        seen.add(lexEvsUrl);
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
        // Use this to extract a map to put into a spreadsheet
        // System.out.println("Y Y Y\t" + lexEvsUrl + "\t" + evsExploreUrl);
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
