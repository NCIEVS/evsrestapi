package gov.nih.nci.evs.api.configuration;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.TerminologyCapabilities;

import java.util.List;

@Interceptor
public class CapabilityStatementCustomizer {

	//private final CodeSystemRepository codeSystemRepository;

	public CapabilityStatementCustomizer() {
		//this.codeSystemRepository = codeSystemRepository;
	}

	@Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
	public void customize(IBaseConformance theCapabilityStatement) {
		if (theCapabilityStatement instanceof CapabilityStatement capabilityStatement) {
			capabilityStatement.addInstantiates("http://hl7.org/fhir/CapabilityStatement/terminology-server");

			List<CapabilityStatement.CapabilityStatementRestComponent> rest = capabilityStatement.getRest();
			rest.get(0).setSecurity(new CapabilityStatement.CapabilityStatementRestSecurityComponent().addService(new CodeableConcept(
					new Coding("http://terminology.hl7.org/CodeSystem/restful-security-service", "Basic", "Basic Authentication"))));
		} else if (theCapabilityStatement instanceof TerminologyCapabilities terminologyCapabilities) {
			//FHIRCodeSystem codeSystem = codeSystemRepository.getCodeSystem();
			//if (codeSystem != null) {
				TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent codeSystemComponent = new TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent();
				codeSystemComponent.setUri("http://snomed.info/sct");
				codeSystemComponent.setSubsumption(true);
				codeSystemComponent.setSubsumption(true);
				codeSystemComponent.addVersion().setCode("http://snomed.info/sct/9000000000000207008/version/20250101").setIsDefault(true);
				terminologyCapabilities.addCodeSystem(codeSystemComponent);
			//}
		}
	}
}