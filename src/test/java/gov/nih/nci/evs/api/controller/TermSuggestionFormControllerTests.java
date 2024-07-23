package gov.nih.nci.evs.api.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.CaptchaService;
import gov.nih.nci.evs.api.service.TermSuggestionFormServiceImpl;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/** Test class for the Term Form Controller */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TermSuggestionFormControllerTests {
  // logger
  private static final Logger log =
      LoggerFactory.getLogger(TermSuggestionFormControllerTests.class);

  // Mock the MVC automatically
  @Autowired private MockMvc mvc;

  // Mock the email service and captcha service
  @Mock private TermSuggestionFormServiceImpl termFormService;
  @MockBean private CaptchaService captchaService;

  // create an instance of the controller and inject service
  private TermSuggestionFormController termSuggestionFormController;

  // mock request servlet
  private MockHttpServletRequest request;

  // Base url for api calls
  String baseUrl;

  // Recaptcha Token
  final String recaptchaToken = "testTokenString";

  @Qualifier("objectMapper")
  @Autowired
  private ObjectMapper objectMapper;

  /** Setup method to create a mock request for testing */
  @Before
  public void setUp() {
    baseUrl = "/api/v1/suggest/";
    termSuggestionFormController =
        new TermSuggestionFormController(termFormService, captchaService);
    request = new MockHttpServletRequest();
    request.addHeader("Captcha-Token", recaptchaToken);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  /**
   * Test the getForm returns our expected response JsonObject when passing formType and test Json
   *
   * @throws Exception exception
   */
  @Test
  public void testGetFormTemplateWithTestData() throws Exception {
    // SET UP
    final String formType = "NCIT";
    final String formPath = "formSamples/testNCIT.json";
    // read the file as an Input Stream and set our expected response
    final JsonNode expectedResponse = createForm(formPath);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT - mock the email service and call the getForm
    when(termFormService.getFormTemplate(formType)).thenReturn(expectedResponse);
    final ResponseEntity<?> responseEntity = termSuggestionFormController.getForm(formType, null);

    // ASSERT
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(expectedResponse, responseEntity.getBody());
  }

  /**
   * Test the getForm throws an exception when the file can't be loaded
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
   * @throws Exception exception
   */
  @Test
  public void testSubmitForm() throws Exception {
    // SET UP - create our form data JsonNode
    final String formPath = "formSamples/submissionFormTest.json";
    final JsonNode formData = createForm(formPath);
    // Create expected EmailDetails
    EmailDetails expectedEmailDetails = EmailDetails.generateEmailDetails(formData);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);
    doNothing().when(termFormService).sendEmail(any(EmailDetails.class));

    // ACT - stub the void method to do nothing when called
    termSuggestionFormController.submitForm(formData, null, recaptchaToken);

    // ASSERT
    verify(termFormService, times(1)).sendEmail(expectedEmailDetails);
  }

  /**
   * Test the sumbitForm throws an exception when the form details have null values
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitFormThrowsExceptionWithNullFields() throws Exception {
    // SET UP - create our form data JsonNode
    final String formPath = "formSamples/submissionFormNullTest.json";
    final JsonNode formData = createForm(formPath);
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
   * Test the sumbitForm throws an exception when the form details have null values
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
   * Test the submitForm throws an exception when the email fails to send
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitFormThrowsExceptionWhenSendEmailFails() throws Exception {
    // SET UP - create our form data JsonNode
    final String formPath = "formSamples/submissionFormTest.json";
    final JsonNode formData = createForm(formPath);
    final String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT - stub the void method to do throw an exception when called
    doThrow(new RuntimeException("Email failed to send"))
        .when(termFormService)
        .sendEmail(any(EmailDetails.class));

    // ASSERT
    final Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              termSuggestionFormController.submitForm(formData, null, recaptchaToken);
            });
    assertTrue(exception.getMessage().contains(expectedResponse));
  }

  @Test
  public void testSubmitFormRecaptchaVerificationPasses() throws Exception {
    // SET UP
    final String formPath = "formSamples/submissionFormTest.json";
    final JsonNode formData = createForm(formPath);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);
    doNothing().when(termFormService).sendEmail(any(EmailDetails.class));

    // ACT & ASSERT
    try {
      termSuggestionFormController.submitForm(formData, null, recaptchaToken);
    } catch (ResponseStatusException e) {
      fail("Should not have thrown any exception");
    }
  }

  @Test
  public void testSubmitFormRecaptchaVerificationFails() throws Exception {
    // SET UP
    final String formPath = "formSamples/submissionFormTest.json";
    final JsonNode formData = createForm(formPath);
    // Create expected EmailDetails

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(false);
    doNothing().when(termFormService).sendEmail(any(EmailDetails.class));

    // ACT & ASSERT
    try {
      termSuggestionFormController.submitForm(formData, null, recaptchaToken);
      fail("Expected a ResponseStatusException to be thrown");
    } catch (ResponseStatusException e) {
      assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }
  }

  /**
   * Integration test to verify we can call the api path, path the formType, and return the JsonNode
   * form
   *
   * @throws Exception exception
   */
  @Test
  public void integrationTestGetFormTemplate() throws Exception {
    // SET UP
    final String formType = "ncit-form";
    final String url = baseUrl + formType;
    JsonNode form;

    // ACT
    log.info("Testing url: {}", url);
    final MvcResult mvcResult =
        this.mvc.perform(MockMvcRequestBuilders.get(url)).andExpect(status().isOk()).andReturn();
    form = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    log.info("Form = {}", form);

    // ASSERT
    assertNotNull(form);
    assertFalse(form.isEmpty());
    assertEquals("NCIt Term Suggestion Request", form.get("formName").asText());
    // TODO: Update this to ncithesaurus@mail.nih.gov after the form is updated
    assertEquals("agarcia@westcoastinformatics.com", form.get("recipientEmail").asText());
  }

  /**
   * Integration test for submitting a filled out form and sending the email. NOTE: Set your local
   * environment variables in your config. Your test email will need an App Password for access, if
   * using Gmail.
   *
   * @throws Exception exception
   */
  @Test
  public void integrationTestSubmitForm() throws Exception {
    // SET UP
    final String formPath = "formSamples/submissionFormTest.json";
    final JsonNode formData = createForm(formPath);
    final String requestBody = objectMapper.writeValueAsString(formData);

    // Mock the RecaptchaService to always return true for verifyRecaptcha
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);

    // ACT
    log.info("Form data = {}", formData);
    final MvcResult result =
        this.mvc
            .perform(
                MockMvcRequestBuilders.post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Captcha-Token", recaptchaToken)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andReturn();

    // ASSERT
    EmailDetails expectedEmailDetails = EmailDetails.generateEmailDetails(formData);
    verify(termFormService, times(1)).sendEmail(expectedEmailDetails);
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
