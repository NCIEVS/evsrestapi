package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.mail.Message.RecipientType;
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
  /** The Constant logger. */
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormServiceImpl.class);

  /** The mail sender. */
  // JavaMailSender
  private final JavaMailSender mailSender;

  /** The application properties. */
  // The application properties
  private final ApplicationProperties applicationProperties;

  /** The form file path. */
  // path for the form file
  URL formFilePath;

  /**
   * Constructor: Instantiates dependencies.
   *
   * @param mailSender java mail sender
   * @param applicationProperties the application properties
   */
  public TermSuggestionFormServiceImpl(
      final JavaMailSender mailSender, final ApplicationProperties applicationProperties) {
    this.mailSender = mailSender;
    this.applicationProperties = applicationProperties;
  }

  /**
   * Gets the term suggestion form based on the formType, reads the json, and returns the template
   * as JsonObject.
   *
   * @param formType form template to load
   * @return JsonNode
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IOException io exception
   */
  @Override
  public JsonNode getFormTemplate(final String formType)
      throws IllegalArgumentException, IOException {
    // Set the form file path based on the formType passed. If we receive an invalid path, throw
    // exception
    if (formType == null || formType.isEmpty() || formType.isBlank()) {
      throw new IllegalArgumentException("Invalid form template provided");
    } else {
      formFilePath = new URL(applicationProperties.getConfigBaseUri() + "/" + formType + ".json");
    }
    // Create objectMapper. Read file and return JsonNode
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(formFilePath);
  }

  /**
   * Sends an email.
   *
   * @param emailDetails details of the email created from the form data
   * @throws MessagingException the messaging exception
   */
  @Override
  public void sendEmail(final EmailDetails emailDetails) throws MessagingException {
    // Check if starttls.enable is false
    if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
      final Properties mailProperties = javaMailSender.getJavaMailProperties();
      final String starttls = mailProperties.getProperty("mail.smtp.starttls.enable");
      // check we want to start the tls
      if ("false".equals(starttls)) {
        return; // do nothing
      }
    }
    // Create the MimeMessage
    final MimeMessage message = mailSender.createMimeMessage();
    logger.info(
        "   Sending email for {} form to {}", emailDetails.getSource(), emailDetails.getToEmail());
    // Set the email details
    message.setRecipients(RecipientType.TO, emailDetails.getToEmail());
    message.setFrom(new InternetAddress(emailDetails.getFromEmail()));
    message.setSubject(emailDetails.getSubject());
    if (emailDetails.getMsgBody().contains("<html")) {
      message.setContent(emailDetails.getMsgBody(), "text/html; charset=utf-8");
    } else {
      message.setText(String.valueOf(emailDetails.getMsgBody()));
    }
    mailSender.send(message);
  }
}
