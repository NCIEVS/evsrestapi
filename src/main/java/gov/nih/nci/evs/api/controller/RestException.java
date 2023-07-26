
package gov.nih.nci.evs.api.controller;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An payload for exceptions.
 */
@Schema(description = "Payload for JSON error responses")
public class RestException {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(RestException.class);

  /** The timestamp. */
  private Date timestamp;

  /** The status. */
  private int status;

  /** The error. */
  private String error;

  /** The message. */
  private String message;

  /** The path. */
  private String path;

  /**
   * Instantiates an empty {@link RestException}.
   */
  public RestException() {
    // n/a
  }

  /**
   * Instantiates an empty {@link RestException}.
   *
   * @param map the map
   */
  public RestException(Map<String, Object> map) {
    if (map.containsKey("timestamp")) {
      timestamp = ((Date) map.get("timestamp"));
    }
    if (map.containsKey("status")) {
      status = Integer.parseInt(map.get("status").toString());
    }
    if (map.containsKey("error")) {
      error = map.get("error").toString();
    }
    if (map.containsKey("message")) {
      message = map.get("message").toString();
    }
    if (map.containsKey("path")) {
      path = map.get("path").toString();
    }
  }

  /**
   * Returns the timestamp.
   *
   * @return the timestamp
   */
  public Date getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the timestamp.
   *
   * @param timestamp the timestamp
   */
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Returns the status.
   *
   * @return the status
   */
  public int getStatus() {
    return status;
  }

  /**
   * Sets the status.
   *
   * @param status the status
   */
  public void setStatus(int status) {
    this.status = status;
  }

  /**
   * Returns the error.
   *
   * @return the error
   */
  public String getError() {
    return error;
  }

  /**
   * Sets the error.
   *
   * @param error the error
   */
  public void setError(String error) {
    this.error = error;
  }

  /**
   * Returns the message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message.
   *
   * @param message the message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Returns the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path.
   *
   * @param path the path
   */
  public void setPath(String path) {
    this.path = path;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    return Objects.hash(error, message, path, status, timestamp);
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RestException other = (RestException) obj;
    return Objects.equals(error, other.error) && Objects.equals(message, other.message)
        && Objects.equals(path, other.path) && Objects.equals(status, other.status)
        && Objects.equals(timestamp, other.timestamp);
  }

}