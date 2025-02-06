package gov.nih.nci.evs.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.CaptchaService;
import gov.nih.nci.evs.api.service.TermSuggestionFormServiceImpl;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Controller for /suggest endpoints. */
@Hidden
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Terminology form endpoints")
public class TermSuggestionFormController extends BaseController {

  /** The Constant logger. */
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormController.class);

  /** The email service. */
  // term form email service
  private final TermSuggestionFormServiceImpl formService;

  private final CaptchaService captchaService;

  /**
   * Instantiates a new Term suggestion form controller with params.
   *
   * @param emailService Form Email Service dependency
   */
  public TermSuggestionFormController(
      TermSuggestionFormServiceImpl emailService, CaptchaService captchaService) {
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
  @Operation(summary = "Get the suggestion form based on type parameter")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "formType",
        description = "type of form, e.g. ncit-form, cdisc-form",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit-form"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @GetMapping("/suggest/{formType}")
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
      return null;
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
  @Operation(
      summary =
          "Receives the suggestion formData as JsonNode, converts to EmailDetails object and "
              + "sends the email")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "The required suggestion term form filled out",
      required = true,
      content = @Content(schema = @Schema(implementation = JsonNode.class)))
  @Parameters({
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @PostMapping("/suggest")
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
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to submit form\n");
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
}
