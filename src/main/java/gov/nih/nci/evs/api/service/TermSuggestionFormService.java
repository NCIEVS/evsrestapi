package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.model.EmailDetails;
import org.springframework.web.multipart.MultipartFile;

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

  /**
   * Validate file attachment.
   *
   * @param file the file
   * @return true, if successful
   */
  boolean validateFileAttachment(MultipartFile file);

  /**
   * Validate file attachment with form type.
   *
   * @param file the file
   * @param formType the form type (CDISC or NCIT)
   * @return true, if successful
   */
  boolean validateFileAttachment(MultipartFile file, String formType);

  /**
   * Sends an email with the formatted form data and an optional attachment
   *
   * @param emailDetails details of the email created from the form data
   * @param file optional file attachment
   */
  void sendEmailWithAttachment(EmailDetails emailDetails, MultipartFile file) throws Exception;
}
