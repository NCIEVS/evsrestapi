
package gov.nih.nci.evs.api.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Controller for /metadata endpoints.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Metadata endpoints")
public class MetadataController extends BaseController {

  /** The Constant log. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(MetadataController.class);

  /** The metadata service. */
  @Autowired
  MetadataService metadataService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

  /**
   * Returns the terminologies.
   *
   * @return the terminologies
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all available terminologies", response = Terminology.class,
      responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "latest",
          value = "Return terminologies with matching <i>latest</i> value. e.g. true or false",
          required = false, dataTypeClass = Boolean.class, paramType = "query",
          defaultValue = "true"),
      @ApiImplicitParam(name = "tag",
          value = "Return terminologies with matching tag. e.g. 'monthly' or 'weekly' for <i>ncit</i>",
          required = false, dataTypeClass = String.class, paramType = "query"),
      @ApiImplicitParam(name = "terminology",
          value = "Return entries with matching terminology, e.g. 'ncit' or 'ncim'",
          required = false, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/terminologies",
      produces = "application/json")
  public @ResponseBody List<Terminology> getTerminologies(@RequestParam("latest")
  final Optional<Boolean> latest, @RequestParam("tag")
  final Optional<String> tag, @RequestParam("terminology")
  final Optional<String> terminology) throws Exception {
    List<String> tagList = Arrays.asList("monthly", "weekly");
    try {
      List<Terminology> terms = termUtils.getTerminologies(true);

      if (latest.isPresent()) {
        terms = terms.stream().filter(f -> f.getLatest().equals(latest.get()))
            .collect(Collectors.toList());
      }

      if (tag.isPresent() && tagList.contains(tag.get())) {
        terms = terms.stream().filter(f -> "true".equals(f.getTags().get(tag.get())))
            .collect(Collectors.toList());
      }

      if (terminology.isPresent()) {
        terms = terms.stream().filter(f -> f.getTerminology().equals(terminology.get()))
            .collect(Collectors.toList());
      }

      for (Terminology term : terms) {
        final TerminologyMetadata meta = term.getMetadata();
        // Some terminologies may not have metadata
        if (meta != null) {
          meta.setMonthlyDb(null);
          meta.setSources(null);
          meta.setTermTypes(null);
          meta.setSourcesToRemove(null);
          meta.setUnpublished(null);
          meta.setSubsetPrefix(null);
          meta.setSubsetLinks(null);
          meta.setConceptStatus(null);
          meta.setDefinitionSourceSet(null);
          meta.setWelcomeText(null);
        }
      }

      return terms;
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(
      value = "Get all associations (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/associations",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return associations for (or leave blank for all). If invalid values are passed, the result will simply include no entries for those invalid values.",
          required = false, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getAssociations(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include, @RequestParam("list")
  final Optional<String> list) throws Exception {
    try {
      return metadataService.getAssociations(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get the association for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Association code (or name), e.g. "
              + "<ul><li>'A10' or 'Has_CDRH_Parent' for <i>ncit</i></li>"
              + "<li>'RB' or 'has a broader relationship' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/association/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getAssociation(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "codeOrName")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {
    try {

      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Association " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getAssociation(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Association " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(
      value = "Get all roles (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/roles",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology",
          value = "Terminology, e.g. 'ncit'.  This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return roles for (or leave blank for all).  If invalid values are passed, the result will simply include no entries for those invalid values.",
          required = false, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getRoles(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include, @RequestParam("list")
  final Optional<String> list) throws Exception {
    try {
      return metadataService.getRoles(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get the role for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Role code (or name), e.g. "
              + "'R123' or 'Chemotherapy_Regimen_Has_Component' for <i>ncit</i>. "
              + "This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/role/{codeOrName}",
      produces = "application/json")
  public @ResponseBody Concept getRole(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "codeOrName")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {
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
      handleException(e);
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
  @ApiOperation(
      value = "Get all properties (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/properties",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return properties for (or leave blank for all).  If invalid values are passed, the result will simply include no entries for those invalid values.",
          required = false, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getProperties(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include, @RequestParam("list")
  final Optional<String> list) throws Exception {

    try {
      return metadataService.getProperties(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(
      value = "Get all subsets (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/subsets",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology",
          value = "Terminology, e.g. 'ncit'.  This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return subsets for (or leave blank for all).  If invalid values are passed, the result will simply include no entries for those invalid values.",
          required = false, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getSubsets(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include, @RequestParam("list")
  final Optional<String> list) throws Exception {
    try {
      return metadataService.getSubsets(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(
      value = "Get all qualifiers (properties on properties) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/qualifiers",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return qualifiers for (or leave blank for all)",
          required = false, dataTypeClass = String.class, paramType = "query")

  })
  @RecordMetric
  public @ResponseBody List<Concept> getQualifiers(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include, @RequestParam("list")
  final Optional<String> list) throws Exception {
    try {
      return metadataService.getQualifiers(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get the qualifier for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Qualifier code (or name), e.g."
              + "<ul><li>'P390' or 'go-source' for <i>ncit</i></li>"
              + "<li>'RG' or 'Relationship group' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/qualifier/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getQualifier(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "codeOrName")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {

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
      handleException(e);
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
  @ApiOperation(value = "Get all term types for the specified terminology",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/termTypes",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  public @ResponseBody List<ConceptMinimal> getTermTypes(@PathVariable(value = "terminology")
  final String terminology) throws Exception {
    try {
      return metadataService.getTermTypes(terminology);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get welcome text for the specified terminology", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/welcomeText",
      produces = "text/html")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  public @ResponseBody String getWelcomeText(@PathVariable(value = "terminology")
  final String terminology) throws Exception {
    try {
      return metadataService.getWelcomeText(terminology);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get the property for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Property code (or name), e.g. "
              + "<ul><li>'P216' or 'BioCarta_ID' for <i>ncit</i></li>"
              + "<li>'BioCarta_ID' or ''BioCarta ID' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/property/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getProperty(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "codeOrName")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {

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
      handleException(e);
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
  @ApiOperation(value = "Get the subset for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'.", required = true,
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Subset code, e.g. 'C116978' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data tc return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/subset/{code}",
      produces = "application/json")
  public @ResponseBody Concept getSubset(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {
    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + code + " not found");
      }
      Optional<Concept> concept = metadataService.getSubset(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + code + " not found");
      return concept.get();
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get all concept status values for the specified terminology",
      response = String.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology",
          value = "Terminology, e.g. 'ncit'. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/conceptStatuses",
      produces = "application/json")
  public @ResponseBody List<String> getConceptStatuses(@PathVariable(value = "terminology")
  final String terminology) throws Exception {
    try {
      Optional<List<String>> result = metadataService.getConceptStatuses(terminology);
      if (!result.isPresent()) {
        // this should never happen
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }
      return result.get();
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get all definition sources for the specified terminology",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/definitionSources",
      produces = "application/json")
  public @ResponseBody List<ConceptMinimal> getDefinitionSources(
    @PathVariable(value = "terminology")
    final String terminology) throws Exception {
    try {
      return metadataService.getDefinitionSources(terminology);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get all synonym sources for the specified terminology",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/synonymSources",
      produces = "application/json")
  public @ResponseBody List<ConceptMinimal> getSynonymSources(@PathVariable(value = "terminology")
  final String terminology) throws Exception {
    try {
      return metadataService.getSynonymSources(terminology);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get qualifier values for the specified terminology and code/name",
      response = String.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Qualifier code (or name), e.g."
              + "<ul><li>'P390' or 'go-source' for <i>ncit</i></li>"
              + "<li>'RG' or 'Relationship group' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/qualifier/{codeOrName}/values",
      produces = "application/json")
  public @ResponseBody List<String> getQualifierValues(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "codeOrName")
  final String code) throws Exception {
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
      handleException(e);
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
  @ApiOperation(
      value = "Get all synonym types (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/synonymTypes",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return synonym types for (or leave blank for all).  If invalid values are passed, the result will simply include no entries for those invalid values.",
          required = false, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getSynonymTypes(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include, @RequestParam("list")
  final Optional<String> list) throws Exception {

    try {
      return metadataService.getSynonymTypes(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  @ApiOperation(value = "Get the synonym type for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Synonym type code (or name), e.g."
              + "<ul><li>'P90' or 'FULL_SYN' for <i>ncit</i></li>"
              + "<li>'Preferred_Name' or 'Preferred name' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/synonymType/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getSynonymType(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "codeOrName")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {

    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Synonym type " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getSynonymType(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Synonym type " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(
      value = "Get all definition types (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/definitionTypes",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return definition types for (or leave blank for all).  If invalid values are passed, the result will simply include no entries for those invalid values.",
          required = false, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getDefinitionTypes(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include, @RequestParam("list")
  final Optional<String> list) throws Exception {

    try {
      return metadataService.getDefinitionTypes(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
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
  @ApiOperation(value = "Get the definition type for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Definition type code (or name), e.g."
              + "<ul><li>'P325' or 'DEFINITION' for <i>ncit</i></li>"
              + "<li>'DEFINITION' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/definitionType/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getDefinitionType(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "codeOrName")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {

    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Definition type " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getDefinitionType(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Defininition type " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }
}
