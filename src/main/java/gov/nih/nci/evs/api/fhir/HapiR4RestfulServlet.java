package gov.nih.nci.evs.api.fhir;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.RestfulServer;

/**
 * The Hapi servlet itself.
 */
public class HapiR4RestfulServlet extends RestfulServer {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(HapiR4RestfulServlet.class);

  /**
   * The initialize method is automatically called when the servlet is starting up, so it can be
   * used to configure the servlet to define resource providers, or set up configuration,
   * interceptors, etc.
   *
   * @throws ServletException the servlet exception
   */
  @Override
  protected void initialize() throws ServletException {

    setDefaultResponseEncoding(EncodingEnum.JSON);

    final FhirContext fhirContext = FhirContext.forR4();
    final LenientErrorHandler delegateHandler = new LenientErrorHandler();
    fhirContext.setParserErrorHandler(new StrictErrorHandler() {
      @Override
      public void unknownAttribute(final IParseLocation theLocation,
        final String theAttributeName) {
        delegateHandler.unknownAttribute(theLocation, theAttributeName);
      }

      @Override
      public void unknownElement(final IParseLocation theLocation, final String theElementName) {
        delegateHandler.unknownElement(theLocation, theElementName);
      }

      @Override
      public void unknownReference(final IParseLocation theLocation, final String theReference) {
        delegateHandler.unknownReference(theLocation, theReference);
      }
    });
    setFhirContext(fhirContext);

    /*
     * The servlet defines any number of resource providers, and configures itself to use them by
     * calling setResourceProviders()
     */
    final WebApplicationContext applicationContext =
        WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());

    setResourceProviders(applicationContext.getBean(CodeSystemProviderR4.class),
        applicationContext.getBean(ValueSetProviderR4.class),
        applicationContext.getBean(ConceptMapProviderR4.class));

    setServerConformanceProvider(new FHIRTerminologyCapabilitiesProviderR4(this));

    // Register interceptors
    registerInterceptor(new EVSAPIOpenApiInterceptorR4());

    logger.info("FHIR Resource providers and interceptors registered");
  }
}
