package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Test class for the Term Form Controller. Unlike TermSuggestionFormControllerTests here we call real captcha and email service.
 * So to run this you need to set some stuff up. Otherwise these tests will be skipped.
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
@EnabledIfEnvironmentVariable(named = "MAIL_USER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "MAIL_PASSWORD", matches = ".+")
public class TermSuggestionFormControllerEmailTests {

  /** The Constant log. */
  // logger
  private static final Logger log =
      LoggerFactory.getLogger(TermSuggestionFormControllerEmailTests.class);

  /** The mvc. */
  // Mock the MVC automatically
  @Autowired private MockMvc mvc;

  /** The base url. */
  // Base url for api calls
  private String baseUrl = "/api/v1/submit/";
  ;

  private String recaptchaToken = "TEST-KEY";

  /** The object mapper. */
  @Qualifier("objectMapper")
  @Autowired
  private ObjectMapper objectMapper;

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
    final String formPath = "formSamples/submissionFormTest.json";
    JsonNode formData = createForm(formPath);

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
    final String formPath = "formSamples/submissionFormTestCDISC.json";
    JsonNode formData = createForm(formPath);

    // Prepare multipart file from resources
    final org.springframework.mock.web.MockMultipartFile attachment;
    try (final InputStream is =
        getClass().getClassLoader().getResourceAsStream("formSamples/filled-form-submission.xls")) {
      if (is == null) {
        fail("Test attachment not found in resources");
        return;
      }
      attachment =
          new org.springframework.mock.web.MockMultipartFile(
              "file", "filled-form-submission.xls", "application/vnd.ms-excel", is);
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
}
