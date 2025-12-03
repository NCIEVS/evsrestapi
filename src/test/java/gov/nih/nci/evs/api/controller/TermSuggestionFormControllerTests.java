package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.CaptchaService;
import gov.nih.nci.evs.api.service.TermSuggestionFormService;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/** Test class for the Term Form Controller. This test mocks the email and captcha services. */
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
  @MockitoBean private TermSuggestionFormService termFormService;

  /** The captcha service. */
  @MockitoBean private CaptchaService captchaService;

  /** The base url. */
  // Base url for api calls
  private String baseUrl = "/api/v1/submit/";

  /** The recaptcha token. */
  // Recaptcha Token
  final String recaptchaToken = "testTokenString";

  final String licenseKey = "test-key-123";

  private ObjectMapper objectMapper = new ObjectMapper();

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

    // ACT - mock the email service and call the getForm
    when(termFormService.getFormTemplate(formType)).thenReturn(expectedResponse);
    mvc.perform(get("/api/v1/form/suggest/{formType}", formType).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()) // Check for HTTP 200
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(
            jsonPath("$.formName").value("NCIT")) // Verify JSON content
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

  /** Tests the "happy path" where captcha is valid and email sends successfully. */
  @Test
  void testSubmitForm() throws Exception {
    // Create a mock JSON payload that will pass validation
    JsonNode formData = createForm("formSamples/submissionFormTest-ncit.json");
    ArgumentCaptor<EmailDetails> emailDetailsCaptor = ArgumentCaptor.forClass(EmailDetails.class);

    // Mock the service calls
    when(captchaService.verifyRecaptcha(recaptchaToken)).thenReturn(true);
    // use doNothing() for void methods. Use any() since the object is created inside the method.
    EmailDetails emailDetails = mock(EmailDetails.class);
    doNothing().when(termFormService).sendEmail(emailDetailsCaptor.capture());

    // 2. Act & 3. Assert
    mvc.perform(
            post("/api/v1/form/submit") // Assuming /api/v1 based on your GET test
                .header("X-EVSRESTAPI-License-Key", licenseKey)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formData)))
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
    final String expectedResponse = "417 EXPECTATION_FAILED";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    mvc.perform(
            post("/api/v1/form/submit") // Assuming /api/v1 based on your GET test
                .header("X-EVSRESTAPI-License-Key", licenseKey)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formData)))
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
    final JsonNode formData = objectMapper.createObjectNode();
    final String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    mvc.perform(
            post("/api/v1/form/submit") // Assuming /api/v1 based on your GET test
                .header("X-EVSRESTAPI-License-Key", licenseKey)
                .header("Captcha-Token", recaptchaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formData)))
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
    final String expectedResponse = "500 INTERNAL_SERVER_ERROR";

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
                .content(objectMapper.writeValueAsString(formData)))
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
                .content(objectMapper.writeValueAsString(formData)))
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
