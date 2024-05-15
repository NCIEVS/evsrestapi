package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/** Implementation class for the terminology suggestion form service. */
@Service
public class TermSuggestionFormServiceImpl implements TermSuggestionFormService {
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormServiceImpl.class);

  // JavaMailSender
  private final JavaMailSender mailSender;

  // The application properties
  private final ApplicationProperties applicationProperties;

  // path for the form file
  URL formFilePath;

  /**
   * Constructor: Instantiates dependencies.
   *
   * @param mailSender java mail sender
   */
  public TermSuggestionFormServiceImpl(
      JavaMailSender mailSender, ApplicationProperties applicationProperties) {
    this.mailSender = mailSender;
    this.applicationProperties = applicationProperties;
  }

  /**
   * Gets the term suggestion form based on the formType, reads the json, and returns the template
   * as JsonObject
   *
   * @param formType form template to load
   * @return JsonNode
   * @throws IOException io exception
   */
  public JsonNode getFormTemplate(String formType) throws IllegalArgumentException, IOException {
    // Set the form file path based on the formType passed. If we receive an invalid path, throw
    // exception
    if (formType == null || formType.isEmpty() || formType.isBlank()) {
      throw new IllegalArgumentException("Invalid form template provided");
    } else {
      formFilePath = new URL(applicationProperties.getConfigBaseUri() + "/" + formType + ".json");
    }

    // Create objectMapper. Read file and return JsonNode
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(formFilePath);
  }

  /**
   * Sends an email
   *
   * @param emailDetails details of the email created from the form data
   */
  public void sendEmail(EmailDetails emailDetails) throws MessagingException {
    // Check if starttls.enable is false
    if (mailSender instanceof JavaMailSenderImpl mailSenderImpl) {
      Properties mailProperties = mailSenderImpl.getJavaMailProperties();
      String starttls = mailProperties.getProperty("mail.smtp.starttls.enable");
      if ("false".equals(starttls)) {
        return; // do nothing
      }
    }

    // Create the MimeMessage
    MimeMessage message = mailSender.createMimeMessage();

    logger.info(
        "   Sending email for {} form to {}", emailDetails.getSource(), emailDetails.getToEmail());

    // Set the email details
    message.setRecipients(MimeMessage.RecipientType.TO, emailDetails.getToEmail());
    message.setFrom(new InternetAddress(emailDetails.getFromEmail()));
    message.setSubject(emailDetails.getSubject());
    if (emailDetails.getMsgBody().contains("<html")) {
      message.setContent(emailDetails.getMsgBody(), "text/hmtl; charset=utf-8");
    } else {
      message.setText(emailDetails.getMsgBody());
    }

    mailSender.send(message);
  }
}
