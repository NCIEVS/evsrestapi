package gov.nih.nci.evs.api.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/** Test class for the EmailDetails Model. Ensure our model is generating the email as expected */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class EmailDetailsTest {
  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(AssociationUnitTest.class);

  // JsonObject variable for loading json test file
  JsonObject testFormObject;

  // email detail object
  EmailDetails testDetails;

  @Before
  public void setup() throws Exception {}

  /**
   * Test our EmailDetails is generated with the values from our submitted json file
   *
   * @throws Exception exception
   */
  @Test
  public void testGenerateEmailDetails() throws Exception {
    // SETUP - create JsonObject from the json test form
    String formPath = "formSamples/submissionFormTest.json";
    testFormObject = createJsonObject(formPath);
    assertNotNull(testFormObject);

    // ACT - generate the email details model
    testDetails = EmailDetails.generateEmailDetails(testFormObject);

    // ASSERT
    assertNotNull(testDetails);
    assertEquals("NCIT", testDetails.getSource());
    assertEquals("ncithesaurus@mail.nih.gov", testDetails.getToEmail());
    assertEquals("submitterEmail@gmail.com", testDetails.getFromEmail());
    assertTrue(testDetails.getMsgBody().contains("test body"));
  }

  /**
   * Test the mdoel will throw an exception if there are null fields
   *
   * @throws Exception exception
   */
  @Test
  public void testGenerateEmailDetailsThrowsExceptionWithNullFields() throws Exception {
    // SETUP
    String formPath = "formSamples/submissionFormNullTest.json";
    testFormObject = createJsonObject(formPath);
    assertNotNull(testFormObject);

    // ACT & ASSERT
    assertThrows(
        Exception.class,
        () -> {
          testDetails = EmailDetails.generateEmailDetails(testFormObject);
        });
  }

  /**
   * Test the model will throw an exception when a null form is passed
   *
   * @throws Exception exception
   */
  @Test
  public void testGenerateEmailDetailsThrowsExceptionWithNullForm() throws Exception {
    // SETUP

    // ACT & ASSERT
    assertThrows(
        Exception.class,
        () -> {
          testDetails = EmailDetails.generateEmailDetails(null);
        });
  }

  /**
   * Help method to create the JsonObject from a json file representing our submitted form data
   *
   * @param formPath path of the file to convert to JsonObject
   * @return JsonObject
   */
  private JsonObject createJsonObject(String formPath) {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(formPath)) {
      // Verify the input file loaded
      if (input == null) {
        throw new FileNotFoundException("Test file not found: " + formPath);
      }
      // Parse the file into a JSON
      return JSON.parse(input).getAsObject();
    } catch (IOException e) {
      logger.error("Error creating JsonObject: " + e);
      return null;
    }
  }
}
