package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.model.api.annotation.ChildOrder;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import gov.nih.nci.evs.api.controller.StaticContextAccessor;
import gov.nih.nci.evs.api.controller.VersionController;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.ContactDetail;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Enumerations.CapabilityStatementKind;
import org.hl7.fhir.r5.model.Meta;
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
  @Autowired OpensearchQueryService esQueryService;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /**
   * With defaults.
   *
   * @return the FHIR terminology capabilities
   */
  public FhirTerminologyCapabilitiesR5 withDefaults() {
    setName("EVSRESTAPITerminologyCapabilities");
    setStatus(Enumerations.PublicationStatus.DRAFT);
    setTitle("EVSRESTAPI Terminology Capability Statement");
    setVersion(VersionController.VERSION);
    setDate(new java.util.Date());
    setPurpose(
        "The EVS REST API Terminology Capability Statement provides a summary of the EVS REST API's"
            + " capabilities.");
    setKind(CapabilityStatementKind.CAPABILITY);
    setSoftware(
        new TerminologyCapabilitiesSoftwareComponent()
            .setName("EVSRESTAPI")
            .setVersion(VersionController.VERSION));
    this.setExperimental(true);
    this.setPublisher("NCI EVS");
    Meta meta = new Meta();
    meta.addProfile("http://hl7.org/fhir/StructureDefinition/TerminologyCapabilities");
    this.setMeta(meta);

    setCodeSystems();
    setContact();
    setExpansion();

    return this;
  }

  /** Sets the expansion. */
  private void setExpansion() {

    TerminologyCapabilitiesExpansionComponent expansion =
        new TerminologyCapabilitiesExpansionComponent();
    expansion.setHierarchical(false);
    expansion.setPaging(true);
    expansion.setTextFilter(
        "Matching is word-prefix, any-order across all designations and the code itself.\n"
            + "Codes are returned in best-match-first order.");

    // Add parameters
    expansion.addParameter(new TerminologyCapabilitiesExpansionParameterComponent().setName("url"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("valueSet"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("valueSetVersion"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent()
            .setName("filter")
            .setDocumentation(
                "Matching is word-prefix, any-order across all designations and the code itself."));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("profile"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("date"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("context"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("offset"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("count"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("activeOnly"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("includeDesignations"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("includeDefinition"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("system-version"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("force-system-version"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent()
            .setName("property")
            .setDocumentation("Pre-adopted from R5."));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent()
            .setName("useSupplement")
            .setDocumentation("Pre-adopted from R5."));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("displayLanguage"));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent()
            .setName("excludeNested")
            .setDocumentation(
                "Soft-defaults to true. Only returns nested results when a stored expansion is"
                    + " being use and there is no `filter` parameter."));
    expansion.addParameter(
        new TerminologyCapabilitiesExpansionParameterComponent().setName("tx-resource"));

    this.setExpansion(expansion);
  }

  /** Sets the code systems. */
  private void setCodeSystems() {

    List<Terminology> terms;
    try {
      OpensearchQueryService esQueryService =
          StaticContextAccessor.getBean(OpensearchQueryService.class);
      TerminologyUtils termUtils = StaticContextAccessor.getBean(TerminologyUtils.class);

      terms = termUtils.getIndexedTerminologies(esQueryService);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Find the matching code systems in the list of terms
    for (final Terminology terminology : terms) {
      final CodeSystem cs = FhirUtilityR5.toR5(terminology);
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
