package gov.nih.nci.evs.api.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.EmailDetails;
import gov.nih.nci.evs.api.service.FormEmailServiceImpl;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    ResponseEntity<?> responseEntity;

    // ACT - mock the email service and call the getForm
    when(emailService.getFormTemplate(formType)).thenReturn(expectedResponse);
    responseEntity = termSuggestionFormController.getForm(formType);

    // ASSERT
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(expectedResponse, responseEntity.getBody());
  }

  /**
   * Test the submitForm successfully sends email with submitted form.
   * @throws Exception exception
   */
  @Test
  public void testSubmitNCITForm() throws Exception {
    // SET UP - create our form data JsonNode
    String formPath = "formSamples/submissionFormTest.json";
    JsonNode formData = createJsonNode(formPath);

    // ACT
    when(emailService.sendEmail(any(EmailDetails.class))).thenReturn(true);
    ResponseEntity<?> responseEntity = termSuggestionFormController.submitForm(formData);

    // ASSERT
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  /**
   * Helper method for creating a JsonNode from a Json file
   * @param path   path for the json file
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
