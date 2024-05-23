package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.model.EmailDetails;

/** Interface for the terminology suggestion form services */
public interface TermSuggestionFormService {
  /**
   * Gets the term suggestion form based on the formType. reads the json and returns it as a String
   *
   * @param formType
   */
  JsonNode getFormTemplate(String formType) throws Exception;

  /**
   * Sends an email with the formatted form data, handled by the javaMailSender
   *
   * @param emailDetails details of the email created from the form data
   */
  void sendEmail(EmailDetails emailDetails) throws Exception;

  //  /**
  //   * Send an email with the formatted form data and an attachment, handled by javaMailSender
  //   * @param formData
  //   * @param formType
  //   */
  //  void sendEmailWithAttachment(JsonObject formData, String formType);
}
