package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.EmailDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class FormEmailServiceImpl implements FormEmailService {
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(FormEmailServiceImpl.class);

  // JavaMailSender
  private final JavaMailSender javaMailSender;

  /**
   * Constructor: Instantiates a new Form email service with params.
   *
   * @param javaMailSender java mail sender
   */
  public FormEmailServiceImpl(JavaMailSender javaMailSender) {
    this.javaMailSender = javaMailSender;
  }

  /**
   * Sends an email with the formatted form data
   *
   * @param emailDetails details of the email created from the form data
   */
  public void sendEmail(EmailDetails emailDetails) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      // Check which formType is being submitted and set to/subject accordingly
      if (emailDetails.getSource().equals("NCIT")) {
        message.setTo(emailDetails.getToEmail());
        message.setSubject(emailDetails.getSubject());
      }
      if (emailDetails.getSource().equals("CDISC")) {
        message.setTo(emailDetails.getToEmail());
        message.setSubject(emailDetails.getSubject());
      }
      message.setText(emailDetails.getMsgBody());

      javaMailSender.send(message);
    } catch (Exception e) {
      logger.error("Unable to send email: " + e);
    }
  }
}
