package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.controller.TermSuggestionFormController;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.util.EVSUtils;
import jakarta.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    String filePath = configUrl + "/" + formType + ".json";

    when(applicationProperties.getConfigBaseUri()).thenReturn(configUrl);

    try (MockedStatic<EVSUtils> mockedUtils = Mockito.mockStatic(EVSUtils.class)) {
      mockedUtils
          .when(() -> EVSUtils.getValueFromFile(filePath))
          .thenThrow(new IOException("IO Exception reading file: " + filePath));

      // ACT & ASSERT
      assertThrows(IOException.class, () -> termFormService.getFormTemplate(formType));
    }
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
    String filePath = configUrl + "/" + formType + ".json";

    when(applicationProperties.getConfigBaseUri()).thenReturn(configUrl);

    try (MockedStatic<EVSUtils> mockedUtils = Mockito.mockStatic(EVSUtils.class)) {
      mockedUtils
          .when(() -> EVSUtils.getValueFromFile(filePath))
          .thenThrow(new FileNotFoundException("File not found: " + filePath));

      // ACT & ASSERT
      assertThrows(FileNotFoundException.class, () -> termFormService.getFormTemplate(formType));
    }
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
    String filePath = configUrl + "/" + formType + ".json";

    when(applicationProperties.getConfigBaseUri()).thenReturn(configUrl);
    when(objectMapper.readTree(any(URL.class))).thenReturn(termForm);

    // ACT & ASSERT

    try (MockedStatic<EVSUtils> mockedUtils = Mockito.mockStatic(EVSUtils.class)) {
      mockedUtils
          .when(() -> EVSUtils.getValueFromFile(filePath))
          .thenThrow(new IllegalArgumentException("Invalid form template."));

      // ACT & ASSERT
      Exception exception =
          assertThrows(
              IllegalArgumentException.class, () -> termFormService.getFormTemplate(formType));
      assertTrue(exception.getMessage().contains("Invalid form template."));
    }
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


  /** Check that blank excel form attachment fails validation. */
  @Test
  public void blankFormSubmissionFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/blank-form-submission-cdisc.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT
    assertFalse(termFormService.validateFileAttachment(testFile, "CDISC"));
  }


  /** Check that blank excel file attachment fails validation. */
  @Test
  public void blankSpreadsheetSubmissionFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/blank-spreadsheet-submission-cdisc.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT
    assertFalse(termFormService.validateFileAttachment(testFile, "CDISC"));
  }

  /** Check that fake excel file attachment fails validation. */
  @Test
  public void FakeExcelSubmissionFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/fake-excel-submission-cdisc.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT
    assertFalse(termFormService.validateFileAttachment(testFile, "CDISC"));
  }

  /** Check that extra sheet added to the attachment fails validation. */
  @Test
  public void ExtraSheetAddedFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/extra-sheets-submission-cdisc.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT
    assertFalse(termFormService.validateFileAttachment(testFile, "CDISC"));
  }

  /** Check that changed sheet name in the attachment fails validation. */
  @Test
  public void ChangedSheetNameFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/changed-sheets-submission-cdisc.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT
    assertFalse(termFormService.validateFileAttachment(testFile, "CDISC"));
  }

  /** Check that filled out form attachment passes validation. */
  @Test
  public void filledFormSubmissionPassesValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/filled-form-submission-cdisc.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT
    assertTrue(termFormService.validateFileAttachment(testFile, "CDISC"));
  }

  /** Check that filled out NCIT form attachment passes validation. */
  @Test
  public void filledFormSubmissionNCITPassesValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/filled-form-submission-ncit.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT - Using the form-type-aware version
    assertTrue(termFormService.validateFileAttachment(testFile, "NCIT"));
  }

  /** Check that blank NCIT form attachment fails validation. */
  @Test
  public void blankFormSubmissionNCITFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/blank-form-submission-ncit.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT - Should fail because there are no data rows
    assertFalse(termFormService.validateFileAttachment(testFile, "NCIT"));
  }

  /** Check that NCIT form with invalid C-code format fails validation. */
  @Test
  public void invalidCodeFormatNCITFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/invalid-code-ncit.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT - Should fail because C-code format is invalid
    assertFalse(termFormService.validateFileAttachment(testFile, "NCIT"));
  }

  /** Check that NCIT form with missing required columns fails validation. */
  @Test
  public void missingRequiredColumnsNCITFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/missing-columns-ncit.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT - Should fail because required column D is empty
    assertFalse(termFormService.validateFileAttachment(testFile, "NCIT"));
  }

  /** Check that NCIT form with wrong header fails validation. */
  @Test
  public void invalidHeaderRowNCITFailsValidation() throws Exception {
    // SET UP
    Path p = Paths.get("src/test/resources/formSamples/invalid-header-ncit.xls");
    byte[] content = Files.readAllBytes(p);
    MultipartFile testFile = new MockMultipartFile(p.getFileName().toString(), content);

    // ACT & ASSERT - Should fail because headers don't match
    assertFalse(termFormService.validateFileAttachment(testFile, "NCIT"));
  }

  /**
   * Test that TermSuggestionFormController.suggestWithAttachment throws EXPECTATION_FAILED when the
   * service's validateFileAttachment returns false.
   */
  @Test
  public void suggestWithAttachmentInvalidAttachmentThrowsExpectationFailed() throws Exception {
    // SET UP controller with mocked service and captcha
    TermSuggestionFormServiceImpl mockedService = mock(TermSuggestionFormServiceImpl.class);
    CaptchaService mockedCaptcha = mock(CaptchaService.class);

    TermSuggestionFormController controller =
        new TermSuggestionFormController(mockedService, mockedCaptcha);

    // Prepare inputs - Load a valid form JSON
    Path p = Paths.get("src/test/resources/formSamples/testNCIT.json");
    String formJsonString = Files.readString(p);
    JsonNode formData = new ObjectMapper().readTree(formJsonString);
    MultipartFile file = new MockMultipartFile("file.xlsx", new byte[] {1, 2, 3});

    // Mock captcha to succeed
    when(mockedCaptcha.verifyRecaptcha(any())).thenReturn(true);

    // Mock validateFileAttachment with form type to return false
    when(mockedService.validateFileAttachment(any(MultipartFile.class), anyString()))
        .thenReturn(false);

    // ACT & ASSERT: calling submitWithAttachment should raise ResponseStatusException
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.submitWithAttachment(formData, file, null, "token"));

    // Verify we got the EXPECTATION_FAILED status and message contains our reason
    assertTrue(ex.getStatusCode() == HttpStatus.EXPECTATION_FAILED);
    assertTrue(ex.getReason() != null && ex.getReason().contains("Unexpected sheet 'X'"));
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
