package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * EmailDetails model, created from a JsonObject data form. This allows us to handle the form data
 * and extract information we need to create the email more easily
 */
@Data
public class EmailDetails {
  private String source;
  private String fromEmail;
  private String toEmail;
  private String subject;
  private String msgBody;

  private static final String nullError = "Fields cannot be null";

  /**
   * Create the email model from the submitted term form
   *
   * @param formData submitted form data
   * @return an EmailDetails object
   * @throws Exception exception
   */
  public static EmailDetails generateEmailDetails(JsonNode formData) throws Exception {
    // validate formData is not empty
    if (formData.isEmpty() || formData.isNull()) {
      throw new Exception("Form data not found. Please check your form data.");
    } else {
      EmailDetails emailDetails = new EmailDetails();
      // Set the values from the form data
      String formName = formData.get("formName").textValue();
      String recipientEmail = formData.get("recipientEmail").textValue();
      String businessEmail = formData.get("businessEmail").textValue();
      String subject = formData.get("subject").textValue();
      String body = formData.get("body").textValue();

      if (formName == null || formName.isEmpty()) {
        throw new Exception(nullError);
      }
      if (recipientEmail == null || recipientEmail.isEmpty()) {
        throw new Exception(nullError);
      }
      if (businessEmail == null || businessEmail.isEmpty()) {
        throw new Exception(nullError);
      }
      if (subject == null || subject.isEmpty()) {
        throw new Exception(nullError);
      }
      if (body == null || body.isEmpty()) {
        throw new Exception(nullError);
      }

      emailDetails.setSource(formName);
      emailDetails.setToEmail(recipientEmail);
      emailDetails.setFromEmail(businessEmail);
      emailDetails.setSubject(subject);
      emailDetails.setMsgBody(body);

      return emailDetails;
    }
  }
}
