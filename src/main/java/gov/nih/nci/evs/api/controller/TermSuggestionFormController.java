package gov.nih.nci.evs.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.CaptchaService;
import gov.nih.nci.evs.api.service.TermSuggestionFormService;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Controller for /submit endpoints. Hidden from Swagger/OpenAPI. */
@Hidden
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
public class TermSuggestionFormController extends BaseController {

  /** The Constant logger. */
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormController.class);

  /** The email service. */
  // term form email service
  private final TermSuggestionFormService formService;

  private final CaptchaService captchaService;

  /**
   * Instantiates a new Term suggestion form controller with params.
   *
   * @param emailService Form Email Service dependency
   */
  public TermSuggestionFormController(
      TermSuggestionFormService emailService, CaptchaService captchaService) {
    this.formService = emailService;
    this.captchaService = captchaService;
  }

  /**
   * Get form template for a given form type.
   *
   * @param formType the JSON form to load
   * @param license the license
   * @return Response status and form, if successful
   * @throws Exception exception/ioexception
   */
  @GetMapping("/form/suggest/{formType}")
  @RecordMetric
  public ResponseEntity<?> getForm(
      @PathVariable(value = "formType") final String formType,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    // Try getting the form and return form template
    try {
      JsonNode formTemplate = formService.getFormTemplate(formType);
      if (formTemplate.isEmpty() || formTemplate.isNull()) {
        logger.error("Returned Form Is Empty/Null");
        throw new Exception("Error retrieving the form");
      }
      return ResponseEntity.ok().body(formTemplate);
    } catch (Exception e) {
      logger.error("Error reading form template: {}", formType, e);
      handleException(e, null);
      throw e;
    }
  }

  /**
   * Submit form data to email service.
   *
   * @param formData data from the completed term suggestion form
   * @param license the license
   * @return ResponseEntity the response
   * @throws Exception the exception
   */
  @PostMapping("/form/submit")
  @RecordMetric
  public void submitForm(
      @RequestBody JsonNode formData,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license,
      @RequestHeader(name = "Captcha-Token") final String captchaToken)
      throws Exception {
    // Try sending the email
    try {
      // Verify our captcha token
      if (!captchaService.verifyRecaptcha(captchaToken)) {
        logger.error("Failed to verify the submitted Recaptcha!");
        throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Unable to submit form\n");
      }

      // convert the form data into our email details object
      EmailDetails emailDetails = EmailDetails.generateEmailDetails(formData);
      // Send the email
      formService.sendEmail(emailDetails);
    } catch (Exception e) {
      logger.error("Error creating email details or sending email", e);
      handleException(e, null);
    }
  }

  /**
   * Submit form data with an optional attachment.
   *
   * <p>Sample call in curl form:
   *
   * <pre>
   * curl -X POST "http://localhost:8082/api/v1/form/submitWithAttachment" \
   * -H "Captcha-Token: TEST-KEY" \
   * -F 'formData=@src/test/resources/formSamples/submissionFormTest-cdisc.json;type=application/json' \
   * -F "file=@src/test/resources/formSamples/filled-form-submission-cdisc.xls"
   * </pre>
   *
   * <p>Accepts multipart/form-data with a JSON part named `formData` and an optional file part
   * named `file`.
   *
   * @param formData the form data
   * @param file the file
   * @param license the license
   * @param captchaToken the captcha token
   * @throws Exception the exception
   */
  @PostMapping(
      path = "/form/submitWithAttachment",
      consumes = {"multipart/form-data"})
  @RecordMetric
  public void submitWithAttachment(
      @RequestPart("formData") JsonNode formData,
      @RequestPart(name = "file", required = false) MultipartFile file,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license,
      @RequestHeader(name = "Captcha-Token") final String captchaToken)
      throws Exception {
    try {

      // Verify our captcha token
      if (!captchaService.verifyRecaptcha(captchaToken)) {
        logger.error("Failed to verify the submitted Recaptcha!");
        throw new ResponseStatusException(
            HttpStatus.EXPECTATION_FAILED,
            "Unable to submit form. Failed to verify the submitted Recaptcha!");
      }

      // Convert the form data into our email details object first to get form type
      EmailDetails emailDetails = EmailDetails.generateEmailDetails(formData);
      final String invalidReason =
          formService.validateFileAttachmentReason(file, emailDetails.getSource());
      if (invalidReason != null) {
        logger.error("Invalid attachment file: {}", invalidReason);
        throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, invalidReason);
      }

      // convert the form data into our email details object
      if (!"CDISC".equals(emailDetails.getSource()) && !"NCIT".equals(emailDetails.getSource())) {
        logger.error("Form type is not valid for attachment. Must be CDISC or NCIT.");
        throw new ResponseStatusException(
            HttpStatus.EXPECTATION_FAILED,
            "Invalid form type for attachment. Must be CDISC or NCIT.");
      }

      // Validate file attachment with form-type-specific validation
      if (!formService.validateFileAttachment(file, emailDetails.getSource())) {
        logger.error(
            "Invalid attachment file for {}, does not match the template.",
            emailDetails.getSource());
        throw new ResponseStatusException(
            HttpStatus.EXPECTATION_FAILED,
            "Invalid attachment file for "
                + emailDetails.getSource()
                + ", does not match the template.");
      }

      // Send the email with optional attachment
      formService.sendEmailWithAttachment(emailDetails, file);
    } catch (Exception e) {
      logger.error("Error creating email details or sending email with attachment", e);
      handleException(e, null);
    }
  }
}
