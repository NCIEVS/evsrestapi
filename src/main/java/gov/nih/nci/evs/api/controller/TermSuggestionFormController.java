package gov.nih.nci.evs.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.FormEmailServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  private final FormEmailServiceImpl emailService;

  /**
   * Instantiates a new Term suggestion form controller with params.
   *
   * @param emailService Form Email Service dependency
   */
  public TermSuggestionFormController(FormEmailServiceImpl emailService) {
    this.emailService = emailService;
  }

  /**
   * Get form template for a given form type.
   *
   * @param formType the JSON form to load
   * @return Response status and form, if successful
   * @throws Exception exception/ioexception
   */
  @GetMapping("/suggest/{formType}")
  public ResponseEntity<?> getForm(@PathVariable String formType) throws Exception {
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
  @PostMapping("/suggest")
  public ResponseEntity<?> submitForm(@RequestBody JsonNode formData) throws Exception {
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
