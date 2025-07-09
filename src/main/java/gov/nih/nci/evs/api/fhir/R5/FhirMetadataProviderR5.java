package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerConfiguration;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import gov.nih.nci.evs.api.controller.VersionController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.Enumerations.OperationParameterUse;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.OperationDefinition;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StringType;

/**
 * See <a href="https://www.hl7.org/fhir/terminologycapabilities.html">terminology capabilities</a>
 * See <a
 * href="https://smilecdr.com/hapi-fhir/docs/server_plain/introduction.html#capability-statement-server-metadata">capapbility
 * statement server metadata</a> Call using GET [base]/metadata?mode=terminology See <a
 * href="https://github.com/jamesagnew/hapi-fhir/issues/1681">github issue 1681</a>
 */
public class FhirMetadataProviderR5 extends ServerCapabilityStatementProvider {

  /**
   * Instantiates a new FHIR metadata provider.
   *
   * @param theServer the server
   */
  public FhirMetadataProviderR5(final RestfulServer theServer) {
    super(theServer);
  }

  /**
   * Instantiates a new FHIR metadata provider.
   *
   * @param theContext the context
   * @param theServerConfiguration the server configuration
   */
  public FhirMetadataProviderR5(
      final FhirContext theContext, final RestfulServerConfiguration theServerConfiguration) {
    super(theContext, theServerConfiguration);
  }

  /**
   * Instantiates a new FHIR metadata provider.
   *
   * @param theRestfulServer the restful server
   * @param theSearchParamRegistry the search param registry
   * @param theValidationSupport the validation support
   */
  public FhirMetadataProviderR5(
      final RestfulServer theRestfulServer,
      final ISearchParamRegistry theSearchParamRegistry,
      final IValidationSupport theValidationSupport) {
    super(theRestfulServer, theSearchParamRegistry, theValidationSupport);
  }

  /**
   * Gets the metadata resource.
   *
   * @param request the request
   * @param requestDetails the request details
   * @return the metadata resource
   */
  // cacheMills is to ensure the results of this callar
  @Metadata(cacheMillis = 0)
  public IBaseConformance getMetadataResource(
      final HttpServletRequest request, final RequestDetails requestDetails) {
    // Check if the request is for the terminology mode	TerminologyCapabilities
    if (request.getParameter("mode") != null
        && request.getParameter("mode").equals("terminology")) {
      return new FhirTerminologyCapabilitiesR5().withDefaults();
    }

    // Check if the request is for general CapabilityStatement
    else {
      IBaseConformance ibc = super.getServerConformance(request, requestDetails);
      CapabilityStatement capabilityStatement = (CapabilityStatement) ibc;
      CanonicalType instantiateUri =
          new CanonicalType("http://hl7.org/fhir/CapabilityStatement/terminology-server");

      capabilityStatement.setInstantiates(Collections.singletonList(instantiateUri));

      capabilityStatement.setSoftware(
          new CapabilityStatement.CapabilityStatementSoftwareComponent()
              .setName("EVSRESTAPI")
              .setVersion(VersionController.VERSION)
              .setReleaseDate(new Date()));

      capabilityStatement.setUrl("https://api-evsrest.nci.nih.gov/fhir/r5/metadata");
      capabilityStatement.setVersion(VersionController.VERSION);
      capabilityStatement.setName("EVSRESTAPIFHIRTerminologyServer");
      capabilityStatement.setTitle("EVSRESTAPI R5 FHIR Terminology Server");
      capabilityStatement.setStatus(Enumerations.PublicationStatus.ACTIVE);
      capabilityStatement.setExperimental(true);
      capabilityStatement.setPublisher("NCI EVS");
      capabilityStatement.setDate(new Date());

      addExtensions(capabilityStatement);

      // Find the "rest" component with mode = "server"
      Optional<CapabilityStatementRestComponent> serverRestComponent =
          capabilityStatement.getRest().stream()
              .filter(rest -> "server".equals(rest.getMode().toString().toLowerCase()))
              .findFirst();

      if (serverRestComponent.isPresent()) {
        CapabilityStatementRestComponent rest = serverRestComponent.get();
        CapabilityStatementRestSecurityComponent security = rest.getSecurity();
        if (security.isEmpty()) {
          security = new CapabilityStatementRestSecurityComponent();
          rest.setSecurity(security);
        }

        // Create the CodeableConcept for the "service" property
        CodeableConcept serviceCodeableConcept = new CodeableConcept();
        serviceCodeableConcept.addCoding(
            new Coding(
                "http://terminology.hl7.org/CodeSystem/restful-security-service",
                "OAuth2.0",
                "OAuth 2.0")); // Example coding

        // Set the "service" property
        security.setService(Collections.singletonList(serviceCodeableConcept));
      }
      return capabilityStatement;
    }
  }

