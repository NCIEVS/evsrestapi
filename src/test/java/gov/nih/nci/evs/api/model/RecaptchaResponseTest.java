package gov.nih.nci.evs.api.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * RecaptchaResponseTest test class.
 */
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class RecaptchaResponseTest {
  RecaptchaResponse response;

  /**
   * Setup method to instanitate an instance of the Recaptcha response
   */
  @Before
  public void setUp() {
    response = new RecaptchaResponse();
  }

  /**
   * Test the isSuccess method
   */
  @Test
  public void testIsSuccess() {
    // ACT
    response.setSuccess(true);
    // ASSERT
    assertTrue(response.isSuccess());
  }

  /**
   * Test the setSuccess method
   */
  @Test
  public void testSetSuccess() {
    // ACT
    response.setSuccess(false);

    // ASSERT
    assertFalse(response.isSuccess());
  }

  /**
   * Test the getHostname method
   */
  @Test
  public void testGetHostname() {
    // SETUP
    String hotsname = "TestHotsName";

    // ACT
    response.setHostname(hotsname);

    // ASSERT
    assertEquals(hotsname, response.getHostname());
  }

  /**
   * Test the setHostname method
   */
  @Test
  public void testSetHostname() {
    // SETUP
    String hostName = "NewHostName";

    // ACT
    response.setHostname(hostName);

    // ASSERT
    assertEquals(hostName, response.getHostname());
  }

  /**
   * Test the getChallenge_ts method
   */
  @Test
  public void testGetChallenge_ts() {
    // SETUP
    String ts = "2024-06-06 00:00:00";

    // ACT
    response.setChallenge_ts(ts);

    // ASSERT
    assertEquals(ts, response.getChallenge_ts());
  }

  /**
   * Test the setChallenge_ts method
   */
  @Test
  public void testSetChallenge_ts() {
    String ts = "2023-06-09 00:14:00";

    // ACT
    response.setChallenge_ts(ts);

    // ASSERT
    assertEquals(ts, response.getChallenge_ts());
  }

  @Test
  public void testGetErrorCodes() {
    // SETUP
    String[] errors = new String[] {"testError"};

    // ACT
    response.setErrorCodes(errors);

    // ASSERT
    assertArrayEquals(errors, response.getErrorCodes());
  }

  @Test
  public void testSetErrorCodes() {
    // SETUP
    String[] errors = new String[] {"NEWtestError"};

    // ACT
    response.setErrorCodes(errors);

    // ASSERT
    assertArrayEquals(errors, response.getErrorCodes());
  }
}
