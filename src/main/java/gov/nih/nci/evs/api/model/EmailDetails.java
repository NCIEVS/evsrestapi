package gov.nih.nci.evs.api.model;

import lombok.Data;
import org.apache.jena.atlas.json.JsonObject;

/**
 * EmailDetails model, created from a JsonObject data form. This allows us to handle the form data
 * and extract information we need to create the email more easily
 */
@Data
public class EmailDetails {
  private String subject;
  private String msgBody;
  private String fromEmail;
  private String toEmail;
  private String source;

  //  private String attachment;

  public void generateEmailDetails(JsonObject formData) {
    // validate formData is not empty

    EmailDetails emailDetails = new EmailDetails();
    // Set the subject and fromEmail values from the form data
    emailDetails.setSubject(formData.get("subject").getAsString().value());
    emailDetails.setFromEmail(formData.get("businessEmail").getAsString().value());
    emailDetails.setToEmail(formData.get("recipientEmail").getAsString().value());
    emailDetails.setSource(formData.get("formName").getAsString().value());

    // Initialize msgBody as an empty string
    StringBuilder msgBody = new StringBuilder();

    // Iterate over the key in the json object
    for (String key : formData.keys()) {
      msgBody.append(key).append(": ").append(formData.get(key).getAsString().value()).append("\n");
    }

    // Set the msgBody of the emailDetails object
    emailDetails.setMsgBody(msgBody.toString());
  }
}
