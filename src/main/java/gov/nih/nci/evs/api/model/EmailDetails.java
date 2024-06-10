package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * EmailDetails model, created from a JsonObject data form. This allows us to handle the form data
 * and extract information we need to create the email more easily
 */
// BAC: for now back away from this
// @Data
public class EmailDetails extends BaseModel {

  /** The source. */
  private String source;

  /** The from email. */
  private String fromEmail;

  /** The to email. */
  private String toEmail;

  /** The subject. */
  private String subject;

  /** The msg body. */
  private String msgBody;

  /**
   * Returns the source.
   *
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the source.
   *
   * @param source the source
   */
  public void setSource(final String source) {
    this.source = source;
  }

  /**
   * Returns the from email.
   *
   * @return the from email
   */
  public String getFromEmail() {
    return fromEmail;
  }

  /**
   * Sets the from email.
   *
   * @param fromEmail the from email
   */
  public void setFromEmail(final String fromEmail) {
    this.fromEmail = fromEmail;
  }

  /**
   * Returns the to email.
   *
   * @return the to email
   */
  public String getToEmail() {
    return toEmail;
  }

  /**
   * Sets the to email.
   *
   * @param toEmail the to email
   */
  public void setToEmail(final String toEmail) {
    this.toEmail = toEmail;
  }

  /**
   * Returns the subject.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Sets the subject.
   *
   * @param subject the subject
   */
  public void setSubject(final String subject) {
    this.subject = subject;
  }

  /**
   * Returns the msg body.
   *
   * @return the msg body
   */
  public String getMsgBody() {
    return msgBody;
  }

  /**
   * Sets the msg body.
   *
   * @param msgBody the msg body
   */
  public void setMsgBody(final String msgBody) {
    this.msgBody = msgBody;
  }

  /**
   * Returns the nullerror.
   *
   * @return the nullerror
   */
  public static String getNullerror() {
    return nullError;
  }

  /** The Constant nullError. */
  private static final String nullError = "Fields cannot be null";

  /**
   * Create the email model from the submitted term form.
   *
   * @param formData submitted form data
   * @return an EmailDetails object
   * @throws Exception exception
   */
  public static EmailDetails generateEmailDetails(final JsonNode formData) throws Exception {
    // validate formData is not empty
    if (formData.isEmpty() || formData.isNull()) {
      throw new Exception("Form data not found. Please check your form data.");
    } else {
      final EmailDetails emailDetails = new EmailDetails();
      // Set the values from the form data
      final String formName = formData.get("formName").textValue();
      final String recipientEmail = formData.get("recipientEmail").textValue();
      final String businessEmail = formData.get("businessEmail").textValue();
      final String subject = formData.get("subject").textValue();
      // format the json object to a string
      final String body = formatBody(formData.get("body"));

      if (formName == null
          || formName.isEmpty()
          || recipientEmail == null
          || recipientEmail.isEmpty()) {
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

      // populate the emailDetails
      emailDetails.setSource(formName);
      emailDetails.setToEmail(recipientEmail);
      emailDetails.setFromEmail(businessEmail);
      emailDetails.setSubject(subject);
      emailDetails.setMsgBody(body);

      return emailDetails;
    }
  }

  /**
   * Format the body of the email.
   *
   * @param body JsonNode body of the email
   * @return formatted string of the body
   */
  public static String formatBody(JsonNode body) {
    StringBuilder formattedBody = new StringBuilder();
    formatBodyRecursive(body, formattedBody, "");
    return formattedBody.toString();
  }

  /**
   * Recursive helper function to format the body of the email.
   *
   * @param body the JsonNode body of the email
   * @param formattedBody the formatted body we are building
   * @param indent the indent between key/values
   */
  private static void formatBodyRecursive(
      JsonNode body, StringBuilder formattedBody, String indent) {
    if (body.isObject()) {
      ObjectNode object = (ObjectNode) body;
      object
          .fields()
          .forEachRemaining(
              entry -> {
                String key = entry.getKey();
                // Skip processing if the key is 'recaptcha'
                if (key.equals("recaptcha")) {
                  return;
                }
                String capitalizedKey = Character.toUpperCase(key.charAt(0)) + key.substring(1);
                // check we are on the root node
                if (indent.isEmpty()) {
                  // we are in the root node and want to print the key
                  formattedBody.append(indent).append(capitalizedKey).append(":\n");
                  formatBodyRecursive(entry.getValue(), formattedBody, indent + "  ");
                } else {
                  // we are in the field and want both key/value pairs
                  formattedBody.append(indent).append(capitalizedKey).append(": ");
                  formatBodyRecursive(entry.getValue(), formattedBody, indent);
                }
              });
    } else if (body.isArray()) {
      // check if we have an array
      for (JsonNode field : body) {
        formatBodyRecursive(field, formattedBody, indent + "  ");
      }
    } else {
      // we are at the leaf node
      formattedBody.append(indent).append(body.asText()).append("\n");
    }
  }
}
