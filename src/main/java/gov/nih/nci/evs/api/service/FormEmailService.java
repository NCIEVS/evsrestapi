package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.EmailDetails;

public interface FormEmailService {
  /**
   * Sends an email with the formatted form data, handled by the javaMailSender
   *
   * @param emailDetails details of the email created from the form data
   */
  void sendEmail(EmailDetails emailDetails);

  //  /**
  //   * Send an email with the formatted form data and an attachment, handled by javaMailSender
  //   * @param formData
  //   * @param formType
  //   */
  //  void sendEmailWithAttachment(JsonObject formData, String formType);
}
