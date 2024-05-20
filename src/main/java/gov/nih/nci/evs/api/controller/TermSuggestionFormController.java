package gov.nih.nci.evs.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.TermSuggestionFormServiceImpl;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for /suggest endpoints. */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Terminology form endpoints")
public class TermSuggestionFormController extends BaseController {
  // Logger
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormController.class);

  // term form email service
  private final TermSuggestionFormServiceImpl emailService;

  /**
   * Instantiates a new Term suggestion form controller with params.
   *
   * @param emailService Form Email Service dependency
   */
  public TermSuggestionFormController(TermSuggestionFormServiceImpl emailService) {
    this.emailService = emailService;
  }

  /**
   * Get form template for a given form type.
   *
   * @param formType the JSON form to load
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
      JsonNode formTemplate = emailService.getFormTemplate(formType);
      return ResponseEntity.ok().body(formTemplate);
    } catch (Exception e) {
      logger.error("Error reading form template: " + formType);
      handleException(e);
      return ResponseEntity.internalServerError().body("An error occurred while loading form");
    }
  }

  /**
   * Submit form data to email service.
   *
   * @param formData data from the completed term suggestion form
   * @return ResponseEntity
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
  public ResponseEntity<?> submitForm(
      @RequestBody JsonNode formData,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    try {
      // convert the form data into our email details object
      EmailDetails emailDetails = EmailDetails.generateEmailDetails(formData);

      // Send the email with our email details
      emailService.sendEmail(emailDetails);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      logger.error("Error creating email details or sending email");
      handleException(e);
      return ResponseEntity.internalServerError().body("An error occurred while submitting form");
    }
  }
}
