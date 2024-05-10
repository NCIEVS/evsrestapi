package gov.nih.nci.evs.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.EmailDetails;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class FormEmailServiceTest {
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(FormEmailServiceImpl.class);

  // Mock JavaMailSender
  @Mock private JavaMailSender javaMailSender;

  // Inject mocks automatically into FormEmailServiceImpl
  @InjectMocks private FormEmailServiceImpl formEmailService;

  // email details object
  private EmailDetails emailDetails = new EmailDetails();

  private final String source = "NCIT";
  private final String toEmail = "agarcia@westcoastinformatics.com";
  private final String fromEmail = "test@example.com";
  private final String subject = "Test Subject";
  private final String msgBody = "Test Body";

  /** Test sending an email */
  @Test
  public void testSendEmail() throws Exception {
    // SET UP

    emailDetails = createEmail();
    // asser the emailDetails was populated correctly
    assertEquals(source, emailDetails.getSource());
    assertEquals(toEmail, emailDetails.getToEmail());
    assertEquals(fromEmail, emailDetails.getFromEmail());
    assertEquals(subject, emailDetails.getSubject());
    assertEquals(msgBody, emailDetails.getMsgBody());

    // ACT
    formEmailService.sendEmail(emailDetails);

    // ASSERT: verify the email was sent
    verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
  }

  /** Test we throw an exception when the email doesn't send */
  @Test
  public void testSendEmailThrowsException() throws Exception {
    // SETUP
    emailDetails = createEmail();
    doThrow(new MailSendException("")).when(javaMailSender).send(any(SimpleMailMessage.class));

    // ACT
    formEmailService.sendEmail(emailDetails);

    // ASSERT
    verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
  }

  /**
   * Helper method for creating the email details to send in the email
   *
   * @return EmailDetails object
   */
  private EmailDetails createEmail() {
    emailDetails.setSource(source);
    emailDetails.setToEmail(toEmail);
    emailDetails.setFromEmail(fromEmail);
    emailDetails.setSubject(subject);
    emailDetails.setMsgBody(msgBody);

    return emailDetails;
  }
}
