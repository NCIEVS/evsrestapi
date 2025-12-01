package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.service.CaptchaService;
import gov.nih.nci.evs.api.service.TermSuggestionFormServiceImpl;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Test class for the Term Form Controller. To run this you need to set some stuff up.
 *
 * <pre>
 * Uses the following env vars (if not set, tests do not run):
 *
 * MAIL_USERNAME
 * MAIL_PASSWORD
 *
 * as well as the following yml properties:
 *
 * mail.host
 * mail.port
 * mail.smtp.auth
 * mail.smtp.starttls.enable
 * </pre>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TermSuggestionFormControllerTests {

  /** The Constant log. */
  // logger
  private static final Logger log =
      LoggerFactory.getLogger(TermSuggestionFormControllerTests.class);

  /** The mvc. */
  // Mock the MVC automatically
  @Autowired private MockMvc mvc;

  /** The term form service. */
  // Mock the email service and captcha service
  @MockitoBean private TermSuggestionFormServiceImpl termFormService;

  /** The captcha service. */
  @MockitoBean private CaptchaService captchaService;

  /** The term suggestion form controller. */
  // create an instance of the controller and inject service
  @Autowired private TermSuggestionFormController termSuggestionFormController;

  /** The mail sender. */
  @Autowired private JavaMailSender mailSender;

  /** The request. */
  // mock request servlet
  private MockHttpServletRequest request;

  /** The base url. */
  // Base url for api calls
  String baseUrl;

  /** The recaptcha token. */
  // Recaptcha Token
  final String recaptchaToken = "testTokenString";

  /** The object mapper. */
  @Qualifier("objectMapper")
  @Autowired
  private ObjectMapper objectMapper;

  /** Setup method to create a mock request for testing. */
  @BeforeEach
  public void setUp() throws Exception {
    baseUrl = "/api/v1/submit/";
    //    termSuggestionFormController =
    //        new TermSuggestionFormController(termFormService, captchaService);
    request = new MockHttpServletRequest();
    request.addHeader("Captcha-Token", recaptchaToken);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    // Set up default mock behavior for ncit-form that works for all tests
    // Individual tests can override this if needed
    final String formPath = "formSamples/testNCIT.json";
    final JsonNode defaultFormResponse = createForm(formPath);
    when(termFormService.getFormTemplate("ncit-form")).thenReturn(defaultFormResponse);
  }

  /**
   * Test the getForm returns our expected response JsonObject when passing formType and test Json.
   *
   * @throws Exception exception
   */
  @Test
  public void testGetFormTemplateWithTestData() throws Exception {
    // SET UP
    final String formType = "ncit-form";
    // This should match the actual form returned
    final String formPath = "formSamples/testNCIT.json";
    // read the file as an Input Stream and set our expected response
    final JsonNode expectedResponse = createForm(formPath);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT - call the getForm (mock behavior is set up in setUp())
    final ResponseEntity<?> responseEntity = termSuggestionFormController.getForm(formType, null);

    // ASSERT
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    // Get response body as JsonNode for detailed assertions
    JsonNode responseBody = (JsonNode) responseEntity.getBody();
    assertNotNull(responseBody);

    // 1. Validate top-level metadata (critical for form identification)
    assertEquals("NCIt Term Suggestion Request", responseBody.get("formName").asText());
    assertEquals("NCIT", responseBody.get("formType").asText());
    assertEquals("ncithesaurus@mail.nih.gov", responseBody.get("recipientEmail").asText());
    assertNotNull(responseBody.get("recaptchaSiteKey"));

    // 2. Validate section structure (validates major form reorganization)
    JsonNode sections = responseBody.get("sections");
    assertNotNull(sections);
    assertEquals(3, sections.size());
    assertEquals("contact", sections.get(0).get("name").asText());
    assertEquals("Contact Information", sections.get(0).get("label").asText());
    assertEquals("requestType", sections.get(1).get("name").asText());
    assertEquals("Request Type", sections.get(1).get("label").asText());
    assertEquals("additionalInfo", sections.get(2).get("name").asText());
    assertEquals("Additional Information", sections.get(2).get("label").asText());

    // 3. Validate Contact section - new "name" field (key change from "other")
    JsonNode contactFields = sections.get(0).get("fields");
    assertEquals(2, contactFields.size());
    assertEquals("email", contactFields.get(0).get("name").asText());
    assertEquals("name", contactFields.get(1).get("name").asText());  // NEW FIELD

    // 4. Validate Request Type dropdown options (most critical new feature)
    JsonNode requestTypeFields = sections.get(1).get("fields");
    JsonNode requestTypeDropdown = requestTypeFields.get(0);
    assertEquals("requestTypeSelection", requestTypeDropdown.get("name").asText());
    assertEquals("dropdown", requestTypeDropdown.get("type").asText());

    JsonNode options = requestTypeDropdown.get("options");
    assertEquals(3, options.size());
    assertEquals("Proposed New Concept", options.get(0).asText());
    assertEquals("Proposed Modification to Existing Concept", options.get(1).asText());
    assertEquals("Inquiry", options.get(2).asText());

    // 5. Validate conditional display fields (validates conditional logic)
    // Find the "term" field (conditionally displayed for "Proposed New Concept")
    JsonNode termField = null;
    for (JsonNode field : requestTypeFields) {
      if ("term".equals(field.get("name").asText())) {
        termField = field;
        break;
      }
    }
    assertNotNull(termField);
    JsonNode termConditionalDisplay = termField.get("conditionalDisplay");
    assertNotNull(termConditionalDisplay);
    assertEquals("requestTypeSelection", termConditionalDisplay.get("dependsOn").asText());
    assertEquals("Proposed New Concept", termConditionalDisplay.get("showWhen").asText());

    // Find the "currentCCode" field (conditionally displayed for "Proposed Modification to Existing Concept")
    JsonNode currentCCodeField = null;
    for (JsonNode field : requestTypeFields) {
      if ("currentCCode".equals(field.get("name").asText())) {
        currentCCodeField = field;
        break;
      }
    }
    assertNotNull(currentCCodeField);
    JsonNode codeConditionalDisplay = currentCCodeField.get("conditionalDisplay");
    assertNotNull(codeConditionalDisplay);
    assertEquals("requestTypeSelection", codeConditionalDisplay.get("dependsOn").asText());
    assertEquals("Proposed Modification to Existing Concept", codeConditionalDisplay.get("showWhen").asText());

    // Find the "detailedDescription" field (conditionally displayed for "Inquiry")
    JsonNode detailedDescField = null;
    for (JsonNode field : requestTypeFields) {
      if ("detailedDescription".equals(field.get("name").asText())) {
        detailedDescField = field;
        break;
      }
    }
    assertNotNull(detailedDescField);
    JsonNode descConditionalDisplay = detailedDescField.get("conditionalDisplay");
    assertNotNull(descConditionalDisplay);
    assertEquals("requestTypeSelection", descConditionalDisplay.get("dependsOn").asText());
    assertEquals("Inquiry", descConditionalDisplay.get("showWhen").asText());

    // 6. Validate required fields in Additional Info section (new requirements)
    JsonNode additionalInfoFields = sections.get(2).get("fields");
    assertEquals(3, additionalInfoFields.size());

    // Organization - NEW REQUIRED FIELD
    JsonNode organization = additionalInfoFields.get(0);
    assertEquals("organization", organization.get("name").asText());
    assertNotNull(organization.get("validations"));
    assertTrue(organization.get("validations").toString().contains("required"));

    // Project - NOW REQUIRED (was optional)
    JsonNode project = additionalInfoFields.get(1);
    assertEquals("project", project.get("name").asText());
    assertNotNull(project.get("validations"));
    assertTrue(project.get("validations").toString().contains("required"));

    // 7. Validate count of conditional fields (validates all request type scenarios)
    assertEquals(8, requestTypeFields.size());  // 1 dropdown + 7 conditional fields

    // Final assertion - overall JSON equality
    assertEquals(expectedResponse, responseBody);
  }

  /**
   * Test the getForm throws an exception when the file can't be loaded.
   *
   * @throws Exception exception
   */
  @Test
  public void testGetFormThrowsException() throws Exception {
    // SET UP
    final String formType = "CAD"; // form type doesn't exist
    final String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT
    when(termFormService.getFormTemplate(formType)).thenThrow(new FileNotFoundException());

    // ASSERT
    final Exception exception =
        assertThrows(
            ResponseStatusException.class,
            () -> {
              termSuggestionFormController.getForm(formType, null);
            });
    assertTrue(Objects.requireNonNull(exception.getMessage()).contains(expectedResponse));
  }

  /**
   * Test the submitForm successfully sends email with submitted form.
   *
   * <p>This is really an integration test that requires properties for mail authentication to
   * succeed.
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitForm() throws Exception {
    // SET UP - create our form data JsonNode
    final String formPath = "formSamples/submissionFormTest-ncit.json";
    JsonNode formData = createForm(formPath);
    // Create expected EmailDetails
    EmailDetails expectedEmailDetails = EmailDetails.generateEmailDetails(formData);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    boolean smtpConfigured = hasSmtpConfig();

    if (!smtpConfigured) {
      // If no configuration, bail
      return;
    } else {
      // If smtp is configured, we will actually send the email
      formData = setCredentials(formData, false);
      expectedEmailDetails = EmailDetails.generateEmailDetails(formData);
      log.info("    SMTP is configured, will send email to: {}", expectedEmailDetails.getToEmail());
    }

    // ACT
    termSuggestionFormController.submitForm(formData, null, recaptchaToken);
  }

  /**
   * Test the sumbitForm throws an exception when the form details have null values.
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitFormThrowsExceptionWithNullFields() throws Exception {
    // SET UP - create our form data JsonNode
    final String formPath = "formSamples/submissionFormNullTest.json";
    final JsonNode formData = createForm(formPath);
    final String expectedResponse = "417 EXPECTATION_FAILED";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT & ASSERT
    final Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              termSuggestionFormController.submitForm(formData, null, recaptchaToken);
            });
    assertTrue(exception.getMessage().contains(expectedResponse));
  }

  /**
   * Test the sumbitForm throws an exception when the form details have null values.
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitFormThrowsExceptionWhenFormIsEmpty() throws Exception {
    // SET UP - create our form data JsonNode
    final JsonNode formData = objectMapper.createObjectNode();
    final String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT & ASSERT
    final Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              termSuggestionFormController.submitForm(formData, null, recaptchaToken);
            });
    assertTrue(exception.getMessage().contains(expectedResponse));
  }

  /**
   * Test the submitForm throws an exception when the email fails to send.
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitFormThrowsExceptionWhenSendEmailFails() throws Exception {
    // SET UP - create our form data JsonNode
    final String formPath = "formSamples/submissionFormTest-ncit.json";
    JsonNode formData = createForm(formPath);
    final String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT - stub the void method to do throw an exception when called
    if (!hasSmtpConfig()) {
      // If no configuration, bail
      return;
    } else {
      // If smtp is configured, we will actually attempt to send the email
      // but we will give it bad credentials so it will fail
      log.info(
          "SMTP is configured, will send email to: {}", formData.get("recipientEmail").asText());
      formData = setCredentials(formData, true);
    }

    // ASSERT
    final JsonNode formDataFinal = formData;
    // create a props object that returns no override
    ApplicationProperties props = new ApplicationProperties();
    props.setMailRecipient("");

    // find the formService instance used by the controller
    Object form = ReflectionTestUtils.getField(termSuggestionFormController, "formService");

    // set the applicationProperties on THAT instance
    ReflectionTestUtils.setField(form, "applicationProperties", props);
    final Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              termSuggestionFormController.submitForm(formDataFinal, null, recaptchaToken);
            });
    assertTrue(exception.getMessage().contains(expectedResponse));
  }

  /**
   * Test submit form recaptcha verification passes. This is an integration test that requires email
   * credentials. We need an escape hatch to avoid actually trying to send an email here but verify
   * everything else works.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSubmitFormRecaptchaVerificationPasses() throws Exception {
    // SET UP
    final String formPath = "formSamples/submissionFormTest-ncit.json";
    JsonNode formData = createForm(formPath);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);
    // Mock the email service to do nothing if not configured
    if (!hasSmtpConfig()) {
      // If no configuration, bail
      return;
    } else {
      // If smtp is configured, we will actually send the email
      log.info(
          "SMTP is configured, will send email to: {}", formData.get("recipientEmail").asText());
      formData = setCredentials(formData, false);
    }

    // ACT & ASSERT
    try {
      termSuggestionFormController.submitForm(formData, null, recaptchaToken);
    } catch (ResponseStatusException e) {
      fail("Should not have thrown any exception");
    }
  }

  /**
   * Test submit form recaptcha verification fails.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSubmitFormRecaptchaVerificationFails() throws Exception {
    // SET UP
    final String formPath = "formSamples/submissionFormTest-ncit.json";
    JsonNode formData = createForm(formPath);
    // Create expected EmailDetails

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(false);
    if (!hasSmtpConfig()) {
      // If no configuration, bail
      return;
    } else {
      // If smtp is configured, we will actually attempt send the email
      // but we will fail because fo the recaptcha failure
      log.info(
          "SMTP is configured, will send email to: {}", formData.get("recipientEmail").asText());
      formData = setCredentials(formData, false);
    }

    // ACT & ASSERT
    try {
      termSuggestionFormController.submitForm(formData, null, recaptchaToken);
      fail("Expected a ResponseStatusException to be thrown");
    } catch (ResponseStatusException e) {
      assertEquals(HttpStatus.EXPECTATION_FAILED, e.getStatusCode());
    }
  }

  /**
   * Integration test to verify we can call the api path, path the formType, and return the JsonNode
   * form.
   *
   * @throws Exception exception
   */
  @Test
  public void integrationTestGetFormTemplate() throws Exception {
    // SET UP
    final String formType = "ncit-form";
    final String url = "/api/v1/form/suggest/" + formType;
    JsonNode form;

    // ACT
    log.info("Testing url: {}", url);
    final MvcResult mvcResult =
        this.mvc.perform(MockMvcRequestBuilders.get(url)).andExpect(status().isOk()).andReturn();
    form = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    log.info("    Form = {}", form);

    // ASSERT
    assertNotNull(form);
    assertFalse(form.isEmpty());
    assertEquals("NCIt Term Suggestion Request", form.get("formName").asText());
    assertEquals("ncithesaurus@mail.nih.gov", form.get("recipientEmail").asText());
  }

  /**
   * Integration test for submitting a filled out form and sending the email. NOTE: Set your local
   * environment variables in your config. Your test email will need an App Password for access, if
   * using Gmail. This is an integration test that requires extra info. We need to set these tests
   * up to avoid running if environment vars are not set, but to support them if they are.
   *
   * @throws Exception exception
   */
  @Test
  public void integrationTestSubmitForm() throws Exception {
    // SET UP
    baseUrl = "/api/v1/form/submit";
    final String formPath = "formSamples/submissionFormTest-ncit.json";
    JsonNode formData = createForm(formPath);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT & ASSERT - Verify the email was sent to the receiver in the form
    if (!hasSmtpConfig()) {
      // If no configuration, bail
      return;
    } else {
      // If smtp is configured, we will actually send the email
      log.info(
          "SMTP is configured, will send email to: {}", formData.get("recipientEmail").asText());
      // Set credentials in the form data
      formData = setCredentials(formData, false);
    }
    final String requestBody = objectMapper.writeValueAsString(formData);
    log.info("Form data = {}", formData);
    @SuppressWarnings("unused")
    final MvcResult result =
        this.mvc
            .perform(
                MockMvcRequestBuilders.post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Captcha-Token", recaptchaToken)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andReturn();
  }

  @Test
  public void integrationTestSubmitFormWithAttachment() throws Exception {
    // SET UP
    baseUrl = "/api/v1/form/submitWithAttachment";
    final String formPath = "formSamples/submissionFormTest-cdisc.json";
    JsonNode formData = createForm(formPath);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    if (!hasSmtpConfig()) {
      // If no configuration, bail
      return;
    } else {
      // If smtp is configured, we will actually send the email
      log.info("SMTP is configured.");
      // Set credentials in the form data
      formData = setCredentials(formData, false);
    }

    // Prepare multipart file from resources
    final org.springframework.mock.web.MockMultipartFile attachment;
    try (final InputStream is =
        getClass()
            .getClassLoader()
            .getResourceAsStream("formSamples/filled-form-submission-cdisc.xls")) {
      if (is == null) {
        fail("Test attachment not found in resources");
        return;
      }
      attachment =
          new org.springframework.mock.web.MockMultipartFile(
              "file", "filled-form-submission-cdisc.xls", "application/vnd.ms-excel", is);
    }

    // formData part as application/json
    final org.springframework.mock.web.MockMultipartFile jsonPart =
        new org.springframework.mock.web.MockMultipartFile(
            "formData", "formData", "application/json", objectMapper.writeValueAsBytes(formData));

    // ACT & ASSERT - perform multipart request
    this.mvc
        .perform(
            MockMvcRequestBuilders.multipart(baseUrl)
                .file(attachment)
                .file(jsonPart)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk());
  }

  @Test
  public void integrationTestSubmitFormWithAttachmentNCIT() throws Exception {
    // SET UP
    baseUrl = "/api/v1/form/submitWithAttachment";
    final String formPath = "formSamples/testNCIT.json";
    JsonNode formData = createForm(formPath);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    if (!hasSmtpConfig()) {
      // If no configuration, bail
      return;
    } else {
      // If smtp is configured, we will actually send the email
      log.info("SMTP is configured.");
      // Set credentials in the form data
      formData = setCredentials(formData, false);
    }

    // Prepare NCIT multipart file from resources
    final org.springframework.mock.web.MockMultipartFile attachment;
    try (final InputStream is =
        getClass()
            .getClassLoader()
            .getResourceAsStream("formSamples/filled-form-submission-ncit.xls")) {
      if (is == null) {
        fail("NCIT test attachment not found in resources");
        return;
      }
      attachment =
          new org.springframework.mock.web.MockMultipartFile(
              "file", "filled-form-submission-ncit.xls", "application/vnd.ms-excel", is);
    }

    // formData part as application/json
    final org.springframework.mock.web.MockMultipartFile jsonPart =
        new org.springframework.mock.web.MockMultipartFile(
            "formData", "formData", "application/json", objectMapper.writeValueAsBytes(formData));

    // ACT & ASSERT - perform multipart request
    this.mvc
        .perform(
            MockMvcRequestBuilders.multipart(baseUrl)
                .file(attachment)
                .file(jsonPart)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk());
  }

  /**
   * Helper method for creating a JsonNode from a Json file.
   *
   * @param path path for the json file
   * @return JsonNode
   * @throws Exception exception
   */
  private JsonNode createForm(final String path) throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    // read the file as an Input Stream
    try (final InputStream input = getClass().getClassLoader().getResourceAsStream(path); ) {
      // Set our expected response to the form from the formPath
      return mapper.readTree(input);
    }
  }

  private boolean hasSmtpConfig() {
    // check that MAIL_USER and MAIL_PASSWORD are set
    // as environment variables
    if (System.getenv("MAIL_USER") == null) {
      log.info(" MAIL_USER not set, skipping test");
      return false;
    }
    // to generate a valid MAIL_PASSWORD, you need to set up an App Password
    // for your email account, e.g., Gmail, and set it in your environment variables.
    // See https://support.google.com/mail/answer/185833?hl=en
    // for more information on how to set up App Passwords
    if (System.getenv("MAIL_PASSWORD") == null) {
      log.info(" MAIL_PASSWORD not set, skipping test");
      return false;
    }

    if (mailSender == null || !(mailSender instanceof JavaMailSenderImpl)) {
      return false;
    }
    JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;

    return impl.getHost() != null
        && impl.getPort() > 0
        && impl.getUsername() != null
        && impl.getPassword() != null
        && !"false"
            .equalsIgnoreCase(
                impl.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
  }

  /**
   * Set credentials in the form data to test email sending.
   *
   * @param formData the form data
   * @return JsonNode with bad credentials
   */
  private JsonNode setCredentials(JsonNode formData, boolean badCredentials) {
    if (!(mailSender instanceof JavaMailSenderImpl)) {
      throw new IllegalStateException("Mail sender is not configured correctly.");
    }
    JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;

    ObjectNode o = (ObjectNode) formData;
    o.put("businessEmail", impl.getUsername());

    if (badCredentials) {
      // Set bad recipient email
      o.put("recipientEmail", "invalid@@westcoastinformatics.com");

    } else {
      // Set real credentials
      o.put("mail.smtp.auth", "true");
      o.put("mail.smtp.starttls.enable", "true");
      o.put("recipientEmail", impl.getUsername());
      o.put("MAIL_USERNAME", impl.getUsername());
      o.put("MAIL_PASSWORD", impl.getPassword());
    }
    return o;
  }
}
