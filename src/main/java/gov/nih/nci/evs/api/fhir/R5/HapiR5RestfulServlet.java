package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/** The FHIR R5 Hapi servlet. */
public class HapiR5RestfulServlet extends RestfulServer {
  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(HapiR5RestfulServlet.class);

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
    final FhirContext fhirContext = FhirContext.forR5();
    final LenientErrorHandler delegateHandler = new LenientErrorHandler();
    // Set the parser error handler
    fhirContext.setParserErrorHandler(
        new StrictErrorHandler() {

          /* see superclass */
          @Override
          public void unknownAttribute(
              final IParseLocation theLocation, final String theAttributeName) {
            delegateHandler.unknownAttribute(theLocation, theAttributeName);
          }

          /* see superclass */
          @Override
          public void unknownElement(
              final IParseLocation theLocation, final String theElementName) {
            delegateHandler.unknownElement(theLocation, theElementName);
          }

          /* see superclass */
          @Override
          public void unknownReference(
              final IParseLocation theLocation, final String theReference) {
            delegateHandler.unknownReference(theLocation, theReference);
          }
        });
    setFhirContext(fhirContext);

    // The servlet defines any number of resource providers, and configures itself to use them by
    // calling
    // setResourceProviders()
    final WebApplicationContext applicationContext =
        WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
    assert applicationContext != null;
    setResourceProviders(
        applicationContext.getBean(CodeSystemProviderR5.class),
        applicationContext.getBean(ValueSetProviderR5.class),
        applicationContext.getBean(ConceptMapProviderR5.class));

    setServerConformanceProvider(new FhirTerminologyCapabilitiesProviderR5(this));

    // Register interceptors
    registerInterceptor(new OpenApiInterceptorR5());

    logger.info("FHIR Resource providers and interceptors registered");
  }
}
