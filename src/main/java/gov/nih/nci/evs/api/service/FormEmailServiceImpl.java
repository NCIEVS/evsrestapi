package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class FormEmailServiceImpl implements FormEmailService {
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(FormEmailServiceImpl.class);

  // JavaMailSender
  private final JavaMailSender javaMailSender;

  // The application properties
  private final ApplicationProperties applicationProperties;

  // path for the form file
  URL formFilePath;

  /**
   * Constructor: Instantiates a new Form email service with params.
   *
   * @param javaMailSender java mail sender
   */
  public FormEmailServiceImpl(
      JavaMailSender javaMailSender, ApplicationProperties applicationProperties) {
    this.javaMailSender = javaMailSender;
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
   * Sends an email with the formatted form data
   *
   * @param emailDetails details of the email created from the form data
   */
  public void sendEmail(EmailDetails emailDetails) throws MailSendException {
    SimpleMailMessage message = new SimpleMailMessage();
    logger.info(
        "Sending email for form: {} to {}", emailDetails.getSource(), emailDetails.getToEmail());

    // Set the email details
    message.setTo(emailDetails.getToEmail());
    message.setFrom(emailDetails.getFromEmail());
    message.setSubject(emailDetails.getSubject());
    message.setText(emailDetails.getMsgBody());

    javaMailSender.send(message);
  }
}
