package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import gov.nih.nci.evs.api.service.ElasticOperationsService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.Objects;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/** Represents an audit event captured in the system. */
@Schema(description = "Represents an audit event captured in the system")
@Document(indexName = "audit")
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Audit {

  @Schema(description = "Type of event, e.g., REINDEX or LOADER")
  @Field(type = FieldType.Keyword)
  private String type;

  @Schema(description = "Terminology associated with the event")
  @Field(type = FieldType.Keyword)
  private String terminology;

  @Schema(description = "Version of the terminology")
  @Field(type = FieldType.Keyword)
  private String version;

  @Schema(description = "Date when the event occurred")
  @Field(type = FieldType.Date)
  private Date date;

  @Schema(description = "Start date of the event")
  @Field(type = FieldType.Date)
  private Date startDate;

  @Schema(description = "End date of the event")
  @Field(type = FieldType.Date)
  private Date endDate;

  @Schema(description = "Elapsed time in milliseconds")
  @Field(type = FieldType.Long)
  private long elapsedTime;

  @Schema(description = "Process executing the event, e.g., script or class name")
  @Field(type = FieldType.Keyword)
  private String process;

  @Schema(description = "Count of processed concepts")
  @Field(type = FieldType.Long)
  private long count;

  @Schema(description = "Logging level of the audit event")
  @Field(type = FieldType.Keyword)
  private String logLevel;

  @Schema(description = "Details of the audit event")
  @Field(type = FieldType.Text)
  private String details;

  /** Default constructor. */
  public Audit() {
    // Default constructor
  }

  public Audit(
      String type,
      String terminology,
      String version,
      Date date,
      String process,
      String details,
      String logLevel) {
    // smaller contructor for basic error checking/logging
    this.type = type;
    this.terminology = terminology;
    this.version = version;
    this.date = date;
    this.process = process;
    this.details = details;
    this.logLevel = logLevel;
  }

  public Audit(final Audit audit) {
    this.populateFrom(audit);
  }

  /**
   * Constructs an Audit object with specified values.
   *
   * @param type the type of event
   * @param terminology the terminology associated with the event
   * @param version the version of the terminology
   * @param date the date of occurrence
   * @param startDate the start date of the event
   * @param endDate the end date of the event
   * @param elapsedTime the elapsed time in milliseconds
   * @param process the process executing the event
   * @param count the count of processed records
   */
  public Audit(
      final String type,
      final String terminology,
      final String version,
      final Date date,
      final Date startDate,
      final Date endDate,
      final long elapsedTime,
      final String process,
      final long count,
      final String logLevel,
      final String details) {
    this.type = type;
    this.terminology = terminology;
    this.version = version;
    this.date = date;
    this.startDate = startDate;
    this.endDate = endDate;
    this.elapsedTime = elapsedTime;
    this.process = process;
    this.count = count;
    this.logLevel = logLevel;
    this.details = details;
  }

  /**
   * Returns the type of event.
   *
   * @return the event type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of event.
   *
   * @param type the event type to set
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * Returns the terminology associated with the event.
   *
   * @return the terminology
   */
  public String getTerminology() {
    return terminology;
  }

  /**
   * Sets the terminology associated with the event.
   *
   * @param terminology the terminology to set
   */
  public void setTerminology(final String terminology) {
    this.terminology = terminology;
  }

  /**
   * Returns the version of the terminology.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version of the terminology.
   *
   * @param version the version to set
   */
  public void setVersion(final String version) {
    this.version = version;
  }

  /**
   * Returns the date when the event occurred.
   *
   * @return the event date
   */
  public Date getDate() {
    return date;
  }

  /**
   * Sets the date when the event occurred.
   *
   * @param date the event date to set
   */
  public void setDate(final Date date) {
    this.date = date;
  }

  /**
   * Returns the start date of the event.
   *
   * @return the start date
   */
  public Date getStartDate() {
    return startDate;
  }

  /**
   * Sets the start date of the event.
   *
   * @param startDate the start date to set
   */
  public void setStartDate(final Date startDate) {
    this.startDate = startDate;
  }

  /**
   * Returns the end date of the event.
   *
   * @return the end date
   */
  public Date getEndDate() {
    return endDate;
  }

  /**
   * Sets the end date of the event.
   *
   * @param endDate the end date to set
   */
  public void setEndDate(final Date endDate) {
    this.endDate = endDate;
  }

  /**
   * Returns the elapsed time in milliseconds.
   *
   * @return the elapsed time
   */
  public long getElapsedTime() {
    return elapsedTime;
  }

  /**
   * Sets the elapsed time in milliseconds.
   *
   * @param elapsedTime the elapsed time to set
   */
  public void setElapsedTime(final long elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  /**
   * Returns the process executing the event.
   *
   * @return the process name
   */
  public String getProcess() {
    return process;
  }

  /**
   * Sets the process executing the event.
   *
   * @param process the process name to set
   */
  public void setProcess(final String process) {
    this.process = process;
  }

  /**
   * Returns the count of processed concepts.
   *
   * @return the count
   */
  public long getCount() {
    return count;
  }

  /**
   * Sets the count of processed concepts.
   *
   * @param count the count to set
   */
  public void setCount(final long count) {
    this.count = count;
  }

  /**
   * Returns the log level of the audit event.
   *
   * @return the log level
   */
  public String getLogLevel() {
    return logLevel;
  }

  /**
   * Sets the log level of the audit event.
   *
   * @param logLevel the log level to set
   */
  public void setLogLevel(final String logLevel) {
    this.logLevel = logLevel;
  }

  /**
   * Returns the details of the audit event.
   *
   * @return the details
   */
  public String getDetails() {
    return details;
  }

  /**
   * Sets the details of the audit event.
   *
   * @param details the details to set
   */
  public void setDetails(final String details) {
    this.details = details;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Audit audit = (Audit) o;
    return elapsedTime == audit.elapsedTime
        && count == audit.count
        && Objects.equals(type, audit.type)
        && Objects.equals(terminology, audit.terminology)
        && Objects.equals(version, audit.version)
        && Objects.equals(date, audit.date)
        && Objects.equals(startDate, audit.startDate)
        && Objects.equals(endDate, audit.endDate)
        && Objects.equals(process, audit.process)
        && Objects.equals(logLevel, audit.logLevel)
        && Objects.equals(details, audit.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        type,
        terminology,
        version,
        date,
        startDate,
        endDate,
        elapsedTime,
        process,
        count,
        logLevel,
        details);
  }

  @Override
  public String toString() {
    return "Audit{"
        + "type='"
        + type
        + '\''
        + ", terminology='"
        + terminology
        + '\''
        + ", version='"
        + version
        + '\''
        + ", date="
        + date
        + ", startDate="
        + startDate
        + ", endDate="
        + endDate
        + ", elapsedTime="
        + elapsedTime
        + ", process='"
        + process
        + '\''
        + ", count="
        + count
        + '}'
        + ", logLevel="
        + logLevel
        + '}'
        + ", details="
        + details
        + '}';
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Audit other) {
    this.type = other.getType();
    this.terminology = other.getTerminology();
    this.version = other.getVersion();
    this.date = other.getDate();
    this.startDate = other.getStartDate();
    this.endDate = other.getEndDate();
    this.elapsedTime = other.getElapsedTime();
    this.process = other.getProcess();
    this.count = other.getCount();
    this.logLevel = other.getLogLevel();
    this.details = other.getDetails();
  }

  /**
   * Add audit to index.
   *
   * @param operationsService the operations service
   * @param
   */
  public static void addAudit(
      final ElasticOperationsService operationsService,
      String type,
      String process,
      String terminology,
      String details,
      String logLevel)
      throws Exception {
    operationsService.deleteQuery(
        "terminology:" + terminology + " AND logLevel:" + logLevel + " AND details:" + details,
        ElasticOperationsService.AUDIT_INDEX);
    Audit audit = new Audit(type, terminology, null, new Date(), process, details, logLevel);
    operationsService.index(audit, ElasticOperationsService.AUDIT_INDEX, Audit.class);
  }
}
