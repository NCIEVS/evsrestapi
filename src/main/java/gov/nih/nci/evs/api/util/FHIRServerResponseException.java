package gov.nih.nci.evs.api.util;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

/** The Class FHIRServerResponseException. */
public class FHIRServerResponseException extends BaseServerResponseException {

  /**
   * Instantiates a new FHIR server response exception.
   *
   * @param theStatusCode the status code
   * @param theMessage the message
   * @param theBaseOperationOutcome the base operation outcome
   */
  public FHIRServerResponseException(
      final int theStatusCode,
      final String theMessage,
      final IBaseOperationOutcome theBaseOperationOutcome) {
    super(theStatusCode, theMessage, theBaseOperationOutcome);
  }

  /**
   * Instantiates a new FHIR server response exception.
   *
   * @param theStatusCode the status code
   * @param theMessage the message
   * @param theBaseOperationOutcome the base operation outcome
   * @param e the e
   */
  public FHIRServerResponseException(
      final int theStatusCode,
      final String theMessage,
      final IBaseOperationOutcome theBaseOperationOutcome,
      final Throwable e) {
    super(theStatusCode, theMessage, e, theBaseOperationOutcome);
  }
}
