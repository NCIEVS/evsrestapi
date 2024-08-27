package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.model.api.annotation.ChildOrder;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import java.util.Collections;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r5.model.ContactDetail;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.TerminologyCapabilities;

/** Specification of the FHIR TerminologyCapabilities */
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

  /**
   * With defaults.
   *
   * @return the FHIR terminology capabilities
   */
  public FhirTerminologyCapabilitiesR5 withDefaults() {
    setContact();
    setCodeSystem();
    setName("EVSRESTAPITerminologyCapabilities");
    setStatus(Enumerations.PublicationStatus.DRAFT);
    setTitle("EVSRESTAPI Terminology Capability Statement");
    setVersion(getClass().getPackage().getImplementationVersion());
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
