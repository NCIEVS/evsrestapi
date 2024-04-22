package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.Map;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

/** Metric. */
@Schema(hidden = true)
@Document(indexName = "default")
public class Metric {

  /** The remote ip address. */
  @Field(type = FieldType.Text)
  private String remoteIpAddress;

  /** The end point. */
  @Field(type = FieldType.Text)
  private String endPoint;

  /** The query params. */
  @Field(type = FieldType.Object)
  private Map<String, String[]> queryParams;

  /** The start time. */
  @Field(
      type = FieldType.Date,
      store = true,
      format = DateFormat.custom,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private Date startTime;

  /** The end time. */
  @Field(
      type = FieldType.Date,
      store = true,
      format = DateFormat.custom,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private Date endTime;

  /** The duration. */
  @Field(type = FieldType.Long)
  private Long duration;

  /** The hostname */
  @Field(type = FieldType.Text)
  private String hostName;

  /** The geoPoint object */
  private GeoPoint geoPoint;

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

  /**
   * @return the Geoip
   */
  public GeoPoint getGeoPoint() {
    return geoPoint;
  }

  /**
   * @param ipInfo the Geoip
   */
  public void setGeoPoint(GeoPoint geoPoint) {
    this.geoPoint = geoPoint;
  }

  /* see superclass */
  @Override
  public String toString() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (final Exception e) {
      return e.getMessage();
    }
  }
}
