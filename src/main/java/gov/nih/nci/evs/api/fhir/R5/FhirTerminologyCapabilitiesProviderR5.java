package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerConfiguration;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementDocumentComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementImplementationComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.TerminologyCapabilities.TerminologyCapabilitiesSoftwareComponent;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r5.model.UriType;

/**
 * See <a href="https://www.hl7.org/fhir/terminologycapabilities.html">terminology capabilities</a>
 * See <a
 * href="https://smilecdr.com/hapi-fhir/docs/server_plain/introduction.html#capability-statement-server-metadata">capapbility
 * statement server metadata</a> Call using GET [base]/metadata?mode=terminology See <a
 * href="https://github.com/jamesagnew/hapi-fhir/issues/1681">github issue 1681</a>
 */
public class FhirTerminologyCapabilitiesProviderR5 extends ServerCapabilityStatementProvider {
  /**
   * Instantiates a new FHIR terminology capabilities provider.
   *
   * @param theServer the server
   */
  public FhirTerminologyCapabilitiesProviderR5(final RestfulServer theServer) {
    super(theServer);
  }

  /**
   * Instantiates a new FHIR terminology capabilities provider.
   *
   * @param theContext the context
   * @param theServerConfiguration the server configuration
   */
  public FhirTerminologyCapabilitiesProviderR5(
      final FhirContext theContext, final RestfulServerConfiguration theServerConfiguration) {
    super(theContext, theServerConfiguration);
  }

  /**
   * Instantiates a new FHIR terminology capabilities provider.
   *
   * @param theRestfulServer the restful server
   * @param theSearchParamRegistry the search param registry
   * @param theValidationSupport the validation support
   */
  public FhirTerminologyCapabilitiesProviderR5(
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
  @Metadata(cacheMillis = 0)
  public IBaseConformance getMetadataResource(
      final HttpServletRequest request, final RequestDetails requestDetails) {
    if (request.getParameter("mode") != null
        && request.getParameter("mode").equals("terminology")) {
      return new FhirTerminologyCapabilitiesR5().withDefaults();
    } else {
      IBaseConformance ibc = super.getServerConformance(request, requestDetails);
      CapabilityStatement capabilityStatement = (CapabilityStatement) ibc;
      CanonicalType instantiateUri = new CanonicalType("http://hl7.org/fhir/CapabilityStatement/terminology-server");

      capabilityStatement.setInstantiates(Collections.singletonList(instantiateUri));
      
	  capabilityStatement.setSoftware(new CapabilityStatement.CapabilityStatementSoftwareComponent()
				.setName("EVSRESTAPI FHIR Terminology Server Software").setVersion("2.2.0.RELEASE").setReleaseDate(new Date()));
      
	// First Extension
      Extension featureExtension1 = new Extension("http://hl7.org/fhir/uv/application-feature/StructureDefinition/feature");

      Extension definition1 = new Extension("definition", new CanonicalType("http://hl7.org/fhir/uv/tx-tests/FeatureDefinition/test-version"));
      Extension value1 = new Extension("value", new CodeType("1.7.5"));

      featureExtension1.addExtension(definition1);
      featureExtension1.addExtension(value1);

      capabilityStatement.addExtension(featureExtension1);
      
      Extension featureExtension2 = new Extension("http://hl7.org/fhir/uv/application-feature/StructureDefinition/feature");

      Extension definition2 = new Extension("definition", new CanonicalType("http://hl7.org/fhir/uv/tx-ecosystem/FeatureDefinition/CodeSystemAsParameter"));
      Extension value2 = new Extension("value", new BooleanType("true"));

      featureExtension2.addExtension(definition2);
      featureExtension2.addExtension(value2);

      capabilityStatement.addExtension(featureExtension2);
      
      capabilityStatement.setUrl("https://api-evsrest.nci.nih.gov/fhir/r5/metadata");
      capabilityStatement.setVersion("2.2.0.RELEASE");
      capabilityStatement.setName("EVSRESTAPIFHIRTerminologyServer");
      capabilityStatement.setTitle("EVSRESTAPI FHIR Terminology Server");
      capabilityStatement.setStatus(Enumerations.PublicationStatus.ACTIVE);
      capabilityStatement.setExperimental(true);
      capabilityStatement.setPublisher("NCI EVS");
      capabilityStatement.setDate(new Date());
      
      
      // Find the "rest" component with mode = "server"
      Optional<CapabilityStatementRestComponent> serverRestComponent =
              capabilityStatement.getRest().stream()
                  .filter(rest -> "server".equals(rest.getMode().toString().toLowerCase()
                		  ))
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
            new Coding("http://terminology.hl7.org/CodeSystem/restful-security-service", "OAuth2.0", "OAuth 2.0")); // Example coding

        // Set the "service" property
        security.setService(Collections.singletonList(serviceCodeableConcept));
        
        CapabilityStatementRestResourceOperationComponent versionsOperation =
                new CapabilityStatementRestResourceOperationComponent()
                    .setName("versions")
                    .setDefinition("http://hl7.org/fhir/OperationDefinition/-versions"); 
            rest.addOperation(versionsOperation);
      }
      return capabilityStatement;
    }
  }
}
