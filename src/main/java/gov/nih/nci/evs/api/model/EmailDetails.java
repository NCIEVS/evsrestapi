package gov.nih.nci.evs.api.model;

import lombok.Data;
import org.apache.jena.atlas.json.JsonObject;

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

  private final String nullError = "Fields cannot be null";

  //  private String attachment;

  /**
   * Create the email model from the submitted term form
   * @param formData submitted form data
   * @return an EmailDetails object
   * @throws Exception exception
   */
  public static EmailDetails generateEmailDetails(JsonObject formData) throws Exception {
    // validate formData is not empty
    if (formData.isEmpty() || formData.isNull()) {
      throw new Exception("Form data not found. Please check your form data.");
    } else {
      EmailDetails emailDetails = new EmailDetails();
      // Set the values from the form data
      String formName = formData.get("formName").getAsString().value();
      String recipientEmail = formData.get("recipientEmail").getAsString().value();
      String businessEmail = formData.get("businessEmail").getAsString().value();
      String subject = formData.get("subject").getAsString().value();
      String body = formData.get("body").getAsString().value();

      if (formName == null || formName.isEmpty()) {
        throw new Exception("form name cannot be null or empty");
      }
      if (recipientEmail == null || recipientEmail.isEmpty()) {
        throw new Exception("Recipient Email cannot be null or empty");
      }
      if (businessEmail == null || businessEmail.isEmpty()) {
        throw new Exception("Business Email cannot be null or empty");
      }
      if (subject == null || subject.isEmpty()) {
        throw new Exception("Subject cannot be null or empty");
      }
      if (body == null || body.isEmpty()) {
        throw new Exception("Message ody cannot be null or empty");
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
