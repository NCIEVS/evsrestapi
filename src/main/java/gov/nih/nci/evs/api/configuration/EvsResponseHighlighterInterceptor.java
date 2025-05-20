package gov.nih.nci.evs.api.configuration;

import org.hl7.fhir.instance.model.api.IBaseConformance;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

/** "HAPI-1700: Unknown child name 'format' in element" error */
public class EvsResponseHighlighterInterceptor extends ResponseHighlighterInterceptor {

	/**
	 * Constructor
	 */
	public EvsResponseHighlighterInterceptor() {
		super();
	}
	
	// override this method in ResponseHighlighterInterceptor in order to avoid the
	// "HAPI-1700: Unknown child name 'format' in element" error
	// avoid the FhirTerser format assignments
	@Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
	public void capabilityStatementGenerated(
			RequestDetails theRequestDetails, IBaseConformance theCapabilityStatement) {
		System.out.println("test2");
	}
}
