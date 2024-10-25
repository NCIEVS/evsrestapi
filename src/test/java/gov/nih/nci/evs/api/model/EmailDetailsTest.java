package gov.nih.nci.evs.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class for the EmailDetails Model. Ensure our model is generating the email as expected. DOES
 * NOT test the formatBodyRecursive
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class EmailDetailsTest {
  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(EmailDetailsTest.class);

  /** The test form object. */
  // JsonObject variable for loading json test file
  private JsonNode testFormObject;

  /** The test details. */
  // email detail object
  private EmailDetails testDetails;

  @BeforeEach
  public void setUp() {
    testDetails = new EmailDetails();
  }

  /**
   * Test our EmailDetails is generated with the values from our submitted json file.
   *
   * @throws Exception exception
   */
  @Test
  public void testGenerateEmailDetailsPasses() throws Exception {
    // SETUP - create JsonObject from the json test form
    String formPath = "formSamples/submissionFormTest.json";
    testFormObject = createJsonNode(formPath);
    assertNotNull(testFormObject);

    // ACT - generate the email details model
    testDetails = EmailDetails.generateEmailDetails(testFormObject);

    // ASSERT
    assertNotNull(testDetails);
    assertEquals("NCIT", testDetails.getSource());
    assertEquals("agarcia@westcoastinformatics.com", testDetails.getToEmail());
    assertEquals("bcarlsen@westcoastinformatics.com ", testDetails.getFromEmail());
    assertTrue(testDetails.getMsgBody().contains("C65498"));
  }

  /**
   * Test the email body is generated as expected from the submitted form data with an array list
   * present
   *
   * @throws Exception exception
   */
  @Test
  public void testGenerateHtmlEmailBodyHandlesArray() throws Exception {
    // SETUP - create JsonObject
    String formPath = "formSamples/submissionFormWithArrayList.json";
    testFormObject = createJsonNode(formPath);
    assertNotNull(testFormObject);

    // ACT - generate HTML email
    String emailBody = EmailDetails.generateHtmlEmailBody(testFormObject.get("body"));

    // ASSERT
    assertTrue(emailBody.contains("<li>CDISC Subset: <ul>"));
    assertTrue(emailBody.contains("<li>ADaM</li>"));
    assertTrue(emailBody.contains("<li>CDASH</li>"));
    assertTrue(emailBody.contains("<li>CDASH-EXDOSU</li>"));
    assertTrue(emailBody.contains("<li>DDF - Endpoint Level Value Set Terminology</li>"));
  }

  /**
   * Test the mdoel will throw an exception if there are null fields. This fails on the first field,
   * formName, but the logic applies to all fields
   *
   * @throws Exception exception
   */
  @Test
  public void testGenerateEmailDetailsThrowsExceptionWithNullFields() throws Exception {
    // SETUP
    String formPath = "formSamples/submissionFormNullTest.json";
    testFormObject = createJsonNode(formPath);
    assertNotNull(testFormObject);

    // ACT & ASSERT
    assertThrows(
        Exception.class,
        () -> {
          testDetails = EmailDetails.generateEmailDetails(testFormObject);
        });
  }

  /**
   * Test the model will throw an exception when a null form is passed.
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
   * Test the model will throw an exception when an empty form is passed.
   *
   * @throws Exception exception
   */
  @Test
  public void testGenerateEmailDetailsThrowsExceptionWithEmptyForm() throws Exception {
    // SETUP
    ObjectMapper mapper = new ObjectMapper();
    // create an empty form instance
    testFormObject = mapper.createObjectNode();

    // ACT & ASSERT
    assertThrows(
        Exception.class,
        () -> {
          testDetails = EmailDetails.generateEmailDetails(testFormObject);
        });
  }

  /**
   * Test the overridden equal method in Email Details is comparing values as expected. Results
   * should be true
   *
   * @throws Exception exception
   */
  @Test
  public void testEqualsWithEqualEmailDetails() throws Exception {
    // SETUP
    String formPath1 = "formSamples/submissionFormTest.json";
    testFormObject = createJsonNode(formPath1);
    JsonNode compTestFormObject = createJsonNode(formPath1);

    // ACT - populate Email details
    testDetails = EmailDetails.generateEmailDetails(testFormObject);
    EmailDetails compTestDetails = EmailDetails.generateEmailDetails(compTestFormObject);

    // ASSERT - using the equal comparator to test it's working as expected
    assertEquals(testDetails, compTestDetails);
    assertEquals(testDetails.getToEmail(), compTestDetails.getToEmail());
    assertEquals(testDetails.getFromEmail(), compTestDetails.getFromEmail());
    assertEquals(testDetails.getSubject(), compTestDetails.getSubject());
    assertEquals(testDetails.getMsgBody(), compTestDetails.getMsgBody());
  }

  /**
   * Test the overridden equal method in Email Details is comparing values as expected. Results
   * should be false
   *
   * @throws Exception exception
   */
  @Test
  public void testEqualWithDifferentEmailDetails() throws Exception {
    // SETUP
    String formPath1 = "formSamples/submissionFormTest.json";
    String formPath2 = "formSamples/compareEmailDetailsFail.json";
    testFormObject = createJsonNode(formPath1);
    JsonNode compTestFormObject = createJsonNode(formPath2);

    // ACT - populate Email details
    testDetails = EmailDetails.generateEmailDetails(testFormObject);
    EmailDetails compTestDetails = EmailDetails.generateEmailDetails(compTestFormObject);

    // ASSERT
    assertNotEquals(testDetails, compTestDetails);
    assertNotEquals(testDetails.getToEmail(), compTestDetails.getToEmail());
    assertNotEquals(testDetails.getFromEmail(), compTestDetails.getFromEmail());
    assertNotEquals(testDetails.getSubject(), compTestDetails.getSubject());
    assertNotEquals(testDetails.getMsgBody(), compTestDetails.getMsgBody());
  }

  /**
   * Test the overridden equal method in Email Details is comparing values as expected. Results
   * should be false
   *
   * @throws Exception exception
   */
  @Test
  public void testEqualWithNullEmailDetails() throws Exception {
    // SETUP
    String formPath1 = "formSamples/submissionFormTest.json";
    testFormObject = createJsonNode(formPath1);

    // ACT - populate Email details
    testDetails = EmailDetails.generateEmailDetails(testFormObject);

    // ASSERT
    assertFalse(testDetails.equals(null));
  }

  /**
   * Helper method to create the JsonObject from a json file representing our submitted form data.
   *
   * @param formPath path of the file to convert to JsonObject
   * @return JsonObject
   */
  private JsonNode createJsonNode(String formPath) {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(formPath)) {
      // Verify the input file loaded
      if (input == null) {
        throw new FileNotFoundException("Test file not found: " + formPath);
      }
      // create object mapper
      ObjectMapper mapper = new ObjectMapper();
      // Parse the file into a JsonNode
      return mapper.readTree(input);
    } catch (IOException e) {
      logger.error("Error creating JsonObject: " + e);
      return null;
    }
  }
}