  /**
   * Adds the extensions.
   *
   * @param capabilityStatement the capability statement
   */
  private void addExtensions(CapabilityStatement capabilityStatement) {
    // First Extension
    Extension featureExtension1 =
        new Extension("http://hl7.org/fhir/uv/application-feature/StructureDefinition/feature");

    Extension definition1 =
        new Extension(
            "definition",
            new CanonicalType("http://hl7.org/fhir/uv/tx-tests/FeatureDefinition/test-version"));
    Extension value1 = new Extension("value", new CodeType("1.7.5"));

    featureExtension1.addExtension(definition1);
    featureExtension1.addExtension(value1);

    capabilityStatement.addExtension(featureExtension1);

    Extension featureExtension2 =
        new Extension("http://hl7.org/fhir/uv/application-feature/StructureDefinition/feature");

    Extension definition2 =
        new Extension(
            "definition",
            new CanonicalType(
                "http://hl7.org/fhir/uv/tx-ecosystem/FeatureDefinition/CodeSystemAsParameter"));
    Extension value2 = new Extension("value", new BooleanType("true"));

    featureExtension2.addExtension(definition2);
    featureExtension2.addExtension(value2);

    capabilityStatement.addExtension(featureExtension2);
  }

  /**
   * Read OperationDefinition resources.
   *
   * @param theId the the id
   * @return the operation definition
   */
  @Read(type = OperationDefinition.class)
  public OperationDefinition readOperationDefinition(@IdParam IdType theId) {
    if ("versions".equals(theId.getIdPart()) || "-versions".equals(theId.getIdPart())) {
      return createVersionsOperationDefinition();
    }
    // Also handle the full URL case
    if (theId.hasBaseUrl()
        && theId.getValue().equals("http://hl7.org/fhir/OperationDefinition/-versions")) {
      return createVersionsOperationDefinition();
    }
    throw new ResourceNotFoundException("OperationDefinition/" + theId.getIdPart());
  }

  /**
   * System-level $versions operation.
   *
   * @param theRequest the the request
   * @param theResponse the the response
   * @param theRequestDetails the the request details
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("resource")
  @Operation(name = "$versions", manualResponse = true, manualRequest = true, idempotent = true)
  public void versions(
      HttpServletRequest theRequest,
      HttpServletResponse theResponse,
      RequestDetails theRequestDetails)
      throws IOException {

    // Create your version response
    Parameters parameters = new Parameters();
    parameters
        .addParameter()
        .setName("version")
        .setValue(new StringType(VersionController.VERSION));
    parameters.addParameter().setName("fhirVersion").setValue(new StringType("5.0.0"));

    // Return the response
    FhirContext fhirContext = theRequestDetails.getFhirContext();
    IParser parser = fhirContext.newJsonParser();
    theResponse.setContentType("application/fhir+json");
    theResponse.getWriter().write(parser.encodeResourceToString(parameters));
  }

  /**
   * Creates the versions operation definition.
   *
   * @return the operation definition
   */
  private OperationDefinition createVersionsOperationDefinition() {
    OperationDefinition opDef = new OperationDefinition();
    opDef.setId("versions");
    opDef.setUrl("http://hl7.org/fhir/OperationDefinition/-versions");
    opDef.setName("versions");
    opDef.setTitle("Get Terminology Server Version Information");
    opDef.setStatus(Enumerations.PublicationStatus.ACTIVE);
    opDef.setKind(OperationDefinition.OperationKind.OPERATION);
    opDef.setCode("versions");
    opDef.setSystem(true);
    opDef.setType(false);
    opDef.setInstance(false);

    // Add parameters as needed for your version operation
    OperationDefinition.OperationDefinitionParameterComponent param =
        new OperationDefinition.OperationDefinitionParameterComponent();
    param.setName("return");
    param.setUse(OperationParameterUse.OUT);
    param.setMin(1);
    param.setMax("1");
    param.setType(FHIRTypes.PARAMETERS);
    opDef.addParameter(param);

    return opDef;
  }
}
