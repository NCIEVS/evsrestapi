package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.RecaptchaResponse;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Service class for verifying the recaptcha token with the google recaptcha server. Source link:
 * https://developers.google.com/recaptcha/docs/verify
 */
@Service
public class CaptchaService {
  private final RestTemplate restTemplate;

  private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);

  @Value("${google.recaptcha.secret.key}")
  public String recaptchaSecret;

  @Value("${google.recaptcha.verify.url}")
  public String recaptchaServerUrl;

  /**
   * Constructor with RestTemplateBuilder arg
   *
   * @param restTemplateBuilder RestTemplateBuilder
   */
  @Autowired
  public CaptchaService(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
  }

  /**
   * Verify the recaptcha token with the google recaptcha server.
   *
   * @param captchaToken recaptcha secret key from the submitted form
   * @return verification response True or False
   */
  public Boolean verifyRecaptcha(String captchaToken) throws NullPointerException, Exception {
    // check if the recaptcha server url is set
    if (recaptchaServerUrl == null || recaptchaServerUrl.isBlank()) {
      logger.error("Recaptcha server URL is not specified");
      Audit audit =
          new Audit(
              "NullPointerException",
              null,
              null,
              new Date(),
              "verifyRecaptcha",
              "Recaptcha server URL is not specified",
              "error");
      LoaderServiceImpl.addAudit(audit);
      throw new NullPointerException("Recaptcha server url is not set");
    }
    // create the request headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // create the request body
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("secret", recaptchaSecret);
    map.add("response", captchaToken);

    // send the request to the recaptcha server
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
    RecaptchaResponse verificationResponse =
        restTemplate.postForObject(recaptchaServerUrl, request, RecaptchaResponse.class);

    // log response details
    logger.debug("Recaptcha success = " + verificationResponse.isSuccess());
    logger.debug("Recaptcha hostname = " + verificationResponse.getHostname());
    logger.debug("Recaptcha challenge timestamp =" + verificationResponse.getChallenge_ts());
    if (verificationResponse.getErrorCodes() != null) {
      logger.debug("Recaptcha Errors Found: ");
      for (String error : verificationResponse.getErrorCodes()) {
        logger.debug("\t" + error);
      }
    }
    // return Recaptcha verification response
    return verificationResponse.isSuccess();
  }
}
