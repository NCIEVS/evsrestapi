package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.RecaptchaResponse;
import org.apache.http.HttpEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class CaptchaServiceTest {
  // Logger
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(TermSuggestionFormServiceImpl.class);

  @Mock private RestTemplate restTemplate;

  @Mock private RestTemplateBuilder restTemplateBuilder;

  @InjectMocks private CaptchaService captchaService;

  /** Setup before each test. */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /** Test verifyRecaptcha method return a success */
  @Test
  public void verifyRecaptcha_Success() {
    // SETUP
    RecaptchaResponse mockResponse = new RecaptchaResponse();
    mockResponse.setSuccess(true);

    when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
        .thenReturn(mockResponse);

    // ACT
    Boolean result = captchaService.verifyRecaptcha("validToken");

    // ASSERT
    assertFalse(result);
  }

  /** Test verifyRecaptcha method return a failure when server url is not specified */
  @Test
  public void verifyRecaptcha_ServerUrlNotSpecified() {
    // SETUP
    captchaService.recaptchaServerUrl = null;

    // ACT
    Boolean result = captchaService.verifyRecaptcha("testToken");

    // ASSERT
    assertFalse(result);
  }

  /** Test verifyRecaptcha method return a failure when the toke isn't verifiable */
  @Test
  public void verifyRecaptcha_VerificationFails() {
    // Arrange
    RecaptchaResponse mockResponse = new RecaptchaResponse();
    mockResponse.setSuccess(false);
    when(restTemplate.postForObject(
            any(String.class), any(org.springframework.http.HttpEntity.class), any(Class.class)))
        .thenReturn(mockResponse);

    // Act
    Boolean result = captchaService.verifyRecaptcha("failedToken");

    // Assert
    assertFalse(result);
  }
}
