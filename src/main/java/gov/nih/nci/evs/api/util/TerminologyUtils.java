
package gov.nih.nci.evs.api.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.es.IndexMetadata;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
@Component
public final class TerminologyUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(TerminologyUtils.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /** The es query service. */
  /* The elasticsearch query service */
  @Autowired
  ElasticQueryService esQueryService;

  /** The stardog properties. */
  @Autowired
  StardogProperties stardogProperties;

  /**
   * Returns all terminologies.
   * 
   * @param indexed use {@literal true} to get indexed terminologies as opposed
   *          to terminologies from stardog
   * @return the terminologies
   * @throws Exception Signals that an exception has occurred.
   */
  public List<Terminology> getTerminologies(boolean indexed) throws Exception {
    if (indexed) {
      return getIndexedTerminologies();
    }
    return sparqlQueryManagerService.getTerminologies(stardogProperties.getDb());
  }

  /**
   * Returns terminologies loaded to elasticsearch.
   * 
   * @return the list of terminology objects
   * @throws Exception Signals that an exception has occurred.
   */
  private List<Terminology> getIndexedTerminologies() throws Exception {
    // get index metadata for terminologies completely loaded in es
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    if (CollectionUtils.isEmpty(iMetas))
      return Collections.emptyList();

    return iMetas.stream().map(m -> m.getTerminology()).collect(Collectors.toList());
  }

  /**
   * Returns the stale terminologies.
   *
   * @param dbs the dbs
   * @return the stale terminologies
   * @throws Exception the exception
   */
  public List<IndexMetadata> getStaleTerminologies(final List<String> dbs) throws Exception {
    // get index metadata for terminologies completely loaded in es
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    if (CollectionUtils.isEmpty(iMetas)) {
      return Collections.emptyList();
    }

    final Map<String, Terminology> termMap = new HashMap<>();
    for (final String db : dbs) {
      // get all terminologies and organize in a map by terminologyVersion as
      // key
      List<Terminology> terminologies = sparqlQueryManagerService.getTerminologies(db);
      terminologies.stream().forEach(t -> termMap.putIfAbsent(t.getTerminologyVersion(), t));
    }

    // collect stale terminologies loaded in es
    return iMetas.stream().filter(m -> !termMap.containsKey(m.getTerminologyVersion()))
        .collect(Collectors.toList());
  }

  /**
   * Returns the terminology.
   * 
   * Set {@literal true} for {@code indexedOnly} param to lookup in indexed
   * terminologies.
   *
   * @param terminology the terminology
   * @param indexed use {@literal true} to lookup in indexed terminologies as
   *          opposed to stardog
   * @return the terminology
   * @throws Exception the exception
   */
  public Terminology getTerminology(final String terminology, boolean indexed) throws Exception {
    List<Terminology> terminologies = getTerminologies(indexed);
    for (final Terminology t : terminologies) {

      // For "ncit", choose the "latest monthly" before latest weekly
      if (t.getTerminology().equals(terminology) && "ncit".equals(terminology)
          && "true".equals(t.getTags().get("monthly")) && t.getLatest() != null && t.getLatest()) {
        return t;
      }
      // Otherwise choose the latest (in general) - this works for both the ncim
      // case and the ncit case where only a weekly is loaded.
      else if (t.getTerminology().equals(terminology) && t.getLatest() != null && t.getLatest()) {
        return t;
      }
      // Otherwise choose the matching terminology+version
      else if (t.getTerminologyVersion().equals(terminology)) {
        return t;
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, terminology + " not found");
  }

  /**
   * As set.
   *
   * @param <T> the
   * @param values the values
   * @return the sets the
   */
  public static <T> Set<T> asSet(@SuppressWarnings("unchecked")
  final T... values) {
    final Set<T> set = new HashSet<>(values.length);
    for (final T value : values) {
      if (value != null) {
        set.add(value);
      }
    }
    return set;
  }

  /**
   * Construct name for terminology using comment and version.
   *
   * @param comment the terminology comment
   * @param version the terminology version
   * @return the string
   */
  public static String constructName(String comment, String version) {
    return comment.substring(0, comment.indexOf(",")) + " " + version;
  }

  /**
   * Sets monthly/weekly tags based on date on the given terminology object.
   *
   * @param terminology the terminology
   * @param db the db
   * @throws ParseException the parse exception
   */
  public void setTags(final Terminology terminology, final String db) throws ParseException {

    // Compute "monthly"
    final DateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy");
    boolean monthly = false;

    // If the stardogProperties "db" matches the terminology metadata
    // "monthlyDb"
    // then continue, we're good.
    if (terminology.getMetadata() != null && db != null
        && db.equals(terminology.getMetadata().getMonthlyDb())) {
      logger.info("  stardog monthly db found = " + db);
      monthly = true;
    }

    // If the ncit.json "monthlyDb" isn't set, then calculate
    // NOTE: this wont' handle exceptions like 20210531 being
    // the 5th Monday of May in 2021 but also a holiday
    else if (terminology.getMetadata() == null
        || terminology.getMetadata().getMonthlyDb() == null) {
      final Date d = fmt.parse(terminology.getDate());
      Calendar cal = GregorianCalendar.getInstance();
      cal.setTime(d);
      // Count days of week; for NCI, this should be max Mondays in month
      int maxDayOfWeek = cal.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);

      String version = terminology.getVersion();
      char weekIndicator = version.charAt(version.length() - 1);
      switch (weekIndicator) {
        case 'e':
          monthly = true;// has to be monthly version
          break;
        case 'd':// monthly version, if month has only 4 days of week (for ex:
                 // Monday) only
          if (maxDayOfWeek == 4)
            monthly = true;
          break;
        default:// case a,b,c
          break;
      }
    }
    if (monthly) {
      terminology.getTags().put("monthly", "true");
    }

    // Every version is also a weekly
    terminology.getTags().put("weekly", "true");
  }

}
