package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.dnault.xmlpatch.internal.Log;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.support.ApplicationVersion;
import gov.nih.nci.evs.api.util.ConceptUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Version controller. */
@RestController
@Tag(name = "Application version endpoint")
public class VersionController extends BaseController {

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
            "ICD-10_to_MedDRA_Mapping",
            "MA_to_NCIt_Mapping",
            "MA_to_NCIt_Mapping",
            "NCIt_to_ChEBI_Mapping",
            "NCIt_to_ChEBI_Mapping",
            "NCIt_to_HGNC_Mapping",
            "NCIt_to_HGNC_Mapping",
            "PDQ_2016_07_31_TO_NCI_2016_10E",
            "PDQ_2016_07_31_TO_NCI_2016_10E",
            "SNOMEDCT_US_2023_09_01_TO_ICD10CM_2023",
            "SNOMEDCT_US_2023_09_01_TO_ICD10CM_2023",
            "SNOMEDCT_US_2023_09_01_TO_ICD10_2016",
            "SNOMEDCT_US_2023_09_01_TO_ICD10_2016");

    // Map of LexEVS terminology values to EVS Explore
    final Map<String, String> termMap =
        ConceptUtils.asMap(
            "MED-RT",
            "medrt",
            "CanMED",
            "canmed",
            "ChEBI",
            "chebi",
            "CTCAE",
            "ctcae5",
            "CTCAE_v5",
            "ctcae5",
            "DUO",
            "duo",
            "GO",
            "go",
            "HGNC",
            "hgnc",
            "HL7",
            "hl7v30",
            "ICD-10-CM",
            "icd10cm",
            "ICD-10",
            "icd10",
            "ICD-9-CM",
            "icd9cm",
            "LOINC",
            "lnc",
            "MA",
            "ma",
            "MedDRA",
            "mdr",
            "MGEDOntology.owl",
            "mged",
            "NDFRT",
            "ndfrt",
            "obi",
            "obi",
            "obib",
            "obib",
            "PDQ",
            "pdq",
            "RadLex",
            "radlex",
            "SNOMED Clinical Terms US Edition - snomedct_us",
            "snomedct_us",
            "SNOMED+Clinical+Terms+US+Edition+-+snomedct_us",
            "snomedct_us",
            "SNOMED%20Clinical%20Terms%20US%20Edition%20-%20snomedct_us",
            "snomedct_us",
            "UMLS_SemNet",
            "umlssemnet",
            "Zebrafish",
            "zebrafish");

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
    Log.info("XXX topUrl = " + topUrl);
    Log.info("XXX pathUrl = " + pathUrl);

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
    if (pathUrl.equals("/ncitbrowser")) {
      return baseEvsExploreUrl + "/welcome";
    }

    // Terminology Path
    else if (pathUrl.matches("^/ncitbrowser/pages/vocabulary.jsf\\?dictionary=(.*)&version=.*")) {
      final String terminology =
          pathUrl.replaceFirst(
              "^/ncitbrowser/pages/vocabulary.jsf\\?dictionary=(.*)&version=.*", "$1");
      if (termMap.containsKey(terminology)) {
        return baseEvsExploreUrl + "/welcome?terminology=" + termMap.get(terminology);
      } else if (mapsetMap.containsKey(terminology)) {
        return baseEvsExploreUrl + "/mappings/=" + mapsetMap.get(terminology);
      }
      return baseEvsExploreUrl + "/welcome";
    }

    // Concept details path
    else if (pathUrl.matches("^/ncitbrowser/pages/ConceptReport.jsp.*")
        || pathUrl.matches("^/ncitbrowser/pages/concept_details.jsf.*")) {
      // e.g. /ncitbrowser/ConceptReport.jsp?dictionary=MED-RT&
      // version=2023_09_05&ns=MED-RT&code=N0000191625&key=1741397173&b=1&n=null"
      // e.g. /ncitbrowser/pages/concept_details.jsf?dictionary=MED-RT&
      // version=2023_09_05&code=N0000191625&ns=MED-RT&type=synonym&key=1741397173&b=1&n=0&vse=null

      final String terminology = pathUrl.replaceFirst("^.*dictionary=([^&]*).*", "$1");
      if (termMap.containsKey(terminology)) {
        final String code = pathUrl.replaceFirst("^.*code=([^&]*).*", "$1");
        return baseEvsExploreUrl + "/concept/" + terminology + "/" + code;
      }
      return baseEvsExploreUrl + "/welcome";
    }

    // Hierarchy details path
    else if (pathUrl.matches("^/ncitbrowser/pages/hierarchy.jsf.*")) {
      // e.g. /ncitbrowser/pages/hierarchy.jsf?vih=true&code=N0000191625&ns=MED-RT&
      // ontology_display_name=MED-RT&version=2023_09_05",
      final String terminology = pathUrl.replaceFirst("^.*ns=([^&]*).*", "$1");
      if (termMap.containsKey(terminology)) {
        final String code = pathUrl.replaceFirst("^.*code=([^&]*).*", "$1");
        return baseEvsExploreUrl + "/hierarchy/" + terminology + "/" + code;
      }
      return baseEvsExploreUrl + "/welcome";
    }
    //            "
    //    baseEvsExploreUrl + "/concept/medrt/N0000191625"

    // Subsets path
    else if (pathUrl.startsWith("/ncitbrowser/ajax?action=create_src_vs_tree")) {

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
    }

    // Otherwise, return default
    return baseEvsExploreUrl + "/welcome";
  }
}
