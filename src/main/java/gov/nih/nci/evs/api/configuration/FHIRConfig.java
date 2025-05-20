package gov.nih.nci.evs.api.configuration;

import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ApacheProxyAddressStrategy;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import gov.nih.nci.evs.api.fhir.R4.HapiR4RestfulServlet;
import gov.nih.nci.evs.api.fhir.R5.HapiR5RestfulServlet;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.TerminologyCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Servlet registration bean. */
@Configuration
public class FHIRConfig {


	private final BuildProperties buildProperties;

	public FHIRConfig(@Autowired(required = false) BuildProperties buildProperties) {
		this.buildProperties = buildProperties;
		//this.codeSystemRepository = codeSystemRepository;
	}
	
  /**
   * Hapi R4.
   *
   * @return the servlet registration bean
   */
  @Bean
  public ServletRegistrationBean<HapiR4RestfulServlet> hapiR4() {
    final HapiR4RestfulServlet hapiServlet = new HapiR4RestfulServlet();

    final ServletRegistrationBean<HapiR4RestfulServlet> servletRegistrationBean =
        new ServletRegistrationBean<>(hapiServlet, "/fhir/r4/*");
    hapiServlet.setServerName("EVSRESTAPI R4 FHIR Terminology Server");
    hapiServlet.setServerVersion(getClass().getPackage().getImplementationVersion());
    hapiServlet.setDefaultResponseEncoding(EncodingEnum.JSON);

    // Use apache proxy address strategy
    hapiServlet.setServerAddressStrategy(new ApacheProxyAddressStrategy(true));

    final ResponseHighlighterInterceptor interceptor = new ResponseHighlighterInterceptor();
    hapiServlet.registerInterceptor(interceptor);

    return servletRegistrationBean;
  }

  /**
   * Hapi R5 servlet registration
   *
   * @return the servlet registration bean
   */
  @Bean
  public ServletRegistrationBean<HapiR5RestfulServlet> hapiR5() {
    final HapiR5RestfulServlet hapiServlet = new HapiR5RestfulServlet();
    final ServletRegistrationBean<HapiR5RestfulServlet> servletRegistrationBean =
        new ServletRegistrationBean<>(hapiServlet, "/fhir/r5/*");
    hapiServlet.setServerName("EVSRESTAPI R5 FHIR Terminology Server");
    //hapiServlet.setServerVersion(getClass().getPackage().getImplementationVersion());
    hapiServlet.setDefaultResponseEncoding(EncodingEnum.JSON);
    
    // workaround for "HAPI-1700: Unknown child name 'format' in element" error
	hapiServlet.registerInterceptor(new EvsResponseHighlighterInterceptor() {

		@Override
		public void capabilityStatementGenerated(RequestDetails theRequestDetails, IBaseConformance theCapabilityStatement) {
			if (!(theCapabilityStatement instanceof TerminologyCapabilities)) {
				System.out.println("test1");
				super.capabilityStatementGenerated(theRequestDetails, theCapabilityStatement);
			}
		}
	});


	//hapiServlet.registerInterceptor(new CapabilityStatementCustomizer());

    return servletRegistrationBean;
  }
}
