package gov.nih.nci.evs.api.fhir.R4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerConfiguration;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseConformance;

/**
 * See https://www.hl7.org/fhir/terminologycapabilities.html See
 * https://smilecdr.com/hapi-fhir/docs/server_plain/introduction.html#capability-statement-server-metadata
 * Call using GET [base]/metadata?mode=terminology See
 * https://github.com/jamesagnew/hapi-fhir/issues/1681
 */
public class FHIRTerminologyCapabilitiesProviderR4 extends ServerCapabilityStatementProvider {

  /**
   * Instantiates a new FHIR terminology capabilities provider.
   *
   * @param theServer the the server
   */
  public FHIRTerminologyCapabilitiesProviderR4(final RestfulServer theServer) {
    super(theServer);
  }

  /**
   * Instantiates a new FHIR terminology capabilities provider.
   *
   * @param theContext the the context
   * @param theServerConfiguration the the server configuration
   */
  public FHIRTerminologyCapabilitiesProviderR4(
      final FhirContext theContext, final RestfulServerConfiguration theServerConfiguration) {
    super(theContext, theServerConfiguration);
  }

  /**
   * Instantiates a new FHIR terminology capabilities provider.
   *
   * @param theRestfulServer the the restful server
   * @param theSearchParamRegistry the the search param registry
   * @param theValidationSupport the the validation support
   */
  public FHIRTerminologyCapabilitiesProviderR4(
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
  @Metadata
  public IBaseConformance getMetadataResource(
      final HttpServletRequest request, final RequestDetails requestDetails) {
    if (request.getParameter("mode") != null
        && request.getParameter("mode").equals("terminology")) {
      return new FHIRTerminologyCapabilitiesR4().withDefaults();
    } else {
      return super.getServerConformance(request, requestDetails);
    }
  }
}
