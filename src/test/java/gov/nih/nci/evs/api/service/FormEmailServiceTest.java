package gov.nih.nci.evs.api.service;

import static com.ibm.icu.impl.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

/**
 * Test class for the email form service class.
 */
@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class FormEmailServiceTest {
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(FormEmailServiceImpl.class);

  // Mock JavaMailSender
  @Mock private JavaMailSender javaMailSender;
  @Mock private ApplicationProperties applicationProperties;


  // Inject mocks automatically into FormEmailServiceImpl
  @InjectMocks private FormEmailServiceImpl formEmailService;

  // email details object
  private EmailDetails emailDetails = new EmailDetails();

  // email details
  private final String source = "NCIT";
  private final String toEmail = "agarcia@westcoastinformatics.com";
  private final String fromEmail = "test@example.com";
  private final String subject = "Test Subject";
  private final String msgBody = "Test Body";

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Test the getTermForm returns our NCIT form JsonNode
   *
   * @throws IllegalArgumentException exception
   * @throws IOException IO exception
   */
  @Test
  public void testGetFormTemplate() throws IllegalArgumentException, IOException {
    // SET UP
    String formType = "ncit-form";

    // ACT
    when(applicationProperties.getConfigBaseUri()).thenReturn("https://raw.githubusercontent" +
        ".com/NCIEVS/evsrestapi-operations/develop/config/metadata");
    JsonNode returnedForm = formEmailService.getFormTemplate(formType);

    // ASSERT
    assertNotNull(returnedForm);
    assertEquals("Term Suggestion", returnedForm.get("formName").asText());
    assertEquals("ncithesaurus@mail.nih.gov", returnedForm.get("recipientEmail").asText());
  }

  /**
   * Test get form throws IO exception with invalid formType string
   *
   * @throws IOException exception
   */
  @Test
  public void testGetFormThrowsIOException() throws IOException {
    // SET UP
    String formType = "none-form";

    // ACT
    when(applicationProperties.getConfigBaseUri()).thenReturn("https://raw.githubusercontent" +
        ".com/NCIEVS/evsrestapi-operations/develop/config/metadata");

    // ASSERT
    assertThrows(IOException.class, () -> {
      formEmailService.getFormTemplate(formType);
    });
  }

  /**
   * Test getFormTemplate throws an exception with an empty formType string
   */
  @Test
  public void testGetFormTemplateThrowsIllegalArgExceptionEmpty() throws IllegalArgumentException {
    testGetFormTemplateThrowsIllegalArgException("");
  }

  /**
   * Test getFormTemplate throws an exception with a blank formType string
   */
  @Test
  public void testGetFormTemplateTypeThrowsIllegalArgExceptionSpace() throws IllegalArgumentException {
    testGetFormTemplateThrowsIllegalArgException(" ");
  }

  /**
   * Test getFormTemplate throws an exception with a null formType string
   */
  @Test
  public void testGetFormTemplateThrowsIllegalArgExceptionNull() throws IllegalArgumentException {
    testGetFormTemplateThrowsIllegalArgException(null);
  }

  /**
   * Helper method to test multiple formType inputs throw an exception
   * @param formType  string form template to get
   * @throws IllegalArgumentException exception
   */
  private void testGetFormTemplateThrowsIllegalArgException(String formType) throws IllegalArgumentException {
    // SET UP
    String expectedMessage = "Invalid form template provided";

    // ACT & ASSERT
    Exception exception = assertThrows( IllegalArgumentException.class, () -> {
      formEmailService.getFormTemplate(formType);
    });
    assertTrue(exception.getMessage().contains(expectedMessage));
  }


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

    // ACT
    doThrow(new MailSendException("")).when(javaMailSender).send(any(SimpleMailMessage.class));
    try {
      formEmailService.sendEmail(emailDetails);
      fail("Exception Thrown");
    } catch (MailSendException e) {
      // ASSERT
      verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }
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
