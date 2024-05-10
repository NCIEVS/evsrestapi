package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.EmailDetails;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

  // Config base uri
  @Value("nci.evs.application.configBaseUri")
  String configBaseUri;

  /**
   * Constructor: Instantiates a new Form email service with params.
   *
   * @param javaMailSender java mail sender
   */
  public FormEmailServiceImpl(JavaMailSender javaMailSender) {
    this.javaMailSender = javaMailSender;
  }

  /**
   * Gets the term suggestion form based on the formType, reads the json, and returns the template
   * as JsonObject
   *
   * @param formType form template to load
   * @return JsonNode
   * @throws Exception
   */
  public JsonNode getFormTemplate(String formType) throws IOException {
    // Create file path from config base uri and form type
    String formFilePath = configBaseUri + "/" + formType + ".json";

    // Create objectMapper. Read file and convert to JsonNode
    ObjectMapper mapper = new ObjectMapper();

    // read the file and return the jsonNode
    return mapper.readTree(new File(formFilePath));
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
