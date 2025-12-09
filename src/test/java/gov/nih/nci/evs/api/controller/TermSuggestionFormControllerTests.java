package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.CaptchaService;
import gov.nih.nci.evs.api.service.TermSuggestionFormService;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Test class for the Term Form Controller. This test mocks the email and captcha services. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TermSuggestionFormControllerTests {

  /** The Constant log. */
  // logger
  @SuppressWarnings("unused")
  private static final Logger log =
      LoggerFactory.getLogger(TermSuggestionFormControllerTests.class);

  /** The mvc mock. */
  @Autowired private MockMvc mvc;

  /** The term form service. */
  @MockitoBean private TermSuggestionFormService termFormService;

  /** The term suggestion form controller. */
  @Autowired private TermSuggestionFormController termSuggestionFormController;

  /** The captcha service. */
  @MockitoBean private CaptchaService captchaService;

  /** The recaptcha token. */
  final String recaptchaToken = "testTokenString";

  /** The license key. */
  final String licenseKey = "test-key-123";

  /**
   * Setup method to create a mock request for testing.
   *
   * @throws Exception the exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    //    termSuggestionFormController =
    //        new TermSuggestionFormController(termFormService, captchaService);
    MockHttpServletRequest request = new MockHttpServletRequest();
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

    // this may not be null if properites are configured
    //    assertNotNull(responseBody.get("recaptchaSiteKey"));

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
    assertEquals("name", contactFields.get(1).get("name").asText()); // NEW FIELD

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

    // Find the "currentCCode" field (conditionally displayed for "Proposed Modification to Existing
    // Concept")
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
    assertEquals(
        "Proposed Modification to Existing Concept",
        codeConditionalDisplay.get("showWhen").asText());

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
    assertEquals(8, requestTypeFields.size()); // 1 dropdown + 7 conditional fields

    // Final assertion - overall JSON equality
    assertEquals(expectedResponse, responseBody);

    // ACT - mock the email service and call the getForm
    when(termFormService.getFormTemplate(formType)).thenReturn(expectedResponse);
    mvc.perform(get("/api/v1/form/suggest/{formType}", formType).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()) // Check for HTTP 200
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.formName").value("NCIt Term Suggestion Request"))
        .andExpect(jsonPath("$.formType").value("NCIT"));
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
    mvc.perform(get("/api/v1/form/suggest/{formType}", formType).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is5xxServerError()) // Check for HTTP 200
        .andExpect(
            result ->
                assertTrue(
                    Objects.requireNonNull(result.getResolvedException())
                        .getMessage()
                        .contains(expectedResponse)));
  }

  /**
   * Tests the "happy path" where captcha is valid and email sends successfully.
   *
   * @throws Exception the exception
   */
  @Test
  void testSubmitForm() throws Exception {
    // Create a mock JSON payload that will pass validation
    JsonNode formData = createForm("formSamples/submissionFormTest-ncit.json");
    ArgumentCaptor<EmailDetails> emailDetailsCaptor = ArgumentCaptor.forClass(EmailDetails.class);

    // Mock the service calls
    when(captchaService.verifyRecaptcha(recaptchaToken)).thenReturn(true);
    // use doNothing() for void methods. Use any() since the object is created inside the method.
    // EmailDetails emailDetails = mock(EmailDetails.class);
    doNothing().when(termFormService).sendEmail(emailDetailsCaptor.capture());

    // 2. Act & 3. Assert
    mvc.perform(
            post("/api/v1/form/submit") // Assuming /api/v1 based on your GET test
                .header("X-EVSRESTAPI-License-Key", licenseKey)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ThreadLocalMapper.get().writeValueAsString(formData)))
        .andExpect(status().isOk()); // Default status for a void method is 200 OK

    // Verify that all our mocks were called correctly
    verify(captchaService).verifyRecaptcha(recaptchaToken);
    verify(termFormService).sendEmail(emailDetailsCaptor.getValue());
    EmailDetails capturedEmailDetails = emailDetailsCaptor.getValue();
    assertEquals(formData.get("recipientEmail").asText(), capturedEmailDetails.getToEmail());
    assertEquals(formData.get("subject").asText(), capturedEmailDetails.getSubject());
    assertEquals(formData.get("businessEmail").asText(), capturedEmailDetails.getFromEmail());
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
    // final String expectedResponse = "417 EXPECTATION_FAILED";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    mvc.perform(
            post("/api/v1/form/submit") // Assuming /api/v1 based on your GET test
                .header("X-EVSRESTAPI-License-Key", licenseKey)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ThreadLocalMapper.get().writeValueAsString(formData)))
        .andExpect(status().isExpectationFailed()); // Default status for a void method is 200 OK
  }

  /**
   * Test the sumbitForm throws an exception when the form details have null values.
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitFormThrowsExceptionWhenFormIsEmpty() throws Exception {
    // SET UP - create our form data JsonNode
    final JsonNode formData = ThreadLocalMapper.get().createObjectNode();
    // final String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    mvc.perform(
            post("/api/v1/form/submit") // Assuming /api/v1 based on your GET test
                .header("X-EVSRESTAPI-License-Key", licenseKey)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ThreadLocalMapper.get().writeValueAsString(formData)))
        .andExpect(status().is5xxServerError()); // Default status for a void method is 200 OK
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
    // final String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);
    doThrow(new RuntimeException("Simulated email sending failure"))
        .when(termFormService)
        .sendEmail(any());

    // ASSERT
    mvc.perform(
            post("/api/v1/form/submit") // Assuming /api/v1 based on your GET test
                .header("X-EVSRESTAPI-License-Key", licenseKey)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ThreadLocalMapper.get().writeValueAsString(formData)))
        .andExpect(status().is5xxServerError()); // Default status for a void method is 200 OK
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
    // ACT & ASSERT
    mvc.perform(
            post("/api/v1/form/submit") // Assuming /api/v1 based on your GET test
                .header("X-EVSRESTAPI-License-Key", licenseKey)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ThreadLocalMapper.get().writeValueAsString(formData)))
        .andExpect(status().isExpectationFailed());
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
}
