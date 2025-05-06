package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.model.api.annotation.ChildOrder;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import gov.nih.nci.evs.api.controller.StaticContextAccessor;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.util.TerminologyUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.ContactDetail;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.TerminologyCapabilities;
import org.springframework.beans.factory.annotation.Autowired;

/** Specification of the FHIR TerminologyCapabilities. */
@ResourceDef(
    name = "TerminologyCapabilities",
    profile = "http://hl7.org/fhir/StructureDefinition/TerminologyCapabilities")
@ChildOrder(
    names = {
      "url",
      "version",
      "name",
      "title",
      "status",
      "experimental",
      "date",
      "publisher",
      "contact",
      "description",
      "useContext",
      "jurisdiction",
      "purpose",
      "copyright",
      "kind",
      "software",
      "implementation",
      "lockedDate",
      "codeSystem",
      "expansion",
      "codeSearch",
      "validateCode",
      "translation",
      "closure"
    })
public class FhirTerminologyCapabilitiesR5 extends TerminologyCapabilities
    implements IBaseConformance {
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** the query service. */
  @Autowired ElasticQueryService esQueryService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;
  
  /**
   * With defaults.
   *
   * @return the FHIR terminology capabilities
   */
  public FhirTerminologyCapabilitiesR5 withDefaults() {
    setContact();
    //setCodeSystem();
    setName("EVSRESTAPITerminologyCapabilities");
    setStatus(Enumerations.PublicationStatus.DRAFT);
    setTitle("EVSRESTAPI Terminology Capability Statement");
    setVersion(getClass().getPackage().getImplementationVersion());
    this.setExperimental(true);
    this.setPublisher("NCI EVS");
    Meta meta = new Meta();
    meta.addProfile("http://hl7.org/fhir/StructureDefinition/TerminologyCapabilities");
    this.setMeta(meta);
    
    // Get the indexed terms

    List<Terminology> terms;
    try {
      ElasticQueryService esQueryService = StaticContextAccessor.getBean(ElasticQueryService.class);
      TerminologyUtils termUtils = StaticContextAccessor.getBean(TerminologyUtils.class);

      terms = termUtils.getIndexedTerminologies(esQueryService);

     
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Find the matching code systems in the list of terms
    for (final Terminology terminology : terms) {
      final CodeSystem cs = FhirUtilityR5.toR5(terminology);
      System.out.println("CodeSystem: " + cs.getName());
      TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent tccsc =
    	        new TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent();
      tccsc.setUri(cs.getUrl());
      TerminologyCapabilities.TerminologyCapabilitiesCodeSystemVersionComponent vc =
  	        new TerminologyCapabilities.TerminologyCapabilitiesCodeSystemVersionComponent();
      vc.setCode(cs.getVersion());
      vc.setIsDefault(true);
      vc.setCompositional(false);
      tccsc.setVersion(Collections.singletonList(vc));
      this.addCodeSystem(tccsc);
    }  
    return this;
  }

  /** Sets the code system. */
  private void setCodeSystem() {
    final TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent tccsc =
        new TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent();
    tccsc.setUri("https://api-evsrest.nci.nih.gov");
    setCodeSystem(Collections.singletonList(tccsc));
  }

  /** Sets the contact. */
  private void setContact() {
    final ContactPoint contactPoint = new ContactPoint();
    contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
    final ContactDetail contactDetail = new ContactDetail();
    contactDetail.addTelecom(contactPoint);
    setContact(Collections.singletonList(contactDetail));
  }
}
