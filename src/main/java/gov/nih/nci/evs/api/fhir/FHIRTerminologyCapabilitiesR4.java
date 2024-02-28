package gov.nih.nci.evs.api.fhir;

import ca.uhn.fhir.model.api.annotation.ChildOrder;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import java.util.Collections;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.TerminologyCapabilities;

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
public class FHIRTerminologyCapabilitiesR4 extends TerminologyCapabilities
    implements IBaseConformance {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /**
   * With defaults.
   *
   * @return the FHIR terminology capabilities
   */
  public FHIRTerminologyCapabilitiesR4 withDefaults() {
    setContact();
    setCodeSystem();
    setName("EVSRESTAPITerminologyCapabilities");
    setStatus(PublicationStatus.DRAFT);
    setTitle("EVSRESTAPI Terminology Capability Statement");
    setVersion(getClass().getPackage().getImplementationVersion());
    return this;
  }

  /** Sets the code system. */
  private void setCodeSystem() {
    final TerminologyCapabilitiesCodeSystemComponent tccsc =
        new TerminologyCapabilitiesCodeSystemComponent();
    tccsc.setUri("https://api-evsrest.nci.nih.gov");
    setCodeSystem(Collections.singletonList(tccsc));
  }

  /** Sets the contact. */
  private void setContact() {
    final ContactPoint contactPoint = new ContactPoint();
    contactPoint.setSystem(ContactPointSystem.EMAIL);
    final ContactDetail contactDetail = new ContactDetail();
    contactDetail.addTelecom(contactPoint);
    setContact(Collections.singletonList(contactDetail));
  }
}
