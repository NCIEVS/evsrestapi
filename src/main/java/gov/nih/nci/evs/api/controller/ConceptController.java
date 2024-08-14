package gov.nih.nci.evs.api.controller;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.AssociationEntryResultList;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMap;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Controller for /concept endpoints. */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Concept endpoints")
public class ConceptController extends BaseController {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ConceptController.class);

  /** The sparql query manager service. */
  @Autowired SparqlQueryManagerService sparqlQueryManagerService;

  /** The elastic query service. */
  @Autowired ElasticQueryService elasticQueryService;

  /** The elastic search service. */
  @Autowired ElasticSearchService elasticSearchService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /** The metadata service. */
  /* The metadata service */
  @Autowired MetadataService metadataService;

  /** The request. */
  @Autowired HttpServletRequest request;

  /**
   * Returns the associations.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @param license the license
   * @return the associations
   * @throws Exception the exception
   */
  @Operation(summary = "Get concepts specified by list parameter")
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
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "list",
        description =
            "List (comma-separated) of codes to return concepts for, e.g."
                + "<ul><li>'C2291,C3224' for <i>ncit</i></li>"
                + "<li>'C0010137,C0025202' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @GetMapping(value = "/concept/{terminology}", produces = "application/json")
  @RecordMetric
  public @ResponseBody List<Concept> getConcepts(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(name = "list", required = true) final String list,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));

      final String[] codes = list.split(",");
      // Impose a maximum number at a time
      if (codes.length > 1000) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Maximum number of concepts to request at a time is 1000 = " + codes.length);
      }

      final List<Concept> concepts =
          elasticQueryService.getConcepts(Arrays.asList(codes), term, ip);

      if (ip.isMaps() && concepts.size() > 0) {
        List<ConceptMap> firstList = null;
        List<String> conceptCodeList = Arrays.asList(list.split(","));
        List<ConceptMap> secondList =
            elasticSearchService.getConceptMappings(conceptCodeList, terminology);
        for (Concept concept : concepts) {
          firstList = concept.getMaps();
          firstList.addAll(
              secondList.stream()
                  .filter(
                      cm ->
                          cm.getSourceCode().equals(concept.getCode())
                              || cm.getTargetCode().equals(concept.getCode()))
                  .collect(Collectors.toList()));
        }
      }

      return concepts;
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the concept.
   *
   * @param terminology the terminology
   * @param code the code
   * @param limit the limit
   * @param include the include
   * @param license the license
   * @return the concept
   * @throws Exception the exception
   */
  @Operation(summary = "Get the concept for the specified terminology and code")
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
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g."
                + "<ul><li>'C3224' for <i>ncit</i></li>"
                + "<li>'C0025202' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "limit",
        description =
            "If set to an integer (between <i>1</i> and <i>100</i>), elements of the concept should"
                + " be limited to that specified number of entries. Thus a user interface can"
                + " quickly retrieve initial data for a concept (even with <i>include=full</i>) and"
                + " then call back for more data. An extra placeholder entry with just a <i>ct</i>"
                + " field will be included to indicate the total count.",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "100"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "summary"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}", produces = "application/json")
  public @ResponseBody Concept getConcept(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestParam(required = false, name = "limit") final Optional<Integer> limit,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));

      final Optional<Concept> concept = elasticQueryService.getConcept(code, term, ip);

      if (!concept.isPresent() || concept.get().getCode() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      if (ip.isMaps()) {
        List<ConceptMap> firstList = concept.get().getMaps();
        List<ConceptMap> secondList =
            elasticSearchService.getConceptMappings(Arrays.asList(code), terminology);
        firstList.addAll(secondList);
        concept.get().setMaps(firstList);
      }

      if (limit.isPresent()) {
        if (limit.get().intValue() < 1 || limit.get().intValue() > 100) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }
        ConceptUtils.applyLimit(concept.get(), limit.get().intValue());
      }
      return concept.get();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the associations.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the associations
   * @throws Exception the exception
   */
  @Operation(summary = "Get the associations for the specified terminology and code")
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
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. <ul><li>'C3224' for <i>ncit</i></li>"
                + "<li>'C0025202' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/associations", produces = "application/json")
  public @ResponseBody List<Association> getAssociations(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("associations"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getAssociations();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the association entries.
   *
   * @param terminology the terminology
   * @param codeOrLabel the code or label
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param license the license
   * @return the association antries
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get the association entries for the specified terminology and code. Associations used to"
              + " define subset membership are not resolved by this call")
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
        name = "codeOrLabel",
        description =
            "Code/label in the specified terminology, e.g. "
                + "'A5' or 'Has_Salt_Form' for <i>ncit</i>."
                + " This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "10"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(
      value = "/concept/{terminology}/associations/{codeOrLabel}",
      produces = "application/json")
  public @ResponseBody AssociationEntryResultList getAssociationEntries(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "codeOrLabel") final String codeOrLabel,
      @RequestParam(required = false, name = "fromRecord") final Optional<Integer> fromRecord,
      @RequestParam(required = false, name = "pageSize") final Optional<Integer> pageSize,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    // Get the association "label"
    final Long startTime = System.currentTimeMillis();
    final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
    termUtils.checkLicense(term, license);

    final Optional<Concept> association =
        metadataService.getAssociation(terminology, codeOrLabel, Optional.ofNullable("minimal"));
    if (!association.isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Association " + codeOrLabel + " not found");
    }
    final String label = association.get().getName();
    if (termUtils
        .getIndexedTerminology(terminology, elasticQueryService)
        .getMetadata()
        .getSubsetMember()
        .contains(codeOrLabel)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Associations used to define subset membership are not resolved by this call");
    }
    // Getting the list
    final AssociationEntryResultList list =
        metadataService.getAssociationEntries(
            terminology, label, fromRecord.orElse(0), pageSize.orElse(10));
    list.setTimeTaken(System.currentTimeMillis() - startTime);

    return list;
  }

  /**
   * Returns the inverse associations.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the inverse associations
   * @throws Exception the exception
   */
  @Operation(summary = "Get inverse associations for the specified terminology and code")
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
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g."
                + "<ul><li>'C3224' for <i>ncit</i></li>"
                + "<li>'C0025202' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(
      value = "/concept/{terminology}/{code}/inverseAssociations",
      produces = "application/json")
  public @ResponseBody List<Association> getInverseAssociations(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("inverseAssociations"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getInverseAssociations();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the subsets.
   *
   * @param terminology the terminology
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param code the code
   * @param include the include
   * @param license the license
   * @return the subsets
   * @throws Exception the exception
   */
  @Operation(
      summary = "Get subset members for the specified terminology and code.",
      description =
          "This endpoint will be deprecated in v2 in favor of a top level subset member endpoint.")
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
        name = "code",
        description =
            "Code for a subset concept in the specified terminology, e.g. 'C157225' for"
                + " <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "10000"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/subsetMembers/{code}", produces = "application/json")
  public @ResponseBody List<Concept> getSubsetMembers(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam("fromRecord") final Optional<Integer> fromRecord,
      @RequestParam(required = false, value = "pageSize") final Optional<Integer> pageSize,
      @PathVariable(required = false, value = "code") final String code,
      @RequestParam("include") final Optional<String> include,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);
      final IncludeParam ip = new IncludeParam(include.orElse("minimal"));

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("inverseAssociations"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      final List<Concept> subsets = new ArrayList<>();
      final int associationListSize = concept.get().getInverseAssociations().size();

      if (associationListSize > 0) {
        final int fromIndex = fromRecord.orElse(0);
        final int toIndex =
            Math.min(pageSize.orElse(associationListSize) + fromIndex, associationListSize);
        for (final Association assn :
            concept.get().getInverseAssociations().subList(fromIndex, toIndex)) {
          final Concept member =
              elasticQueryService.getConcept(assn.getRelatedCode(), term, ip).orElse(null);
          if (member != null) {
            subsets.add(member);
          } else {
            logger.warn(
                "Unexpected subset member that could not be loaded as a concept = "
                    + term.getTerminology()
                    + ", "
                    + assn.getRelatedCode());
          }
        }
      }

      return subsets;
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the roles.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the roles
   * @throws Exception the exception
   */
  @Operation(summary = "Get roles for the specified terminology and code")
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
        description = "Terminology, e.g. 'ncit'",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/roles", produces = "application/json")
  public @ResponseBody List<Role> getRoles(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("roles"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getRoles();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the inverse roles.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the inverse roles
   * @throws Exception the exception
   */
  @Operation(summary = "Get inverse roles for the specified terminology and code")
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
        description = "Terminology, e.g. 'ncit'",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "'C3224' for <i>ncit</i>.  This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/inverseRoles", produces = "application/json")
  public @ResponseBody List<Role> getInverseRoles(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("inverseRoles"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getInverseRoles();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the parents.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the parents
   * @throws Exception the exception
   */
  @Operation(summary = "Get parent concepts for the specified terminology and code")
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
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "<ul><li>'C3224' for <i>ncit</i></li><li>'C0025202' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/parents", produces = "application/json")
  public @ResponseBody List<Concept> getParents(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("parents"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getParents();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the children.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the children
   * @throws Exception the exception
   */
  @Operation(summary = "Get child concepts for the specified terminology and code")
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
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "<ul><li>'C3224' for <i>ncit</i></li><li>'C0025202' for <i>ncim</i></li></ul>",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/children", produces = "application/json")
  public @ResponseBody List<Concept> getChildren(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("children"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getChildren();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the descendants.
   *
   * @param terminology the terminology
   * @param code the code
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param maxLevel the max level
   * @param license the license
   * @return the descendants
   * @throws Exception the exception
   */
  @Operation(summary = "Get descendant concepts for the specified terminology and code")
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
        description = "Terminology, e.g. 'ncit''",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "50000"),
    @Parameter(
        name = "maxLevel",
        description = "Max level of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "10000"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/descendants", produces = "application/json")
  public @ResponseBody List<Concept> getDescendants(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestParam(required = false, name = "fromRecord") final Optional<Integer> fromRecord,
      @RequestParam(required = false, name = "pageSize") final Optional<Integer> pageSize,
      @RequestParam(required = false, name = "maxLevel") final Optional<Integer> maxLevel,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      final List<Concept> baseList =
          new ArrayList<Concept>(elasticQueryService.getDescendants(code, term));
      final Predicate<Concept> byLevel = concept -> concept.getLevel() <= maxLevel.orElse(10000);
      final List<Concept> list = baseList.stream().filter(byLevel).collect(Collectors.toList());

      final int fromIndex = fromRecord.orElse(0);
      // Use a large default page size
      int toIndex = fromIndex + pageSize.orElse(50000);
      if (toIndex >= list.size()) {
        toIndex = list.size();
      }
      // If requesting beyond end of the list, just return empty
      if (fromRecord.orElse(0) > list.size()) {
        return new ArrayList<>();
      }
      // empty list
      if (list.size() == 0) {
        return list;
      }

      return list.subList(fromIndex, toIndex);

    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the maps.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the maps
   * @throws Exception the exception
   */
  @Operation(summary = "Get maps for the specified terminology and code")
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
        description = "Terminology, e.g. 'ncit'",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/maps", produces = "application/json")
  public @ResponseBody List<ConceptMap> getMaps(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("maps"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getMaps();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the history.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the concept with history
   * @throws Exception the exception
   */
  @Operation(summary = "Get history for the specified terminology and code")
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
        description = "Terminology, e.g. 'ncit'",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. 'C3224' for <i>ncit</i>. This call is only"
                + " meaningful for <i>ncit</i> and <i>ncim</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/history", produces = "application/json")
  public @ResponseBody Concept getHistory(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("history"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the disjoint with.
   *
   * @param terminology the terminology
   * @param code the code
   * @param license the license
   * @return the disjoint with
   * @throws Exception the exception
   */
  @Operation(summary = "Get \"disjoint with\" info for the specified terminology and code")
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
        description = "Terminology, e.g. 'ncit'",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "'C3910' for <i>ncit</i>.  This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/disjointWith", produces = "application/json")
  public @ResponseBody List<DisjointWith> getDisjointWith(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("disjointWith"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getDisjointWith();
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the roots.
   *
   * @param terminology the terminology
   * @param include the include
   * @param license the license
   * @return the roots
   * @throws Exception the exception
   */
  @Operation(summary = "Get root concepts for the specified terminology")
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
        description = "Terminology, e.g. 'ncit'.  This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/roots", produces = "application/json")
  public @ResponseBody List<Concept> getRoots(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      final List<Concept> list = elasticQueryService.getRootNodes(term, ip);
      if (list == null || list.isEmpty()) {
        return new ArrayList<>();
      }
      // "leaf" should be set to false for all roots
      for (final Concept c : list) {
        c.setLeaf(false);
      }

      return list;
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the paths from root.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param license the license
   * @return the paths from root
   * @throws Exception the exception
   */
  @Operation(summary = "Get paths from the hierarchy root to the specified concept.")
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
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>. For this call, it is"
                + " recommended to avoid using this parameter unless you need it for a specific use"
                + " case.  Any value other than 'minimal' may produce very large payload results. ",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "100"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/pathsFromRoot", produces = "application/json")
  public @ResponseBody List<List<Concept>> getPathsFromRoot(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "fromRecord") final Optional<Integer> fromRecord,
      @RequestParam(required = false, name = "pageSize") final Optional<Integer> pageSize,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }

      final Paths paths = elasticQueryService.getPathsToRoot(code, term);

      if (fromRecord.orElse(0) >= paths.getPaths().size()) {
        return new ArrayList<>();
      } else if (pageSize.isPresent()) {
        final int toIndex = fromRecord.orElse(0) + pageSize.get();
        paths.setPaths(
            paths
                .getPaths()
                .subList(fromRecord.orElse(0), Math.min(paths.getPaths().size(), toIndex)));
      }

      return ConceptUtils.convertPathsWithInclude(elasticQueryService, ip, term, paths, true);
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the subtree.
   *
   * @param terminology the terminology
   * @param code the code
   * @param limit the limit
   * @param license the license
   * @return the subtree
   * @throws Exception the exception
   */
  @Operation(summary = "Get the entire subtree from the root node to the specified code")
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
  @Parameters({
    @Parameter(
        name = "terminology",
        description = "Terminology, e.g. 'ncit'",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "limit",
        description =
            "If set to an integer (between <i>1</i> and <i>100</i>), subtrees and siblings "
                + "at each level will be limited to the specified number of entries. Thus a user "
                + "interface can quickly retrieve initial data for a subtree and then call back "
                + "for more data. "
                + "An extra placeholder entry with just a <i>ct</i> field will be included "
                + "to indicate the total count.",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "100"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/subtree", produces = "application/json")
  public @ResponseBody List<HierarchyNode> getSubtree(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestParam(required = false, name = "limit") final Optional<Integer> limit,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      if (limit.isPresent()) {
        if (limit.get().intValue() < 1 || limit.get().intValue() > 100) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }
      }
      // final List<HierarchyNode> nodes =
      // elasticQueryService.getPathInHierarchy(code, term);
      // return nodes;

      final List<HierarchyNode> rootNodes = elasticQueryService.getRootNodesHierarchy(term);
      final Paths paths = elasticQueryService.getPathsToRoot(code, term);

      // root hierarchy node map for quick look up
      final HashMap<String, HierarchyNode> rootNodeMap = new HashMap<>();
      rootNodes.stream().forEach(n -> rootNodeMap.put(n.getCode(), n));

      // Limit to 10 paths if a limit of >10 is used.
      final int pathLimit =
          (limit.orElse(0) > 10) ? 10 : (limit.isPresent() ? limit.get().intValue() : -1);
      final List<Path> ps = paths.getPaths();
      int ct = 0;
      for (final Path path : ps) {
        final List<ConceptMinimal> concepts = path.getConcepts();
        if (CollectionUtils.isEmpty(concepts) || concepts.size() < 2) {
          continue;
        }

        // Stop processing if we've reached the path limit
        if (++ct > pathLimit && limit.isPresent()) {
          break;
        }

        final ConceptMinimal rootConcept = concepts.get(concepts.size() - 1);

        final HierarchyNode root = rootNodeMap.get(rootConcept.getCode());
        HierarchyNode previous = root;
        for (int j = concepts.size() - 2; j >= 0; j--) {
          final ConceptMinimal c = concepts.get(j);
          if (!previous.getChildren().stream()
              .anyMatch(n -> n.getCt() == null && n.getCode().equals(c.getCode()))) {
            List<HierarchyNode> children =
                elasticQueryService.getChildNodes(previous.getCode(), 0, term);

            // Apply the limit
            if (limit.isPresent() && children.size() > limit.get().intValue()) {

              // Save the matching node as it should be included
              final HierarchyNode child =
                  children.stream().filter(h -> h.getCode().equals(c.getCode())).findFirst().get();

              final int i = children.indexOf(child);
              final int l = limit.get().intValue();
              // generate sublist (if the matching node isn't within limit, take
              // one less)
              children = ConceptUtils.sublist(children, 0, l);
              // If matching node is beyond the limit, remove the last entry and
              // add it
              if (i > (l - 1)) {
                // Remove and save the ct node
                final HierarchyNode ctNode = children.remove(children.size() - 1);
                // Remove second-to-last element (last one is the "ct" entry)
                if (children.size() > 0) {
                  children.remove(children.size() - 1);
                }
                // Add the node and the ct node
                children.add(child);
                children.add(ctNode);
              }
            }

            for (final HierarchyNode child : children) {
              child.setLevel(null);
              previous.getChildren().add(child);
            }
            previous.setExpanded(true);
          }
          previous =
              previous.getChildren().stream()
                  .filter(n -> n.getCt() == null && n.getCode().equals(c.getCode()))
                  .findFirst()
                  .orElse(null);
        }
      }

      if (limit.isPresent() && ps.size() > pathLimit) {
        final HierarchyNode extra = new HierarchyNode();
        extra.setCt(ps.size());
        rootNodes.add(extra);
      }

      return rootNodes;

    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the subtree children.
   *
   * @param terminology the terminology
   * @param code the code
   * @param limit the limit
   * @param license the license
   * @return the subtree children
   * @throws Exception the exception
   */
  @Operation(summary = "Get the entire subtree from the root node to the specified code")
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
  @Parameters({
    @Parameter(
        name = "terminology",
        description = "Terminology, e.g. 'ncit'",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "code",
        description =
            "Code in the specified terminology, e.g. 'C3224' for <i>ncit</i>. "
                + "This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "limit",
        description =
            "If set to an integer (between <i>1</i> and <i>100</i>), children will "
                + "be limited to the specified number of entries. Thus a user interface can "
                + "quickly retrieve initial data for a subtree and then call back for more data. "
                + "An extra placeholder entry with just a <i>ct</i> field will be included "
                + "to indicate the total count.",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "100"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(
      value = "/concept/{terminology}/{code}/subtree/children",
      produces = "application/json")
  public @ResponseBody List<HierarchyNode> getSubtreeChildren(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestParam(required = false, name = "limit") final Optional<Integer> limit,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      if (limit.isPresent()) {
        if (limit.get().intValue() < 1 || limit.get().intValue() > 100) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }
      }
      // If terminology is "ncim", there are no child nodes
      if ("ncim".equals(terminology)) {
        return new ArrayList<>();
      }
      final List<HierarchyNode> nodes = elasticQueryService.getChildNodes(code, 0, term);
      // "count" doesn't force it to use check the stream.
      nodes.stream().peek(n -> n.setLevel(null)).collect(Collectors.toList());

      if (limit.isPresent() && nodes.size() > limit.get().intValue()) {
        return ConceptUtils.sublist(nodes, 0, limit.get().intValue());
      }
      return nodes;

    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the paths to root.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param license the license
   * @return the paths to root
   * @throws Exception the exception
   */
  @Operation(summary = "Get paths to the hierarchy root from the specified code")
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
        name = "code",
        description =
            "Code in the specified terminology, e.g. "
                + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>. For this call, it is"
                + " recommended to avoid using this parameter unless you need it for a specific use"
                + " case.  Any value other than 'minimal' may produce very large payload results. ",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "100"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/{terminology}/{code}/pathsToRoot", produces = "application/json")
  public @ResponseBody List<List<Concept>> getPathsToRoot(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "fromRecord") final Optional<Integer> fromRecord,
      @RequestParam(required = false, name = "pageSize") final Optional<Integer> pageSize,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final Paths paths = elasticQueryService.getPathsToRoot(code, term);

      if (fromRecord.orElse(0) >= paths.getPaths().size()) {
        return new ArrayList<>();
      } else if (pageSize.isPresent()) {
        final int toIndex = fromRecord.orElse(0) + pageSize.get();
        paths.setPaths(
            paths
                .getPaths()
                .subList(fromRecord.orElse(0), Math.min(paths.getPaths().size(), toIndex)));
      }

      return ConceptUtils.convertPathsWithInclude(elasticQueryService, ip, term, paths, false);
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the paths to ancestor.
   *
   * @param terminology the terminology
   * @param code the code
   * @param ancestorCode the ancestor code
   * @param include the include
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param license the license
   * @return the paths to ancestor
   * @throws Exception the exception
   */
  @Operation(summary = "Get paths from the specified code to the specified ancestor code")
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
        name = "code",
        description =
            "Code in the specified terminology, e.g."
                + " 'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "ancestorCode",
        description =
            "Ancestor code of the other specified code, e.g. "
                + "'C2991' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>. For this call, it is"
                + " recommended to avoid using this parameter unless you need it for a specific use"
                + " case.  Any value other than 'minimal' may produce very large payload results. ",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "100"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(
      value = "/concept/{terminology}/{code}/pathsToAncestor/{ancestorCode}",
      produces = "application/json")
  public @ResponseBody List<List<Concept>> getPathsToAncestor(
      @PathVariable(value = "terminology") final String terminology,
      @PathVariable(value = "code") final String code,
      @PathVariable(value = "ancestorCode") final String ancestorCode,
      @RequestParam(required = false, name = "include") final Optional<String> include,
      @RequestParam(required = false, name = "fromRecord") final Optional<Integer> fromRecord,
      @RequestParam(required = false, name = "pageSize") final Optional<Integer> pageSize,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
      termUtils.checkLicense(term, license);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final Paths paths = elasticQueryService.getPathsToParent(code, ancestorCode, term);

      if (fromRecord.orElse(0) >= paths.getPaths().size()) {
        return new ArrayList<>();
      } else if (pageSize.isPresent()) {
        final int toIndex = fromRecord.orElse(0) + pageSize.get();
        paths.setPaths(
            paths
                .getPaths()
                .subList(fromRecord.orElse(0), Math.min(paths.getPaths().size(), toIndex)));
      }

      return ConceptUtils.convertPathsWithInclude(elasticQueryService, ip, term, paths, false);

    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  // Used for QA to expose a mechanism to calculate extension for a single case.
  // @Autowired
  // MainTypeHierarchy mainTypeHierarchy;
  //
  // @RequestMapping(method = RequestMethod.GET, value = "/extensions/{code}",
  // produces = "application/json")
  // @ApiIgnore
  // public @ResponseBody Concept calculateExtensions(@PathVariable(value =
  // "code")
  // final String code) throws Exception {
  // try {
  // final Terminology term = termUtils.getTerminology("ncit", true);
  // mainTypeHierarchy.initialize(term,
  // sparqlQueryManagerService.getHierarchyUtils(term));
  // final IncludeParam ip = new IncludeParam("full");
  // final Concept concept = sparqlQueryManagerService.getConcept(code, term,
  // ip);
  // concept.setPaths(sparqlQueryManagerService.getHierarchyUtils(term).getPaths(term,
  // code));
  // concept.setExtensions(mainTypeHierarchy.getExtensions(concept));
  // return concept;
  // } catch (Exception e) {
  // handleException(e);
  // return null;
  // }
  // }
}
