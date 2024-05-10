package gov.nih.nci.evs.api.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.FormEmailServiceImpl;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/** Test class for the Term Form Controller */
@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class TermSuggestionFormControllerTests {
  // Mock the email service
  @Mock FormEmailServiceImpl emailService;

  // create an instance of the controller and inject service
  @InjectMocks TermSuggestionFormController termSuggestionFormController;

  /** Setup method to create a mock request for testing */
  @Before
  public void setUp() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  /**
   * Test the getForm returns our expected response JsonObject when passing formType
   *
   * @throws Exception exception
   */
  @Test
  public void testGetFormTemplate() throws Exception {
    // SET UP
    String formType = "NCIT";
    String formPath = "formSamples/testNCIT.json";
    // read the file as an Input Stream and set our expected response
    JsonNode expectedResponse = createJsonNode(formPath);

    // ACT - mock the email service and call the getForm
    when(emailService.getFormTemplate(formType)).thenReturn(expectedResponse);
    ResponseEntity<?> responseEntity = termSuggestionFormController.getForm(formType, null);

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
    String formType = "CAD"; // form type doesn't exist
    String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // ACT
    when(emailService.getFormTemplate(formType)).thenThrow(new FileNotFoundException());

    // ASSERT
    Exception exception =
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
    String formPath = "formSamples/submissionFormTest.json";
    JsonNode formData = createJsonNode(formPath);

    // ACT - stub the void method to do nothing when called
    doNothing().when(emailService).sendEmail(any(EmailDetails.class));
    ResponseEntity<?> responseEntity = termSuggestionFormController.submitForm(formData, null);

    // ASSERT
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  /**
   * Test the sumbitForm throws an exception when the form details have null values
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitFormThrowsExceptionWithNullFields() throws Exception {
    // SET UP - create our form data JsonNode
    String formPath = "formSamples/submissionFormNullTest.json";
    JsonNode formData = createJsonNode(formPath);
    String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // ACT & ASSERT
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              termSuggestionFormController.submitForm(formData, null);
            });
    assertTrue(exception.getMessage().contains(expectedResponse));
  }

  /**
   * Test the subitForm throws an exception when the email fails to send
   *
   * @throws Exception exception
   */
  @Test
  public void testSubmitFormThrowsExceptionWhenSendEmailFails() throws Exception {
    // SET UP - create our form data JsonNode
    String formPath = "formSamples/submissionFormTest.json";
    JsonNode formData = createJsonNode(formPath);
    String expectedResponse = "500 INTERNAL_SERVER_ERROR";

    // ACT - stub the void method to do throw an exception when called
    doThrow(new RuntimeException("Email failed to send"))
        .when(emailService)
        .sendEmail(any(EmailDetails.class));

    // ASSERT
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              termSuggestionFormController.submitForm(formData, null);
            });
    assertTrue(exception.getMessage().contains(expectedResponse));
  }

  /**
   * Helper method for creating a JsonNode from a Json file
   *
   * @param path path for the json file
   * @return JsonNode
   * @throws Exception exception
   */
  private JsonNode createJsonNode(String path) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    // read the file as an Input Stream
    InputStream input = getClass().getClassLoader().getResourceAsStream(path);
    // Set our expected response to the form from the formPath
    return mapper.readTree(input);
  }
}
