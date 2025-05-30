package gov.nih.nci.evs.api.controller;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.StatisticsEntry;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Controller for /metadata endpoints. */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Metadata endpoints")
public class MetadataController extends BaseController {

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(MetadataController.class);

  /** The metadata service. */
  @Autowired MetadataService metadataService;

  /** The opensearch service. */
  @Autowired OpensearchQueryService osQueryService;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /**
   * Returns the terminologies.
   *
   * @param latest the latest
   * @param tag the tag
   * @param terminology the terminology
   * @return the terminologies
   * @throws Exception the exception
   */
  @Operation(summary = "Get all available terminologies")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "latest",
        description = "Return terminologies with matching <i>latest</i> value. e.g. true or false",
        required = false,
        schema = @Schema(implementation = Boolean.class),
        example = "true"),
    @Parameter(
        name = "tag",
        description =
            "Return terminologies with matching tag. e.g. 'monthly' or 'weekly' for <i>ncit</i>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "terminology",
        description =
            "Return entries with matching terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/metadata/terminologies", produces = "application/json")
  public @ResponseBody List<Terminology> getTerminologies(
      @RequestParam(required = false, name = "latest") final Optional<Boolean> latest,
      @RequestParam(required = false, name = "tag") final Optional<String> tag,
      @RequestParam(required = false, name = "terminology") final Optional<String> terminology)
      throws Exception {
    List<String> tagList = Arrays.asList("monthly", "weekly");
    try {
      List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);

      if (latest.isPresent()) {
        terms =
            terms.stream()
                .filter(f -> f.getLatest() != null && f.getLatest().equals(latest.get()))
                .collect(Collectors.toList());
      }

      if (tag.isPresent() && tagList.contains(tag.get())) {
        terms =
            terms.stream()
                .filter(f -> "true".equals(f.getTags().get(tag.get())))
                .collect(Collectors.toList());
      }

      if (terminology.isPresent()) {
        terms =
            terms.stream()
                .filter(f -> f.getTerminology().equals(terminology.get()))
                .collect(Collectors.toList());
      }

      for (final Terminology term : terms) {
        // For internal use
        term.setSource(null);
        term.setIndexName(null);
        term.setObjectIndexName(null);
        final TerminologyMetadata meta = term.getMetadata();
        // Some terminologies may not have metadata
        if (meta != null) {

          // Various other metadata things (schema=hidden)
          meta.setWelcomeText(null);
          meta.setSources(null);
          meta.setDefinitionSourceSet(null);
          meta.setSynonymSourceSet(null);
          meta.setSubsetPrefix(null);
          meta.setSparqlPrefix(null);
          meta.setSourcesToRemove(null);
          meta.setSubsetMember(null);
          meta.setUnpublished(null);
          meta.setMonthlyDb(null);
          meta.setLicenseCheck(null);
          meta.setMapsets(null);
          meta.setRelationshipToTarget(null);
          meta.setCode(null);
          meta.setConceptStatus(null);
          meta.setPreferredName(null);
          meta.setSynonym(null);
          meta.setSynonymTermType(null);
          meta.setSynonymSource(null);
          meta.setSynonymCode(null);
          meta.setSynonymSubSource(null);
          meta.setDefinitionSource(null);
          meta.setDefinition(null);
          meta.setFhirPublisher(null);
          meta.setFhirUri(null);
          meta.setMapRelation(null);
          meta.setMap(null);
          meta.setMapTarget(null);
          meta.setMapTargetTermType(null);
          meta.setMapTargetTermGroup(null);
          meta.setMapTargetTerminology(null);
          meta.setMapTargetTerminologyVersion(null);
          meta.setTermTypes(null);
          meta.setPreferredTermTypes(null);
          meta.setSubset(null);
        }
      }

      return terms;
    } catch (Exception e) {
      handleException(e, null);
      return null;
    }
  }

  /**
   * Returns the metadata.
   *
   * @param terminology the terminology
   * @return the metadata
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get some metadata (associations, properties, qualifiers, roles, term types, sources,"
              + " definition types, synonym types) for the terminology overview tab in EVS-Explore")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/metadata/{terminology}", produces = "application/json")
  public @ResponseBody Map<String, List<Concept>> getOverviewMetadata(
      @PathVariable(value = "terminology") final String terminology) throws Exception {
    Map<String, List<Concept>> metadata = new HashMap<>();
    try {
      metadata.put(
          "associations",
          metadataService.getAssociations(terminology, Optional.empty(), Optional.empty()));
      metadata.put(
          "properties",
          metadataService.getProperties(terminology, Optional.empty(), Optional.empty()));
      metadata.put(
          "qualifiers",
          metadataService.getQualifiers(terminology, Optional.empty(), Optional.empty()));
      metadata.put(
          "roles", metadataService.getRoles(terminology, Optional.empty(), Optional.empty()));
      metadata.put(
          "termTypes",
          metadataService.getTermTypes(terminology).stream()
              .map(Concept::new)
              .collect(Collectors.toList()));
      metadata.put(
          "sources",
          metadataService.getSynonymSources(terminology).stream()
              .map(Concept::new)
              .collect(Collectors.toList()));
      metadata.put(
          "definitionTypes",
          metadataService.getDefinitionSources(terminology).stream()
              .map(Concept::new)
              .collect(Collectors.toList()));
      metadata.put(
          "synonymTypes",
          metadataService.getSynonymTypes(terminology, Optional.empty(), Optional.empty()));
      return metadata;
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the associations.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the associations
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get all associations (or those specified by list parameter) for the specified"
              + " terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "list",
        description =
            "List of codes or labels to return associations for (or leave blank for all). If"
                + " invalid values are passed, the result will simply include no entries for those"
                + " invalid values.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/metadata/{terminology}/associations", produces = "application/json")
  public @ResponseBody List<Concept> getAssociations(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "list") final Optional<String> list)
      throws Exception {
    try {
      return metadataService.getAssociations(terminology, include, list);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the association.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the association
   * @throws Exception the exception
   */
  @Operation(summary = "Get the association for the specified terminology and code/name")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "codeOrName",
        description =
            "Association code (or name), e.g. "
                + "<ul><li>'A10' or 'Has_CDRH_Parent' for <i>ncit</i></li>"
                + "<li>'RB' or 'has a broader relationship' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "summary")
  })
  @RecordMetric
  @GetMapping(
      value = "/metadata/{terminology}/association/{codeOrName}",
      produces = "application/json")
  public @ResponseBody Concept getAssociation(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "codeOrName") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {
    try {

      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Association " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getAssociation(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Association " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the roles.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the roles
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get all roles (or those specified by list parameter) for the specified terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @GetMapping(value = "/metadata/{terminology}/roles", produces = "application/json")
  @Parameters({
    @Parameter(
        name = "terminology",
        description = "Terminology, e.g. 'ncit'.  This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "list",
        description =
            "List of codes or labels to return roles for (or leave blank for all).  If invalid"
                + " values are passed, the result will simply include no entries for those invalid"
                + " values.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  public @ResponseBody List<Concept> getRoles(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "list") final Optional<String> list)
      throws Exception {
    try {
      return metadataService.getRoles(terminology, include, list);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the role.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the role
   * @throws Exception the exception
   */
  @Operation(summary = "Get the role for the specified terminology and code/name")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description = "Terminology, e.g. 'ncit'",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "codeOrName",
        description =
            "Role code (or name), e.g. "
                + "'R123' or 'Chemotherapy_Regimen_Has_Component' for <i>ncit</i>. "
                + "This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "summary")
  })
  @RecordMetric
  @GetMapping(value = "/metadata/{terminology}/role/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getRole(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "codeOrName") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {
    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getRole(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the properties.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the properties
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get all properties (or those specified by list parameter) for the specified terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @GetMapping(value = "/metadata/{terminology}/properties", produces = "application/json")
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "list",
        description =
            "List of codes or labels to return properties for (or leave blank for all).  If invalid"
                + " values are passed, the result will simply include no entries for those invalid"
                + " values.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  public @ResponseBody List<Concept> getProperties(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "list") final Optional<String> list)
      throws Exception {

    try {
      return metadataService.getProperties(terminology, include, list);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the qualifiers.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the qualifiers
   * @throws Exception the exception
   */
  @Operation(
      summary = "Get all qualifiers (properties on properties) for the specified terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @GetMapping(value = "/metadata/{terminology}/qualifiers", produces = "application/json")
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "list",
        description = "List of codes or labels to return qualifiers for (or leave blank for all)",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  public @ResponseBody List<Concept> getQualifiers(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "list") final Optional<String> list)
      throws Exception {
    try {
      return metadataService.getQualifiers(terminology, include, list);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the qualifier.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the qualifier
   * @throws Exception the exception
   */
  @Operation(summary = "Get the qualifier for the specified terminology and code/name")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "codeOrName",
        description =
            "Qualifier code (or name), e.g."
                + "<ul><li>'P390' or 'go-source' for <i>ncit</i></li>"
                + "<li>'RG' or 'Relationship group' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "summary")
  })
  @RecordMetric
  @GetMapping(
      value = "/metadata/{terminology}/qualifier/{codeOrName}",
      produces = "application/json")
  public @ResponseBody Concept getQualifier(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "codeOrName") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {

    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getQualifier(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the term types.
   *
   * @param terminology the terminology
   * @return the term types
   * @throws Exception the exception
   */
  @Operation(summary = "Get all term types for the specified terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @GetMapping(value = "/metadata/{terminology}/termTypes", produces = "application/json")
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit")
  })
  @RecordMetric
  public @ResponseBody List<ConceptMinimal> getTermTypes(
      @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      return metadataService.getTermTypes(terminology);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the welcome text.
   *
   * @param terminology the terminology
   * @return the welcome text
   * @throws Exception the exception
   */
  @Operation(summary = "Get welcome text for the specified terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @GetMapping(value = "/metadata/{terminology}/welcomeText", produces = "text/html")
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit")
  })
  @RecordMetric
  public @ResponseBody String getWelcomeText(
      @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      return metadataService.getWelcomeText(terminology);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the property.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the property
   * @throws Exception the exception
   */
  @Operation(summary = "Get the property for the specified terminology and code/name")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "codeOrName",
        description =
            "Property code (or name), e.g. "
                + "<ul><li>'P216' or 'BioCarta_ID' for <i>ncit</i></li>"
                + "<li>'BioCarta_ID' or ''BioCarta ID' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "summary")
  })
  @RecordMetric
  @GetMapping(
      value = "/metadata/{terminology}/property/{codeOrName}",
      produces = "application/json")
  public @ResponseBody Concept getProperty(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "codeOrName") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {

    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getProperty(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the concept statuses.
   *
   * @param terminology the terminology
   * @return the concept statuses
   * @throws Exception the exception
   */
  @Operation(summary = "Get all concept status values for the specified terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description = "Terminology, e.g. 'ncit'. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit")
  })
  @RecordMetric
  @GetMapping(value = "/metadata/{terminology}/conceptStatuses", produces = "application/json")
  public @ResponseBody List<String> getConceptStatuses(
      @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      Optional<List<String>> result = metadataService.getConceptStatuses(terminology);
      if (!result.isPresent()) {
        // this should never happen
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }

      return result.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the definition sources.
   *
   * @param terminology the terminology
   * @return the definition sources
   * @throws Exception the exception
   */
  @Operation(summary = "Get all definition sources for the specified terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit")
  })
  @RecordMetric
  @GetMapping(value = "/metadata/{terminology}/definitionSources", produces = "application/json")
  public @ResponseBody List<ConceptMinimal> getDefinitionSources(
      @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      return metadataService.getDefinitionSources(terminology);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the synonym sources.
   *
   * @param terminology the terminology
   * @return the synonym sources
   * @throws Exception the exception
   */
  @Operation(summary = "Get all synonym sources for the specified terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit")
  })
  @RecordMetric
  @GetMapping(value = "/metadata/{terminology}/synonymSources", produces = "application/json")
  public @ResponseBody List<ConceptMinimal> getSynonymSources(
      @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      return metadataService.getSynonymSources(terminology);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the axiom qualifiers list.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the axiom qualifiers list
   * @throws Exception the exception
   */
  @Operation(summary = "Get qualifier values for the specified terminology and code/name")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "codeOrName",
        description =
            "Qualifier code (or name), e.g."
                + "<ul><li>'P390' or 'go-source' for <i>ncit</i></li>"
                + "<li>'RG' or 'Relationship group' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(
      value = "/metadata/{terminology}/qualifier/{codeOrName}/values",
      produces = "application/json")
  public @ResponseBody List<String> getQualifierValues(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "codeOrName") final String code)
      throws Exception {
    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");
      }

      Optional<List<String>> result = metadataService.getQualifierValues(terminology, code);
      if (!result.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");

      return result.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the synonym types.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the synonym types
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get all synonym types (or those specified by list parameter) for the specified"
              + " terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @GetMapping(value = "/metadata/{terminology}/synonymTypes", produces = "application/json")
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "list",
        description =
            "List of codes or labels to return synonym types for (or leave blank for all).  If"
                + " invalid values are passed, the result will simply include no entries for those"
                + " invalid values.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  public @ResponseBody List<Concept> getSynonymTypes(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "list") final Optional<String> list)
      throws Exception {

    try {
      return metadataService.getSynonymTypes(terminology, include, list);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the synonym type.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the synonym type
   * @throws Exception the exception
   */
  @Operation(summary = "Get the synonym type for the specified terminology and code/name")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "codeOrName",
        description =
            "Synonym type code (or name), e.g."
                + "<ul><li>'P90' or 'FULL_SYN' for <i>ncit</i></li>"
                + "<li>'Preferred_Name' or 'Preferred name' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "summary")
  })
  @RecordMetric
  @GetMapping(
      value = "/metadata/{terminology}/synonymType/{codeOrName}",
      produces = "application/json")
  public @ResponseBody Concept getSynonymType(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "codeOrName") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {

    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Synonym type " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getSynonymType(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Synonym type " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the definition types.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the definition types
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get all definition types (or those specified by list parameter) for the specified"
              + " terminology")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @GetMapping(value = "/metadata/{terminology}/definitionTypes", produces = "application/json")
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "list",
        description =
            "List of codes or labels to return definition types for (or leave blank for all).  If"
                + " invalid values are passed, the result will simply include no entries for those"
                + " invalid values.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  public @ResponseBody List<Concept> getDefinitionTypes(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "list") final Optional<String> list)
      throws Exception {

    try {
      return metadataService.getDefinitionTypes(terminology, include, list);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the definition type.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the definition type
   * @throws Exception the exception
   */
  @Operation(summary = "Get the definition type for the specified terminology and code/name.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Terminology, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "codeOrName",
        description =
            "Definition type code (or name), e.g."
                + "<ul><li>'P325' or 'DEFINITION' for <i>ncit</i></li>"
                + "<li>'DEFINITION' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "summary")
  })
  @RecordMetric
  @GetMapping(
      value = "/metadata/{terminology}/definitionType/{codeOrName}",
      produces = "application/json")
  public @ResponseBody Concept getDefinitionType(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "codeOrName") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {

    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Definition type " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getDefinitionType(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Defininition type " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the subsets.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the properties
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get all subsets (or those specified by list parameter) for the specified terminology.",
      description =
          "This endpoint will be deprecated in v2 in favor of top level subset endpoints.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @GetMapping(value = "/metadata/{terminology}/subsets", produces = "application/json")
  @Parameters({
    @Parameter(
        name = "terminology",
        description = "Terminology, e.g. 'ncit'.  This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "list",
        description =
            "List of codes or labels to return subsets for (or leave blank for all).  If invalid"
                + " values are passed, the result will simply include no entries for those invalid"
                + " values.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  public @ResponseBody List<Concept> getSubsets(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "list") final Optional<String> list)
      throws Exception {
    try {
      return metadataService.getSubsets(terminology, include, list);
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the subset.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the subset
   * @throws Exception the exception
   */
  @Operation(
      summary = "Get the subset for the specified terminology and code.",
      description =
          "This endpoint will be deprecated in v2 in favor of top level subset endpoints.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description = "Terminology, e.g. 'ncit'.",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Subset code, e.g. 'C116978' for <i>ncit</i>. This call is only meaningful for"
                + " <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data tc return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "summary")
  })
  @RecordMetric
  @GetMapping(value = "/metadata/{terminology}/subset/{code}", produces = "application/json")
  public @ResponseBody Concept getSubset(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {
    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subset " + code + " not found");
      }
      Optional<Concept> concept = metadataService.getSubset(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subset " + code + " not found");
      return concept.get();
    } catch (Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the source stats.
   *
   * @param terminology the terminology
   * @param source the source
   * @return the subset
   * @throws Exception the exception
   */
  @Operation(
      summary = "Get statistics for the source within the specified terminology.",
      description = "This endpoint is mostly for NCIm to make source overlap statistics available.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description = "Terminology, e.g. 'ncit'.",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncim"),
    @Parameter(
        name = "source",
        description = "terminology source code, e.g. 'LNC' for <i>ncim</i>.",
        required = true,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "metadata/{terminology}/stats/{source}", produces = "application/json")
  public @ResponseBody Map<String, List<StatisticsEntry>> getSourceStats(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "source") final String source)
      throws Exception {
    try {
      return metadataService.getSourceStats(terminology, source);
    } catch (Exception e) {
      logger.error(source + " search in " + terminology + " failed.");
      handleException(e, terminology);
      return null;
    }
  }
}
