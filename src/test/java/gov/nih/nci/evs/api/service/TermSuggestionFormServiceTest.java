package gov.nih.nci.evs.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import java.io.IOException;
import javax.mail.internet.MimeMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/** Test class for the email form service class. */
@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = TestConfiguration.class)
public class TermSuggestionFormServiceTest {
  // Logger
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormServiceImpl.class);

  // Mock JavaMailSender & app properties
  @Mock private JavaMailSender javaMailSender;
  @Mock private ApplicationProperties applicationProperties;

  // Inject mocks automatically into FormEmailServiceImpl
  @InjectMocks private TermSuggestionFormServiceImpl termFormService;

  // email details object
  private EmailDetails testEmailDetails = new EmailDetails();

  // email details
  private final String source = "NCIT";
  private final String toEmail = "agarcia@westcoastinformatics.com";
  private final String fromEmail = "test@example.com";
  private final String subject = "Test Subject";
  private final String msgBody = "Test Body";

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
    when(applicationProperties.getConfigBaseUri())
        .thenReturn(
            "https://raw.githubusercontent.com/NCIEVS/evsrestapi-operations/develop/config/metadata");
    JsonNode returnedForm = termFormService.getFormTemplate(formType);

    // ASSERT
    verify(applicationProperties, times(1)).getConfigBaseUri();
    assertNotNull(returnedForm);
    assertEquals("NCIt Term Suggestion Request", returnedForm.get("formName").asText());
    // TODO: Update this test to assertEquals after changing the recipient email in the form
    assertNotEquals("ncithesaurus@mail.nih.gov", returnedForm.get("recipientEmail").asText());
    // Verify the recaptcha site key was set and is in the response
    assertNotNull(returnedForm.get("recaptchaSiteKey").asText());
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
    when(applicationProperties.getConfigBaseUri())
        .thenReturn(
            "https://raw.githubusercontent.com/NCIEVS/evsrestapi-operations/develop/config/metadata");

    // ASSERT
    assertThrows(
        IOException.class,
        () -> {
          termFormService.getFormTemplate(formType);
        });
  }

  /** Test getFormTemplate throws an exception with an empty formType string */
  @Test
  public void testGetFormTemplateThrowsIllegalArgExceptionEmpty() throws IllegalArgumentException {
    testGetFormTemplateThrowsIllegalArgException("");
  }

  /** Test getFormTemplate throws an exception with a blank formType string */
  @Test
  public void testGetFormTemplateTypeThrowsIllegalArgExceptionSpace()
      throws IllegalArgumentException {
    testGetFormTemplateThrowsIllegalArgException(" ");
  }

  /** Test getFormTemplate throws an exception with a null formType string */
  @Test
  public void testGetFormTemplateThrowsIllegalArgExceptionNull() throws IllegalArgumentException {
    testGetFormTemplateThrowsIllegalArgException(null);
  }

  /**
   * Helper method to test multiple formType inputs throw an exception
   *
   * @param formType string form template to get
   * @throws IllegalArgumentException exception
   */
  private void testGetFormTemplateThrowsIllegalArgException(String formType)
      throws IllegalArgumentException {
    // SET UP
    String expectedMessage = "Invalid form template provided";

    // ACT & ASSERT
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              termFormService.getFormTemplate(formType);
            });
    assertTrue(exception.getMessage().contains(expectedMessage));
  }

  /** Test sending an email */
  @Test
  public void testSendEmail() throws Exception {
    // SET UP
    testEmailDetails = createEmail();
    when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
    doNothing().when(javaMailSender).send(any(MimeMessage.class));

    // ACT
    termFormService.sendEmail(testEmailDetails);

    // ASSERT: verify the email was sent
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));
  }

  /** Test we throw an exception when the email doesn't send */
  @Test
  public void testSendEmailThrowsException() throws Exception {
    // SETUP
    testEmailDetails = createEmail();
    when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

    // ACT
    try {
      termFormService.sendEmail(testEmailDetails);
    } catch (MailSendException e) {
      // ASSERT
      verify(javaMailSender, times(0)).send(any(SimpleMailMessage.class));
    }
  }

  /**
   * Helper method for creating the email details to send in the email
   *
   * @return EmailDetails object
   */
  private EmailDetails createEmail() {
    testEmailDetails.setSource(source);
    testEmailDetails.setToEmail(toEmail);
    testEmailDetails.setFromEmail(fromEmail);
    testEmailDetails.setSubject(subject);
    testEmailDetails.setMsgBody(msgBody);

    return testEmailDetails;
  }
}
