
package gov.nih.nci.evs.api.model;

import java.util.Date;
import java.util.Map;

/**
 * Metric.
 */
public class Metric {

  /** The remote ip address. */
  private String remoteIpAddress;

  /** The end point. */
  private String endPoint;

  /** The query params. */
  private Map<String, String[]> queryParams;

  /** The start time. */
  private Date startTime;

  /** The end time. */
  private Date endTime;

  /** The duration. */
  private Long duration;

  /** The hostname */
  private String hostName;

  /**
   * Returns the remote ip address.
   *
   * @return the remote ip address
   */
  public String getRemoteIpAddress() {
    return remoteIpAddress;
  }

  /**
   * Sets the remote ip address.
   *
   * @param remoteIpAddress the remote ip address
   */
  public void setRemoteIpAddress(String remoteIpAddress) {
    this.remoteIpAddress = remoteIpAddress;
  }

  /**
   * Returns the end point.
   *
   * @return the end point
   */
  public String getEndPoint() {
    return endPoint;
  }

  /**
   * Sets the end point.
   *
   * @param endPoint the end point
   */
  public void setEndPoint(String endPoint) {
    this.endPoint = endPoint;
  }

  /**
   * Returns the query params.
   *
   * @return the query params
   */
  public Map<String, String[]> getQueryParams() {
    return queryParams;
  }

  /**
   * Sets the query params.
   *
   * @param queryParams the query params
   */
  public void setQueryParams(Map<String, String[]> queryParams) {
    this.queryParams = queryParams;
  }

  /**
   * Returns the start time.
   *
   * @return the start time
   */
  public Date getStartTime() {
    return startTime;
  }

  /**
   * Sets the start time.
   *
   * @param startTime the start time
   */
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  /**
   * Returns the end time.
   *
   * @return the end time
   */
  public Date getEndTime() {
    return endTime;
  }

  /**
   * Sets the end time.
   *
   * @param endTime the end time
   */
  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  /**
   * Returns the duration.
   *
   * @return the duration
   */
  public Long getDuration() {
    return duration;
  }

  /**
   * Sets the duration.
   *
   * @param duration the duration
   */
  public void setDuration(Long duration) {
    this.duration = duration;
  }

    /**
   * Returns the host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets the host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

}
