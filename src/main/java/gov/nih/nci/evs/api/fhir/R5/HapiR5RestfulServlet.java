package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.ServletException;
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

    // setServerAddressStrategy(new
    // HardcodedServerAddressStrategy("http://localhost:8082/fhir/r5"));

    final FhirContext fhirContext = FhirContext.forR5();

    setFhirContext(fhirContext);

    // Get Spring application context
    final WebApplicationContext applicationContext =
        WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
    assert applicationContext != null;

    // Set resource providers using Spring beans
    setResourceProviders(
        applicationContext.getBean(CodeSystemProviderR5.class),
        applicationContext.getBean(ValueSetProviderR5.class),
        applicationContext.getBean(ConceptMapProviderR5.class));

    setServerConformanceProvider(new FhirMetadataProviderR5(this));

    registerInterceptor(new OpenApiInterceptorR5());

    logger.debug("FHIR Resource providers and interceptors registered");
  }
}
