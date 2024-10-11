package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.FhirUtility;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** FHIR R5 CodeSystem provider. */
@Component
public class CodeSystemProviderR5 implements IResourceProvider {
  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(CodeSystemProviderR5.class);

  /** the query service. */
  @Autowired ElasticQueryService esQueryService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /**
   * Returns the type of resource for this provider.
   *
   * @return the resource
   */
  @Override
  public Class<CodeSystem> getResourceType() {
    return CodeSystem.class;
  }

  /**
   * Find code systems.
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param code the code
   * @param date the date
   * @param description the description
   * @param name the name
   * @param publisher the publisher
   * @param title the title
   * @param url the url (also known as system)
   * @param version the version
   * @param count the count
   * @param offset the offset
   * @return the bundle
   * @throws Exception exception
   */
  @Search
  public Bundle findCodeSystems(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @OptionalParam(name = "_id") final TokenParam id,
      @OptionalParam(name = "code") final TokenParam code,
      @OptionalParam(name = "date") final DateRangeParam date,
      @OptionalParam(name = "description") final StringParam description,
      @OptionalParam(name = "name") final StringParam name,
      @OptionalParam(name = "publisher") final StringParam publisher,
      @OptionalParam(name = "title") final StringParam title,
      @OptionalParam(name = "url") final UriParam url,
      @OptionalParam(name = "version") final StringParam version,
      @Description(shortDefinition = "Number of entries to return") @OptionalParam(name = "_count")
          final NumberParam count,
      @Description(shortDefinition = "Start offset, used when reading a next page")
          @OptionalParam(name = "_offset")
          final NumberParam offset)
      throws Exception {
    try {
      FhirUtilityR5.notSupportedSearchParams(request);
      // Get the indexed terms
      final List<Terminology> terms = termUtils.getIndexedTerminologies(esQueryService);
      final List<CodeSystem> list = new ArrayList<>();

      // Find the matching code systems in the list of terms
      for (final Terminology terminology : terms) {
        final CodeSystem cs = FhirUtilityR5.toR5(terminology);

        // Skip non-matching
        if ((id != null && !id.getValue().equals(cs.getIdPart()))
            || (url != null && !url.getValue().equals(cs.getUrl()))) {
          logger.info("  SKIP url mismatch = " + cs.getUrl());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cs.getDate())) {
          logger.info("  SKIP date mismatch = " + cs.getDate());
          continue;
        }
        if (description != null && !FhirUtility.compareString(description, cs.getDescription())) {
          logger.info("  SKIP description mismatch = " + cs.getDescription());
          continue;
        }
        if (name != null && !FhirUtility.compareString(name, cs.getName())) {
          logger.info("  SKIP name mismatch = " + cs.getName());
          continue;
        }
        if (publisher != null && !FhirUtility.compareString(publisher, cs.getPublisher())) {
          logger.info("  SKIP publisher mismatch = " + cs.getPublisher());
          continue;
        }
        if (title != null && !FhirUtility.compareString(title, cs.getTitle())) {
          logger.info("  SKIP title mismatch = " + cs.getTitle());
          continue;
        }
        if (version != null && !FhirUtility.compareString(version, cs.getVersion())) {
          logger.info("  SKIP version mismatch = " + cs.getVersion());
          continue;
        }

        list.add(cs);
      }
      return FhirUtilityR5.makeBundle(request, list, count, offset);
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR5.exception("Failed to find code systesm", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Look up implicit
   *
   * <pre>
   * <a href="https://build.fhir.org/codesystem-operation-lookup.html">...</a>
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param code the codeType code
   * @param system the uriType
   * @param version the string type version
   * @param coding the coding
   * @param date the date range param
   * @param displayLanguage the string type display language
   * @param property the code type property
   * @return the parameters
   * @throws Exception exception
   */
  @Operation(name = JpaConstants.OPERATION_LOOKUP, idempotent = true)
  public Parameters lookupImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "date") final DateRangeParam date,
      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      @OperationParam(name = "propert") final CodeType property)
      throws Exception {
    // Check if the request is a POST, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_LOOKUP,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired(code, "code", system, "system");
      FhirUtilityR5.mutuallyExclusive(code, "code", coding, "coding");
      FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      FhirUtilityR5.notSupported(property, "property");

      final List<CodeSystem> cs = findPossibleCodeSystems(null, date, system, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), esQueryService);
        final Concept concept =
            esQueryService.getConcept(codeToLookup, term, new IncludeParam("children")).get();
        // populate our Parameters
        params.addParameter("code", "code");
        params.addParameter("system", codeSys.getUrl());
        params.addParameter("code", codeSys.getName());
        params.addParameter("version", codeSys.getVersion());
        params.addParameter("display", concept.getName());
        params.addParameter("active", true);
        for (final Concept parent : concept.getParents()) {
          params.addParameter(FhirUtilityR5.createProperty("parent", parent.getCode(), true));
        }
        for (final Concept child : concept.getChildren()) {
          params.addParameter(FhirUtilityR5.createProperty("child", child.getCode(), true));
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find the matching code system");
        params.addParameter("system", (system == null ? new UriType("<null>") : system));
        params.addParameter("version", version);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to lookup code", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Lookup instance.
   *
   * <pre>
   *     <a href="https://build.fhir.org/codesystem-operation-lookup.html">...</a>
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param code the code
   * @param system the system
   * @param version the version
   * @param coding the coding
   * @param date the date
   * @param displayLanguage the display language
   * @param property the property
   * @return the parameters
   * @throws Exception exception
   */
  @Operation(name = JpaConstants.OPERATION_LOOKUP, idempotent = true)
  public Parameters lookupInstance(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "date") final DateRangeParam date,
      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      @OperationParam(name = "property") final CodeType property)
      throws Exception {
    // check if the request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_LOOKUP,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired(code, "code", system, "system");
      FhirUtilityR5.mutuallyExclusive(code, "code", coding, "coding");
      FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      FhirUtilityR5.notSupported(property, "property");
      final List<CodeSystem> cs = findPossibleCodeSystems(id, date, system, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), esQueryService);
        final Concept concept =
            esQueryService.getConcept(codeToLookup, term, new IncludeParam("children")).get();
        // Populate our Parameters
        params.addParameter("code", "code");
        params.addParameter("system", codeSys.getUrl());
        params.addParameter("code", codeSys.getName());
        params.addParameter("version", codeSys.getVersion());
        params.addParameter("display", concept.getName());
        params.addParameter("active", true);
        for (final Concept parent : concept.getParents()) {
          params.addParameter(FhirUtilityR5.createProperty(parent.getCode(), "parent", true));
        }
        for (final Concept child : concept.getChildren()) {
          params.addParameter(FhirUtilityR5.createProperty(child.getCode(), "child", true));
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find matching code system");
        params.addParameter("system", (system == null ? new UriType("<null>") : system));
        params.addParameter("version", version);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to lookup code", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code implicit
   *
   * <pre>
   * <a href="https://hl7.org/fhir/R5/codesystem-operation-validate-code.html">...</a>
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param url the url
   * @param codeSystem the code system
   * @param code the code
   * @param version the version
   * @param display the display
   * @param coding the coding
   * @param codeableConcept the codeable concept
   * @param date the date
   * @param abstractt the abstract
   * @param displayLanguage the display language
   * @param systemVersion the system version
   * @return the parameters
   * @throws Exception exception
   */
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "codeSystem") final CodeSystem codeSystem,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      @OperationParam(name = "date") final DateRangeParam date,
      @OperationParam(name = "abstract") final BooleanType abstractt,
      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      @OperationParam(name = "systemVersion") final StringType systemVersion)
      throws Exception {
    // Check if the request is a POST, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.notSupported(codeableConcept, "codeableConcept");
      FhirUtilityR5.notSupported(codeSystem, "codeSystem");
      FhirUtilityR5.notSupported(coding, "coding");
      FhirUtilityR5.notSupported(date, "date");
      FhirUtilityR5.notSupported(abstractt, "abstract");
      FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      FhirUtilityR5.notSupported(systemVersion, "systemVersion");
      final List<CodeSystem> cs = findPossibleCodeSystems(null, null, url, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        final String codeToValidate = code.getCode();
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), esQueryService);
        final Optional<Concept> check =
            esQueryService.getConcept(codeToValidate, term, new IncludeParam("children"));
        if (check.isPresent()) {
          final Concept concept =
              esQueryService.getConcept(codeToValidate, term, new IncludeParam("children")).get();
          params.addParameter("result", true);
          if (display == null || concept.getName().equals(display.getValue())) {
            params.addParameter("code", concept.getCode());
          } else {
            params.addParameter(
                "message",
                "The code"
                    + concept.getCode()
                    + " exists in this value set but the "
                    + "display is not valid");
          }
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          params.addParameter("display", concept.getName());
          params.addParameter("active", true);
        } else {
          params.addParameter("result", false);
          params.addParameter(
              "message", "The code does not exist for the supplied code system url and/or version");
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
        }
      } else {
        params.addParameter("results", false);
        params.addParameter("message", "Unable to find matching code system");
        params.addParameter("url", (url == null ? new UriType("<null>") : url));
        params.addParameter("version", version);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to validate code", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate the code instance
   *
   * <pre>
   * <a href="https://hl7.org/fhir/R5/codesystem-operation-validate-code.html">...</a>
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param url the url
   * @param codeSystem the code system
   * @param code the code
   * @param version the version
   * @param display the display
   * @param coding the coding
   * @param codeableConcept the codeable concept
   * @param date the date
   * @param abstractt the abstract
   * @param displayLanguage the display language
   * @param systemVersion the system version
   * @return the parameters
   * @throws Exception exception
   */
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeInstance(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "codeSystem") final CodeSystem codeSystem,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "abstract") final BooleanType abstractt,
      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      @OperationParam(name = "systemVersion") final StringType systemVersion)
      throws Exception {
    // Check if the request is a post, throw exception as we don't support post requests
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.notSupported(codeableConcept, "codeableConcept");
      FhirUtilityR5.notSupported(codeSystem, "codeSystem");
      FhirUtilityR5.notSupported(coding, "coding");
      FhirUtilityR5.notSupported(date, "date");
      FhirUtilityR5.notSupported(abstractt, "abstract");
      FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      FhirUtilityR5.notSupported(systemVersion, "systemVersion");
      final List<CodeSystem> cs = findPossibleCodeSystems(id, null, url, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        final String codeToValidate = code.getCode();
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), esQueryService);
        final Optional<Concept> check =
            esQueryService.getConcept(codeToValidate, term, new IncludeParam("children"));
        if (check.isPresent()) {
          final Concept concept =
              esQueryService.getConcept(codeToValidate, term, new IncludeParam("children")).get();
          params.addParameter("result", true);
          if (display == null || concept.getName().equals(display.getValue())) {
            params.addParameter("code", concept.getCode());
          } else {
            params.addParameter(
                "message",
                "The code "
                    + concept.getCode()
                    + " exists in this value set but the "
                    + "display is not valid");
          }
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          params.addParameter("display", concept.getName());
          params.addParameter("active", true);
        } else {
          params.addParameter("result", false);
          params.addParameter(
              "message", "The code does not exist for the supplied code system url and/or version");
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find matching code syste");
        params.addParameter("url", (url == null ? new UriType("<null>") : url));
        params.addParameter("version", version);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to validate code", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Subsumes implicit
   *
   * <pre>
   * <a href="https://hl7.org/fhir/R5/codesystem-operation-subsumes.html">...</a>
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param codeA the code a
   * @param codeB the code b
   * @param system the system
   * @param version the version
   * @param codingA the coding a
   * @param codingB the coding b
   * @return the parameters
   * @throws Exception exception
   */
  @Operation(name = JpaConstants.OPERATION_SUBSUMES, idempotent = true)
  public Parameters subsumesImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "codeA") final CodeType codeA,
      @OperationParam(name = "codeB") final CodeType codeB,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "codingA") final Coding codingA,
      @OperationParam(name = "codingB") final Coding codingB)
      throws Exception {
    // Check if the request is a post, throw exception as we don't support post requests
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired(codeA, "codeA", system, "system");
      FhirUtilityR5.mutuallyRequired(codeB, "codeB", system, "system");
      FhirUtilityR5.mutuallyExclusive(codingB, "codingB", codeB, "codeB");
      FhirUtilityR5.mutuallyExclusive(codingA, "codingA", codeA, "codeA");
      final List<CodeSystem> cs = findPossibleCodeSystems(null, null, system, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String code1 = "";
        String code2 = "";
        if (codeA != null && codeB != null) {
          code1 = codeA.getCode();
          code2 = codeB.getCode();
        } else if (codingA != null && codingB != null) {
          code1 = codingA.getCode();
          code2 = codingB.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), esQueryService);
        final Optional<Concept> checkA =
            esQueryService.getConcept(code1, term, new IncludeParam("minimal"));
        final Optional<Concept> checkB =
            esQueryService.getConcept(code2, term, new IncludeParam("minimal"));
        if (checkA.get() != null && checkB.get() != null) {
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          if (esQueryService.getPathsToParent(code1, code2, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumes");
          } else if (esQueryService.getPathsToParent(code2, code1, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumbed-by");
          } else {
            params.addParameter("outcome", "no-subsumption-relationship");
          }
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find matching code system");
        params.addParameter("system", (system == null ? new UriType("<null>") : system));
        params.addParameter("version", version);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to check if A subsumes B", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Subsumes instance
   *
   * <pre>
   * <a href="https://hl7.org/fhir/R5/codesystem-operation-subsumes.html">...</a>
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param codeA the code a
   * @param codeB the code b
   * @param system the system
   * @param version the version
   * @param codingA the coding a
   * @param codingB the coding b
   * @return the parameters
   * @throws Exception exception
   */
  @Operation(name = JpaConstants.OPERATION_SUBSUMES, idempotent = true)
  public Parameters subsumesInstance(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "codeA") final CodeType codeA,
      @OperationParam(name = "codeB") final CodeType codeB,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "codingA") final Coding codingA,
      @OperationParam(name = "codingB") final Coding codingB)
      throws Exception {
    // Check if the request is a post, throw exception as we don't support post requests
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired(codeA, "codeA", system, "system");
      FhirUtilityR5.mutuallyRequired(codeB, "codeB", system, "system");
      FhirUtilityR5.mutuallyExclusive(codingB, "codingB", codeB, "codeB");
      FhirUtilityR5.mutuallyExclusive(codingA, "codingA", codeA, "codeA");
      final List<CodeSystem> cs = findPossibleCodeSystems(null, null, system, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String code1 = "";
        String code2 = "";
        if (codeA != null && codeB != null) {
          code1 = codeA.getCode();
          code2 = codeB.getCode();
        } else if (codingA != null && codingB != null) {
          code1 = codingA.getCode();
          code2 = codingB.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), esQueryService);
        final Optional<Concept> checkA =
            esQueryService.getConcept(code1, term, new IncludeParam("minimal"));
        final Optional<Concept> checkB =
            esQueryService.getConcept(code2, term, new IncludeParam("minimal"));
        if (checkA.get() != null && checkB.get() != null) {
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          if (esQueryService.getPathsToParent(code1, code2, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumes");
          } else if (esQueryService.getPathsToParent(code2, code1, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumed-by");
          } else {
            params.addParameter("outcome", "no-subsumption-relationship");
          }
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find matching code system");
        params.addParameter("system", (system == null ? new UriType("<null>") : system));
        params.addParameter("version", version);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to check if A subsumes B", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Returns the concept map for the specified details.
   *
   * @param details the details
   * @param id the id
   * @return the code system concept map
   * @throws Exception exception
   */
  @Read
  public CodeSystem getCodeSystem(final ServletRequestDetails details, @IdParam final IdType id)
      throws Exception {
    try {
      final List<CodeSystem> candidates = findPossibleCodeSystems(id, null, null, null);
      for (final CodeSystem set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }
      throw FhirUtilityR5.exception(
          "Code system not found = " + (id == null ? "null" : id.getIdPart()),
          IssueType.NOTFOUND,
          404);
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR5.exception("Failed to get code system", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Helper method to find possible code systems.
   *
   * @param id the id type
   * @param date the date range
   * @param url the uri type
   * @param version the string type version
   * @return the list of code systems
   * @throws Exception exception
   */
  private List<CodeSystem> findPossibleCodeSystems(
      @OperationParam(name = "_id") final IdType id,
      @OperationParam(name = "date") final DateRangeParam date,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "version") final StringType version)
      throws Exception {
    try {
      if (id == null && url == null) {
        return new ArrayList<>(0);
      }

      final List<Terminology> terms = termUtils.getIndexedTerminologies(esQueryService);
      final List<CodeSystem> list = new ArrayList<>();
      // Find the matching code systems
      for (final Terminology terminology : terms) {
        final CodeSystem cs = FhirUtilityR5.toR5(terminology);
        // Skip non-matching
        if ((id != null && !id.getIdPart().equals(cs.getIdPart()))
            || (url != null && !url.getValue().equals(cs.getUrl()))) {
          logger.info("  SKIP url mismatch = " + cs.getUrl());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cs.getDate())) {
          logger.info("  SKIP date mismatch = " + cs.getDate());
          continue;
        }
        if (version != null && !version.getValue().equals(cs.getVersion())) {
          logger.info("  SKIP version mismatch = " + cs.getVersion());
          continue;
        }
        list.add(cs);
      }
      return list;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR5.exception("Failed to find code systems", IssueType.EXCEPTION, 500);
    }
  }
}
