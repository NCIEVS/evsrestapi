package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.service.CaptchaService;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Exercises form handling and MIME generation with captcha mocked. Mail delivery is mocked when
 * credentials are missing or placeholders, or TEST_EMAIL_DISABLED=true. With configured mail
 * credentials and delivery enabled, these tests send real email to the configured recipients.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TermSuggestionFormControllerEmailTests {

  /** The Constant log. */
  // logger
  private static final Logger log =
      LoggerFactory.getLogger(TermSuggestionFormControllerEmailTests.class);

  /** The mvc. */
  // Mock the MVC automatically
  @Autowired private MockMvc mvc;

  /** Captcha verification is independent of the submission/email behavior tested here. */
  @MockitoBean private CaptchaService captchaService;

  /** Preserve MIME generation and mock only delivery when SMTP is not configured for testing. */
  @MockitoSpyBean private JavaMailSender mailSender;

  /** The resolved test configuration. */
  @Autowired private Environment environment;

  /** Use local templates while retaining the real form service and recipient validation. */
  @MockitoSpyBean private ApplicationProperties applicationProperties;

  /** Local form templates for this test. */
  @TempDir private Path templates;

  /**
   * Set up local forms, deterministic captcha, and real or mocked mail delivery.
   *
   * @throws Exception if a form fixture cannot be copied
   */
  @BeforeEach
  void setUpSubmission() throws Exception {
    copyTemplate("testNCIT.json", "ncit-form.json");
    copyTemplate("submissionFormTest-cdisc.json", "cdisc-form.json");
    doReturn(templates.toString()).when(applicationProperties).getConfigBaseUri();
    when(captchaService.verifyRecaptcha(anyString())).thenReturn(true);
    final boolean mockDelivery =
        environment.getProperty("TEST_EMAIL_DISABLED", Boolean.class, false)
            || !isConfigured(environment.getProperty("spring.mail.host"))
            || !isConfigured(environment.getProperty("spring.mail.username"))
            || !isConfigured(environment.getProperty("spring.mail.password"));
    if (mockDelivery) {
      doReturn(new MimeMessage((Session) null)).when(mailSender).createMimeMessage();
      doNothing().when(mailSender).send(any(MimeMessage.class));
    }
    log.info(
        "Form submission tests: captcha mocked; mail delivery {}",
        mockDelivery ? "mocked" : "REAL");
  }

  /**
   * Copy a bundled form fixture under the name expected by the form service.
   *
   * @param fixture the fixture filename
   * @param filename the form template filename
   * @throws Exception if the fixture cannot be copied
   */
  private void copyTemplate(final String fixture, final String filename) throws Exception {
    try (final InputStream input =
        getClass().getClassLoader().getResourceAsStream("formSamples/" + fixture)) {
      assertNotNull(input);
      Files.copy(input, templates.resolve(filename));
    }
  }

  /**
   * Check whether a mail setting has a non-placeholder value.
   *
   * @param value the setting
   * @return whether the setting is configured
   */
  private boolean isConfigured(final String value) {
    return value != null && !value.isBlank() && !value.startsWith("REPLACE_WITH_");
  }

  /** The base url. */
  // Base url for api calls
  private String baseUrl = "/api/v1/submit/";

  /** The recaptcha token. */
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
   * Integration test for submitting a filled out form and handing its email to the mail sender.
   *
   * @throws Exception exception
   */
  @Test
  public void integrationTestSubmitForm() throws Exception {
    // SET UP
    baseUrl = "/api/v1/form/submit";
    final String formPath = "formSamples/submissionFormTest-ncit.json";
    JsonNode formData = createForm(formPath);

    // Load the form template as the browser flow does, so the service allows this recipient
    this.mvc
        .perform(MockMvcRequestBuilders.get("/api/v1/form/suggest/ncit-form"))
        .andExpect(status().isOk());

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
    verify(mailSender).send(any(MimeMessage.class));
  }

  /**
   * Integration test submit form with attachment.
   *
   * @throws Exception the exception
   */
  @Test
  public void integrationTestSubmitFormWithAttachment() throws Exception {
    // SET UP
    baseUrl = "/api/v1/form/submitWithAttachment";
    final String formPath = "formSamples/submissionFormTest-cdisc.json";
    JsonNode formData = createForm(formPath);

    // Load the form template as the browser flow does, so the service allows this recipient
    this.mvc
        .perform(MockMvcRequestBuilders.get("/api/v1/form/suggest/cdisc-form"))
        .andExpect(status().isOk());

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
    verify(mailSender).send(any(MimeMessage.class));
  }

  /**
   * Integration test submit form with attachment NCIT.
   *
   * @throws Exception the exception
   */
  @Test
  public void integrationTestSubmitFormWithAttachmentNCIT() throws Exception {
    // SET UP
    baseUrl = "/api/v1/form/submitWithAttachment";
    final String formPath = "formSamples/testNCIT.json";
    JsonNode formData = createForm(formPath);

    // Load the form template as the browser flow does, so the service allows this recipient
    this.mvc
        .perform(MockMvcRequestBuilders.get("/api/v1/form/suggest/ncit-form"))
        .andExpect(status().isOk());

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
    verify(mailSender).send(any(MimeMessage.class));
  }

  /**
   * Helper method for creating a JsonNode from a Json file.
   *
   * @param path path for the json file
   * @return JsonNode
   * @throws Exception exception
   */
  private JsonNode createForm(final String path) throws Exception {
    final ObjectMapper mapper = ThreadLocalMapper.get();
    // read the file as an Input Stream
    try (final InputStream input = getClass().getClassLoader().getResourceAsStream(path); ) {
      // Set our expected response to the form from the formPath
      return mapper.readTree(input);
    }
  }
}
