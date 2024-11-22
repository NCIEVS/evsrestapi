package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.RecaptchaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class CaptchaServiceTest {
  // Logger
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(CaptchaServiceTest.class);

  @Mock private RestTemplate restTemplate;

  @Mock private RestTemplateBuilder restTemplateBuilder;

  private CaptchaService captchaService;

  String token = "validToken";

  /** Setup before each test. */
  @BeforeEach
  public void setUp() {
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    captchaService = new CaptchaService(restTemplateBuilder);
  }

  /** Test verifyRecaptcha method returns a success with a valid token (mocked) */
  @SuppressWarnings("unchecked")
  @Test
  public void testVerifyRecaptchaSuccess() {
    // SETUP
    RecaptchaResponse mockResponse = new RecaptchaResponse();
    mockResponse.setSuccess(true);
    captchaService.recaptchaServerUrl = "fakeUrlTest";

    when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
        .thenReturn(mockResponse);

    // ACT
    Boolean result = captchaService.verifyRecaptcha(token);

    // ASSERT
    assertTrue(result);
  }

  /** Test verifyRecaptcha method return a failure when server url is not specified */
  @Test
  public void testVerifyRecaptchaServerUrlNotSpecifiedThrowsException() {
    // SETUP
    captchaService.recaptchaServerUrl = null;

    // ACT & ASSERT
    assertThrows(
        NullPointerException.class,
        () -> {
          captchaService.verifyRecaptcha(token);
        });
  }

  /** Test verifyRecaptcha method return a failure when the token isn't verifiable */
  @Test
  public void testVerifyRecaptchaVerificationFails() {
    // Arrange
    RecaptchaResponse mockResponse = new RecaptchaResponse();
    mockResponse.setSuccess(false);
    captchaService.recaptchaServerUrl = "fakeUrlTest";

    when(restTemplate.postForObject(
            any(String.class), any(org.springframework.http.HttpEntity.class), any()))
        .thenReturn(mockResponse);

    // Act
    Boolean result = captchaService.verifyRecaptcha("failedToken");

    // Assert
    assertFalse(result);
  }

  /** Test verifyRecaptcha method throws an exception when the RestTemplate throws an exception */
  @Test
  public void testVerifyRecaptchaExceptionThrown() {
    // SETUP
    captchaService.recaptchaServerUrl = "fakeUrlTest";

    when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any()))
        .thenThrow(new RestClientException("Service Unavailable"));

    // ACT & ASSERT
    assertThrows(
        RestClientException.class,
        () -> {
          captchaService.verifyRecaptcha(token);
        });
  }
}
