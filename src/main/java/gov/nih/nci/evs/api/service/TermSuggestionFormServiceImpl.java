package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
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

  /** The object mapper to read the config url with readTree. */
  private final ObjectMapper mapper;

  /**
   * Constructor: Instantiates dependencies.
   *
   * @param mailSender java mail sender
   * @param applicationProperties the application properties
   * @param mapper the mapper
   */
  public TermSuggestionFormServiceImpl(
      final JavaMailSender mailSender,
      final ApplicationProperties applicationProperties,
      ObjectMapper mapper) {
    this.mailSender = mailSender;
    this.applicationProperties = applicationProperties;
    this.mapper = mapper;
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
      throws IllegalArgumentException, IOException, MalformedURLException {
    // Set the form file path based on the formType passed. If we receive an invalid path, throw
    // exception
    if (formType == null || formType.isEmpty() || formType.isBlank()) {
      throw new IllegalArgumentException("Invalid form template provided");
    } else {
      formFilePath = new URL(applicationProperties.getConfigBaseUri() + "/" + formType + ".json");
    }
    // Create objectMapper. Read file and return JsonNode
    JsonNode termForm = mapper.readTree(formFilePath);
    // Get the recaptcha_site_key from application properties
    String recaptchaSiteKey = applicationProperties.getRecaptchaSiteKey();

    // Check our termForm is an object node to safely add properties
    if (termForm.isObject()) {
      ((ObjectNode) termForm).put("recaptchaSiteKey", recaptchaSiteKey);
    } else {
      logger.error("Cannot add recaptcha site key. Form template is not a JSON object.");
      throw new IllegalArgumentException("Invalid form template.");
    }

    return termForm;
  }

  /**
   * Sends an email.
   *
   * @param emailDetails details of the email created from the form data
   * @throws MessagingException the messaging exception
   */
  @Override
  public void sendEmail(final EmailDetails emailDetails) throws MessagingException, Exception {
    // Check if starttls.enable is false
    if (mailSender instanceof JavaMailSenderImpl) {
      final Properties mailProperties = ((JavaMailSenderImpl) mailSender).getJavaMailProperties();
      final String starttls = mailProperties.getProperty("mail.smtp.starttls.enable");
      // check we want to start the tls
      if ("false".equals(starttls)) {
        throw new MessagingException(
            "Unable to start TLS to send on a secure connection, aborting send email");
      }
    }
    try {
      // Create the MimeMessage
      final MimeMessage message = mailSender.createMimeMessage();
      logger.info(
          "   Sending email for {} form to {}",
          emailDetails.getSource(),
          emailDetails.getToEmail());
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
    } catch (MessagingException e) {
      throw new MessagingException("Failed to send email, {}", e);
    }
  }
}
