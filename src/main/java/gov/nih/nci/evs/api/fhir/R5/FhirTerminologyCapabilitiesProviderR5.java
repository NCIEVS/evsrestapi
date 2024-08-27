package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerConfiguration;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import gov.nih.nci.evs.api.fhir.R4.FHIRTerminologyCapabilitiesR4;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseConformance;

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
