package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

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
      final String body = generateHtmlEmailBody(formData.get("body"));

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
   * create the html body of the email.
   *
   * @param body JsonNode body of the email
   * @return formatted string of the body
   */
  public static String generateHtmlEmailBody(JsonNode body) {
    StringBuilder htmlBody = new StringBuilder("<html><head><style>");
    // append html styles
    htmlBody.append("body { font-size: 12px; }");
    htmlBody.append("h2 { font-size: 14px; }");
    htmlBody.append("ul, li { font-size: 14px; font-weight: normal }");
    htmlBody.append("</style></head><body>");

    // Iterate over each section in the body object
    body.fields()
        .forEachRemaining(
            section -> {
              String sectionName = section.getKey();
              JsonNode sectionNode = section.getValue();
              // Append section name as a header
              htmlBody.append("<h2>").append(sectionName).append("</h2");
              // Create a list format of eact subsection content with <ul>
              htmlBody.append("<ul>");
              sectionNode
                  .fields()
                  .forEachRemaining(
                      field -> {
                        String fieldName = field.getKey();
                        String fieldValue = field.getValue().asText();

                        // If the value is null, convert to an empty value
                        fieldValue = "null".equals(fieldValue) ? "" : fieldValue;

                        // Append each field as a list item
                        htmlBody
                            .append("<li>")
                            .append(fieldName)
                            .append(": ")
                            .append(fieldValue)
                            .append("</li>");
                      });
              htmlBody.append("</ul>");
            });

    htmlBody.append("</body></html>");
    return htmlBody.toString();
  }

  /* see superclass */
  @Override
  public int hashCode() {
    return Objects.hash(fromEmail, msgBody, source, subject, toEmail);
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EmailDetails other = (EmailDetails) obj;
    return Objects.equals(fromEmail, other.fromEmail)
        && Objects.equals(msgBody, other.msgBody)
        && Objects.equals(source, other.source)
        && Objects.equals(subject, other.subject)
        && Objects.equals(toEmail, other.toEmail);
  }
}
