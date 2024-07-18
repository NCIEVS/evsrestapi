package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.RecaptchaResponse;
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
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
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
  @SuppressWarnings("resource")
  @Before
  public void setUp() {
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    captchaService = new CaptchaService(restTemplateBuilder);
  }

  /** Test verifyRecaptcha method return a success */
  @SuppressWarnings("unchecked")
  @Test
  public void verifyRecaptcha_Success_Test() {
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
  public void verifyRecaptcha_ServerUrl_NotSpecified_ThrowsException_Test() {
    // SETUP
    captchaService.recaptchaServerUrl = null;

    // ACT & ASSERT
    assertThrows(NullPointerException.class, () -> {
      captchaService.verifyRecaptcha(token);
    });
  }

  /** Test verifyRecaptcha method return a failure when the token isn't verifiable */
  @SuppressWarnings("unchecked")
  @Test
  public void verifyRecaptcha_Verification_Fails_Test() {
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

  @Test
  public void verifyRecaptcha_ExceptionThrown() {
    // SETUP
    captchaService.recaptchaServerUrl = "fakeUrlTest";

    when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any())).thenThrow(new RestClientException("Service Unavailable"));

    // ACT & ASSERT
    assertThrows(RestClientException.class, () -> {
      captchaService.verifyRecaptcha(token);
    });
  }
}
