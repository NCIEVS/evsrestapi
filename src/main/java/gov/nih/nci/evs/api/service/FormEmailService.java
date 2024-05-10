package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.model.EmailDetails;

public interface FormEmailService {
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
  boolean sendEmail(EmailDetails emailDetails);

  //  /**
  //   * Send an email with the formatted form data and an attachment, handled by javaMailSender
  //   * @param formData
  //   * @param formType
  //   */
  //  void sendEmailWithAttachment(JsonObject formData, String formType);
}
