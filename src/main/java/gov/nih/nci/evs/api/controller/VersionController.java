package gov.nih.nci.evs.api.controller;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.support.ApplicationVersion;
import gov.nih.nci.evs.api.util.ConceptUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Version controller. */
@RestController
@Tag(name = "Application version endpoint")
public class VersionController extends BaseController {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(VersionController.class);

  /**
   * Returns the evs concept detail.
   *
   * @return the evs concept detail
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Operation(summary = "Get the application version information")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/version", produces = "application/json")
  public @ResponseBody ApplicationVersion getApplicationVersion() throws IOException {
    final ApplicationVersion homePageData = new ApplicationVersion();
    homePageData.setName("NCI EVS Rest API");
    homePageData.setDescription(
        "Endpoints to support searching, metadata, and content retrieval for EVS terminologies. "
            + "To learn more about how to interact with this api, see the  "
            + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK' "
            + "target='_blank'>Github evsrestapi-client-SDK project</a>.<br/><br/>");
    homePageData.setVersion("2.0.0.RELEASE");
    return homePageData;
  }

  /**
   * Rewrite url.
   *
   * @param url the url
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Operation(summary = "Rewrite the specified LexEVS URL to EVS Explore")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @RequestMapping(method = RequestMethod.GET, value = "/rewrite", produces = "application/json")
  public @ResponseBody String rewriteUrl(
      @RequestParam(required = true, name = "url") final String url) throws IOException {

    final Map<String, String> mapsetMap =
        ConceptUtils.asMap(
            "GO_to_NCIt_Mapping",
            "GO_to_NCIt_Mapping",
            "ICD-10_to_MedDRA_Mapping",
            "ICD10_to_MedDRA_Mapping",
            "MA_to_NCIt_Mapping",
            "MA_to_NCIt_Mapping",
            "NCIt_to_ChEBI_Mapping",
            "NCIt_to_ChEBI_Mapping",
            "NCIt_to_HGNC_Mapping",
            "NCIt_to_HGNC_Mapping",
            "PDQ_2016_07_31_TO_NCI_2016_10E",
            "PDQ_2016_07_31_TO_NCI_2016_10E",
            "SNOMEDCT_US_2023_09_01_TO_ICD10CM_2023",
            "SNOMEDCT_US_2023_09_01_to_ICD10CM_2023_Mappings",
            "SNOMEDCT_US_2023_09_01_TO_ICD10_2016",
            "SNOMEDCT_US_2023_09_01_to_ICD10_2016_Mappings");

    // Map of LexEVS terminology values to EVS Explore
    final Map<String, String> termMap =
        ConceptUtils.asMap(
            "Medication%20Reference%20Terminology",
            "medrt",
            "Medication Reference Terminology",
            "medrt",
            "Medication+Reference+Terminology",
            "medrt",
            "MED-RT",
            "medrt",
            "CanMED",
            "canmed",
            "Cancer%20Medications%20Enquiry%20Database",
            "canmed",
            "Cancer Medications Enquiry Database",
            "canmed",
            "Cancer+Medications+Enquiry+Database",
            "canmed",
            "ChEBI",
            "chebi",
            "chebi_ontology",
            "chebi",
            "Common%20Terminology%20Criteria%20for%20Adverse%20Events",
            "ctcae5",
            "Common Terminology Criteria for Adverse Events",
            "ctcae5",
            "Common+Terminology+Criteria+for+Adverse+Events",
            "ctcae5",
            "Common%20Terminology%20Criteria%20for%20Adverse%20Events%20version%205",
            "ctcae5",
            "Common Terminology Criteria for Adverse Events version 5",
            "ctcae5",
            "Common+Terminology+Criteria+for+Adverse+Events+version+5",
            "ctcae5",
            "CTCAE",
            "ctcae5",
            "CTCAE_v5",
            "ctcae5",
            "The%20Data%20Use%20Ontology",
            "duo",
            "The Data Use Ontology",
            "duo",
            "The+Data+Use+Ontology",
            "duo",
            "DUO",
            "duo",
            "Gene%20Ontology",
            "go",
            "Gene Ontology",
            "go",
            "Gene+Ontology",
            "go",
            "GO",
            "go",
            "HUGO%20Gene%20Nomenclature%20Committee%20Ontology",
            "hgnc",
            "HUGO Gene Nomenclature Committee Ontology",
            "hgnc",
            "HUGO+Gene+Nomenclature+Committee+Ontology",
            "hgnc",
            "HGNC",
            "hgnc",
            "HL7",
            "hl7v30",
            "International%20Classification%20of%20Disease,%20Tenth%20Revision,%20Clinical%20Modification",
            "icd10cm",
            "International Classification of Disease, Tenth Revision, Clinical Modification",
            "icd10cm",
            "International+Classification+of+Disease,+Tenth+Revision,+Clinical+Modification",
            "icd10cm",
            "ICD-10-CM",
            "icd10cm",
            "ICD-10",
            "icd10",
            "International%20Classification%20of%20Diseases,%20Ninth%20Revision,%20Clinical%20Modification",
            "icd9cm",
            "International Classification of Diseases, Ninth Revision, Clinical Modification",
            "icd9cm",
            "International+Classification+of+Diseases,+Ninth+Revision,+Clinical+Modification",
            "icd9cm",
            "ICD-9-CM",
            "icd9cm",
            "Logical%20Observation%20Identifier%20Names%20and%20Codes",
            "lnc",
            "Logical Observation Identifier Names and Codes",
            "lnc",
            "Logical+Observation+Identifier+Names+and+Codes",
            "lnc",
            "LOINC",
            "lnc",
            "NCI_Thesaurus",
            "ncit",
            "NCI",
            "ncit",
            "ncit",
            "ncit",
            "Anatomical%20Dictionary%20for%20the%20Adult%20Mouse",
            "ma",
            "Anatomical Dictionary for the Adult Mouse",
            "ma",
            "Anatomical+Dictionary+for+the+Adult+Mouse",
            "ma",
            "MA",
            "ma",
            "MedDRA%20(Medical%20Dictionary%20for%20Regulatory%20Activities%20Terminology)",
            "mdr",
            "MedDRA (Medical Dictionary for Regulatory Activities Terminology)",
            "mdr",
            "MedDRA+(Medical+Dictionary+for+Regulatory+Activities+Terminology)",
            "mdr",
            "MedDRA",
            "mdr",
            "The%20MGED%20Ontology",
            "mged",
            "The MGED Ontology",
            "mged",
            "The+MGED+Ontology",
            "mged",
            "MGEDOntology.owl",
            "mged",
            "National%20Drug%20File%20-%20Reference%20Terminology",
            "ndfrt",
            "National Drug File - Reference Terminology",
            "ndfrt",
            "National+Drug+File+-+Reference+Terminology",
            "ndfrt",
            "NDFRT",
            "ndfrt",
            "Ontology%20for%20Biomedical%20Investigations",
            "obi",
            "Ontology for Biomedical Investigations",
            "obi",
            "Ontology+for+Biomedical+Investigations",
            "obi",
            "obi",
            "obi",
            "Ontology%20for%20Biobanking",
            "obib",
            "Ontology for Biobanking",
            "obib",
            "Ontology+for+Biobanking",
            "obib",
            "obib",
            "obib",
            "Physician%20Data%20Query",
            "pdq",
            "Physician Data Query",
            "pdq",
            "Physician+Data+Query",
            "pdq",
            "PDQ",
            "pdq",
            "Radiology%20Lexicon",
            "radlex",
            "Radiology Lexicon",
            "radlex",
            "Radiology+Lexicon",
            "radlex",
            "RadLex",
            "radlex",
            "NCI%20Metathesaurus",
            "ncim",
            "NCI Metathesaurus",
            "ncim",
            "NCI+Metathesaurus",
            "ncim",
            "SNOMED%20Clinical%20Terms",
            "snomedct_us",
            "SNOMED Clinical Terms",
            "snomedct_us",
            "SNOMED+Clinical+Terms",
            "snomedct_us",
            "SNOMED%20Clinical%20Terms%20US%20Edition",
            "snomedct_us",
            "SNOMED Clinical Terms US Edition",
            "snomedct_us",
            "SNOMED+Clinical+Terms+US+Edition",
            "snomedct_us",
            "SNOMED Clinical Terms US Edition - snomedct_us",
            "snomedct_us",
            "SNOMED+Clinical+Terms+US+Edition+-+snomedct_us",
            "snomedct_us",
            "SNOMED%20Clinical%20Terms%20US%20Edition%20-%20snomedct_us",
            "snomedct_us",
            "UMLS_SemNet",
            "umlssemnet",
            "Zebrafish",
            "zfa");

    // Extract "tier"
    String tier = "";
    String baseEvsExploreUrl = "https://evsexplore.semantics.cancer.gov/evsexplore";
    // nciterms.nci.nih.gov
    if (url.matches("^https?://nciterms-?[^\\.]*.nci.nih.gov.*")) {
      // Extract tier (so we know prod or stage)
      tier = url.replaceFirst("^https?://nciterms(-?[^\\.]*).nci.nih.gov.*", "$1");
      baseEvsExploreUrl = "https://evsexplore" + tier + ".semantics.cancer.gov/evsexplore";
    }
    // ncitermform
    else if (url.matches("^https?://ncitermform-?[^\\.]*.nci.nih.gov.*")) {
      // Extract tier (so we know prod or stage)
      tier = url.replaceFirst("^https?://ncitermform(-?[^\\.]*).nci.nih.gov.*", "$1");
      baseEvsExploreUrl = "https://evsexplore" + tier + ".semantics.cancer.gov/evsexplore";
    }
    // ncit.nci.nih.gov
    else if (url.matches("^https?://ncit-?[^\\.]*.nci.nih.gov.*")) {
      // Extract tier (so we know prod or stage)
      tier = url.replaceFirst("^https?://ncit(-?[^\\.]*).nci.nih.gov.*", "$1");
      baseEvsExploreUrl = "https://evsexplore" + tier + ".semantics.cancer.gov/evsexplore";
    }
    // ncim.nci.nih.gov
    else if (url.matches("^https?://ncim-?[^\\.]*.nci.nih.gov.*")) {
      // Extract tier (so we know prod or stage)
      tier = url.replaceFirst("^https?://ncim(-?[^\\.]*).nci.nih.gov.*", "$1");
      baseEvsExploreUrl = "https://evsexplore" + tier + ".semantics.cancer.gov/evsexplore";
    }
    // http://nciws-p1086-c.nci.nih.gov:8081/
    else if (url.matches("^http://nciws-p1086-c.nci.nih.gov:8081.*")) {
      tier = "";
      baseEvsExploreUrl = "https://evsexplore.semantics.cancer.gov/evsexplore";
    }

    final String topUrl = url.replaceFirst("(https?://[^/]+)/?.*", "$1");
    final String pathUrl = url.replaceFirst("(https?://[^/]+)(/?.*)", "$2");

    // Blank path
    if (pathUrl.isEmpty() || pathUrl.equals("/")) {
      // Reroute based on top url
      if (topUrl.startsWith("https://ncitermform") && topUrl.endsWith("nci.nih.gov")) {
        return baseEvsExploreUrl + "/termform";
      } else if (topUrl.startsWith("https://ncit") && topUrl.endsWith("nci.nih.gov")) {
        return baseEvsExploreUrl + "/welcome?terminology=multi";
      } else if (topUrl.startsWith("https://ncim") && topUrl.endsWith("nci.nih.gov")) {
        return baseEvsExploreUrl + "/welcome?terminology=ncim";
      } else if (topUrl.startsWith("http://nciws-p1086-c")) {
        return baseEvsExploreUrl + "/welcome?terminology=multi";
      } else if (topUrl.startsWith("http://ncitermform") && topUrl.endsWith("nci.nih.gov")) {
        return baseEvsExploreUrl + "/termform";
      } else if (topUrl.startsWith("http://ncit") && topUrl.endsWith("nci.nih.gov")) {
        return baseEvsExploreUrl + "/welcome?terminology=multi";
      } else if (topUrl.startsWith("http://ncim") && topUrl.endsWith("nci.nih.gov")) {
        return baseEvsExploreUrl + "/welcome?terminology=ncim";
      }
    }

    // /ncitbrowser bath
    if (pathUrl.equals("/ncitbrowser") || pathUrl.equals("/ncitbrowser/")) {
      return baseEvsExploreUrl + "/welcome";
    }
    // /ncimbrowser bath
    if (pathUrl.equals("/ncimbrowser")
        || pathUrl.equals("/ncimbrowser/")
        || pathUrl.startsWith("/ncimbrowser/pages/home.jsf")) {
      return baseEvsExploreUrl + "/welcome?terminology=ncim";
    }

    // Terminology Path
    else if (pathUrl.matches("^/ncitbrowser/pages/vocabulary.jsf\\?dictionary=([^&]*).*")) {
      final String terminology =
          pathUrl.replaceFirst("^/ncitbrowser/pages/vocabulary.jsf\\?dictionary=([^&]*).*", "$1");
      if (termMap.containsKey(terminology)) {
        return baseEvsExploreUrl + "/welcome?terminology=" + termMap.get(terminology);
      } else if (mapsetMap.containsKey(terminology)) {
        return baseEvsExploreUrl + "/mappings/" + mapsetMap.get(terminology);
      }
      return baseEvsExploreUrl + "/welcome";
    }

    // Concept details path
    else if (pathUrl.matches("^/nci[tm]browser/ConceptReport.jsp.*")
        || pathUrl.matches("^/nci[tm]browser/pages/concept_details.jsf.*")) {
      // e.g. /ncitbrowser/ConceptReport.jsp?dictionary=MED-RT&
      // version=2023_09_05&ns=MED-RT&code=N0000191625&key=1741397173&b=1&n=null"
      // e.g. /ncitbrowser/pages/concept_details.jsf?dictionary=MED-RT&
      // version=2023_09_05&code=N0000191625&ns=MED-RT&type=synonym&key=1741397173&b=1&n=0&vse=null

      final String terminology = pathUrl.replaceFirst("^.*dictionary=([^&]*).*", "$1");
      // This one not in EVSRESTAPI
      if (terminology.toUpperCase().equals("CTCAE")) {
        return baseEvsExploreUrl + "/welcome?terminology=ctcae5";
      }
      if (termMap.containsKey(terminology)) {
        final String code = pathUrl.replaceFirst("^.*code=([^&]*).*", "$1");
        // codes for hl7v3 don't map across
        if (termMap.get(terminology).equals("hl7v30")) {
          return baseEvsExploreUrl + "/welcome?terminology=hl7v30";
        } else {
          return baseEvsExploreUrl + "/concept/" + termMap.get(terminology) + "/" + code;
        }
      }
      return baseEvsExploreUrl + "/welcome";
    }

    // NCIM source Hierarchy
    else if (pathUrl.startsWith("/ncimbrowser/pages/source_hierarchy.jsf")) {
      final String terminology = pathUrl.replaceFirst("^.*sab=([^&]*).*", "$1");
      if (termMap.containsKey(terminology)) {
        return baseEvsExploreUrl + "/welcome?terminology=" + termMap.get(terminology);
      } else {
        return baseEvsExploreUrl + "/welcome?terminology=" + terminology.toLowerCase();
      }
    }

    // Hierarchy details path
    else if (pathUrl.matches("^/ncitbrowser/pages/hierarchy.jsf.*")) {
      // e.g. /ncitbrowser/pages/hierarchy.jsf?vih=true&code=N0000191625&ns=MED-RT&
      // ontology_display_name=MED-RT&version=2023_09_05",
      final String terminology =
          pathUrl.contains("dictionary=")
              ? pathUrl.replaceFirst("^.*dictionary=([^&]*).*", "$1")
              : pathUrl.replaceFirst("^.*ontology_display_name=([^&]*).*", "$1");
      // This one not in EVSRESTAPI
      if (terminology.toUpperCase().equals("CTCAE")) {
        return baseEvsExploreUrl + "/welcome?terminology=ctcae5";
      }
      if (termMap.containsKey(terminology)) {
        if (pathUrl.contains("code=")) {
          final String code = pathUrl.replaceFirst("^.*code=([^&]*).*", "$1");
          return baseEvsExploreUrl + "/hierarchy/" + termMap.get(terminology) + "/" + code;
        } else {
          return baseEvsExploreUrl + "/welcome?terminology=" + termMap.get(terminology);
        }
      }
      return baseEvsExploreUrl + "/welcome";
    }
    //            "
    //    baseEvsExploreUrl + "/concept/medrt/N0000191625"

    // Subsets path
    else if (pathUrl.startsWith("/ncitbrowser/ajax?action=create_src_vs_tree")
        || pathUrl.startsWith("/ncitbrowser/pages/subset.jsf")) {

      if (pathUrl.contains("vsd_uri=")) {
        // e.g.
        // /ncitbrowser/ajax?action=create_src_vs_tree&vsd_uri=http://evs.nci.nih.gov/valueset/CTRP/C116977",
        final String code = pathUrl.replaceFirst(".*vsd_uri=.*/(C\\d+)", "$1");
        return baseEvsExploreUrl + "/subset/ncit/" + code;
      }

      return baseEvsExploreUrl + "/subsets/ncit";
    }

    // Mappings path
    else if (pathUrl.matches("/ncitbrowser/pages/mapping_search.jsf\\?.*nav_type=mappings.*")) {
      return baseEvsExploreUrl + "/mappings";
    }

    // Start path
    else if (pathUrl.matches("/ncitbrowser/start.jsf")) {
      return baseEvsExploreUrl + "/welcome?terminology=multi";
    } else if (pathUrl.startsWith("/ncitbrowser/home.jsf")) {
      return baseEvsExploreUrl + "/welcome";
    }

    // Otherwise, return default
    // also /ncitbrowser/pages/message.jsf
    return baseEvsExploreUrl + "/welcome";
  }
}
