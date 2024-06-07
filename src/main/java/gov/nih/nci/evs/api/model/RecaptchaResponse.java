package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Recaptcha Response model from the google recaptcha api. */
public class RecaptchaResponse {
  private boolean success;
  private String hostname;
  private String challenge_ts;

  @JsonProperty("error-codes")
  private String[] errorCodes;

  /**
   * Check if the recaptcha response was successful.
   *
   * @return the success response
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Set the success status of the recaptcha response.
   *
   * @param success boolean success response
   */
  public void setSuccess(boolean success) {
    this.success = success;
  }

  /**
   * Get the hostname of the recaptcha response.
   *
   * @return hostname
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Set the hostname of the recaptcha response.
   *
   * @param hostname host name
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * Get the challenge timestamp of the recaptcha response.
   *
   * @return the response challenge timestamp
   */
  public String getChallenge_ts() {
    return challenge_ts;
  }

  /**
   * Set the challenge timestamp of the recaptcha response.
   *
   * @param challenge_ts the response timestamp
   */
  public void setChallenge_ts(String challenge_ts) {
    this.challenge_ts = challenge_ts;
  }

  /**
   * Get the error codes of the recaptcha response.
   *
   * @return response error code array
   */
  public String[] getErrorCodes() {
    return errorCodes;
  }

  /**
   * Set the error codes of the recaptcha response.
   *
   * @param errorCodes response error codes
   */
  public void setErrorCodes(String[] errorCodes) {
    this.errorCodes = errorCodes;
  }
}
