package gov.nih.nci.evs.api.configuration;

import org.hl7.fhir.instance.model.api.IBaseConformance;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

public class EvsResponseHighlighterInterceptor extends ResponseHighlighterInterceptor {

	/**
	 * Constructor
	 */
	public EvsResponseHighlighterInterceptor() {
		super();
		System.out.println("in EvsResponseHighlighterInterceptor");
	}
	
	@Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
	public void capabilityStatementGenerated(
			RequestDetails theRequestDetails, IBaseConformance theCapabilityStatement) {
		System.out.println("EvsResponseHighligherInterceptor.capabilityStatementGenerated");
//		FhirTerser terser = theRequestDetails.getFhirContext().newTerser();
//
//		Set<String> formats = terser.getValues(theCapabilityStatement, "format", IPrimitiveType.class).stream()
//				.map(t -> t.getValueAsString())
//				.collect(Collectors.toSet());
//		addFormatConditionally(
//				theCapabilityStatement, terser, formats, Constants.CT_FHIR_JSON_NEW, Constants.FORMATS_HTML_JSON);
//		addFormatConditionally(
//				theCapabilityStatement, terser, formats, Constants.CT_FHIR_XML_NEW, Constants.FORMATS_HTML_XML);
//		addFormatConditionally(
//				theCapabilityStatement, terser, formats, Constants.CT_RDF_TURTLE, Constants.FORMATS_HTML_TTL);
	}
}
