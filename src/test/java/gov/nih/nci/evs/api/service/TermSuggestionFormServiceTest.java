package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import jakarta.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Test class for the email form service class. */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = TestConfiguration.class)
public class TermSuggestionFormServiceTest {
  // Logger
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormServiceTest.class);

  // Mock JavaMailSender & app properties
  @Mock private JavaMailSender javaMailSender;
  @Mock private ApplicationProperties applicationProperties;
  @Mock private ObjectMapper objectMapper;

  // Inject mocks automatically into FormEmailServiceImpl
  private TermSuggestionFormServiceImpl termFormService;

  // email details object
  private EmailDetails testEmailDetails = new EmailDetails();

  // email details
  private final String source = "NCIT";
  private final String toEmail = "agarcia@westcoastinformatics.com";
  private final String fromEmail = "test@example.com";
  private final String subject = "Test Subject";
  private final String msgBody = "Test Body";

  // Config url
  @Value("${nci.evs.application.configBaseUri}")
  private String configUrl;

  @BeforeEach
  public void setUp() {
    termFormService =
        new TermSuggestionFormServiceImpl(javaMailSender, applicationProperties, objectMapper);
  }

  /**
   * Test the getTermForm returns our NCIT form JsonNode
   *
   * @throws Exception throws exception
   */
  @Test
  public void testGetFormTemplate() throws Exception {
    // SET UP
    String formType = "ncit-form";
    JsonNode termForm = new ObjectMapper().createObjectNode();

    when(applicationProperties.getConfigBaseUri()).thenReturn(configUrl);
    when(objectMapper.readTree(new URL(configUrl + "/" + formType + ".json"))).thenReturn(termForm);

    // ACT
    JsonNode returnedForm = termFormService.getFormTemplate(formType);

    // ASSERT
    verify(applicationProperties, times(1)).getConfigBaseUri();
    assertNotNull(returnedForm);
    assertTrue(returnedForm.isObject());
    // Verify the recaptcha site key was set and is in the response
    assertNotNull(returnedForm.get("recaptchaSiteKey").asText());
  }

  /**
   * Test get form throws IO exception with invalid formType string
   *
   * @throws IOException throws exception
   */
  @Test
  public void testGetFormThrowsIOException() throws IOException {
    // SET UP
    String formType = "none-form";

    when(applicationProperties.getConfigBaseUri()).thenReturn(configUrl);
    when(objectMapper.readTree(new URL(configUrl + "/" + formType + ".json")))
        .thenThrow(IOException.class);

    // ACT & ASSERT
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

  /**
   * Test getFormTemplate throws an exception when the file is not found
   *
   * @throws Exception throws exception
   */
  @Test
  public void testGetFormTemplateThrowsFileNotFound() throws Exception {
    // SET UP - create an invalid term form object
    String formType = "invalid-form";
    JsonNode termForm = new ObjectMapper().createArrayNode();

    when(applicationProperties.getConfigBaseUri()).thenReturn(configUrl);
    when(objectMapper.readTree(new URL(configUrl + "/" + formType + ".json")))
        .thenThrow(FileNotFoundException.class);

    // ACT & ASSERT
    Exception exception =
        assertThrows(
            FileNotFoundException.class,
            () -> {
              termFormService.getFormTemplate(formType);
            });
  }

  /**
   * Test getFormTemplate throws an exception when the form is not an object
   *
   * @throws Exception throws exception
   */
  @Test
  public void testGetFormTemplateWhenNotObjectThrowsException() throws Exception {
    // SET UP - create an invalid term form object
    String formType = "invalid-form";
    JsonNode termForm = new ObjectMapper().createArrayNode();

    when(applicationProperties.getConfigBaseUri()).thenReturn(configUrl);
    when(objectMapper.readTree(any(URL.class))).thenReturn(termForm);

    // ACT & ASSERT
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              termFormService.getFormTemplate(formType);
            });

    assertTrue(exception.getMessage().contains("Invalid form template."));
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
