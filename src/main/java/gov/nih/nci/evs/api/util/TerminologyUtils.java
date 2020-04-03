
package gov.nih.nci.evs.api.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
public final class TerminologyUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(TerminologyUtils.class);

  /**
   * Instantiates an empty {@link TerminologyUtils}.
   */
  private TerminologyUtils() {
    // n/a
  }

  /**
   * Returns the terminologies.
   *
   * @param sparqlQueryManagerService the sparql query manager service
   *
   * @return the terminologies
   * @throws Exception Signals that an exception has occurred.
   */
  public static List<Terminology> getTerminologies(final SparqlQueryManagerService sparqlQueryManagerService) throws Exception {
    
    List<EvsVersionInfo> evsVersionInfoList = sparqlQueryManagerService.getEvsVersionInfoList();
    
    if (CollectionUtils.isEmpty(evsVersionInfoList)) return Collections.<Terminology>emptyList();
    
    final DateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy");
    final List<Terminology> results = new ArrayList<>();
    
    Collections.sort(evsVersionInfoList, new Comparator<EvsVersionInfo>() {
      @Override
      public int compare(EvsVersionInfo o1, EvsVersionInfo o2) {
        return -1 * o1.getVersion().compareTo(o2.getVersion());
      }});
    
    for(int i=0; i<evsVersionInfoList.size(); i++) {
      final EvsVersionInfo versionInfo = evsVersionInfoList.get(i);
      final Terminology term = getTerminologyForVersionInfo(versionInfo, fmt);
      
      logger.debug("Adding terminology - " + term.getTerminologyVersion());
      
      //set latest tag for the most recent version
      term.setLatest(i==0);
      
      logger.debug("  Latest tag - " + term.getLatest());
      
      //temporary code -- enable date logic in getTerminologyForVersionInfo
      if (i==0) {
        term.getTags().put("monthly", "true");
        logger.debug("  Monthly tag - true");
      } else {
        term.getTags().put("weekly", "true");
        logger.debug("  Weekly tag - true");
      }
      
      results.add(term);
    }
    
    return results;
  }

  private static Terminology getTerminologyForVersionInfo(final EvsVersionInfo versionInfo, final DateFormat fmt) throws ParseException {
    final Terminology term = new Terminology(versionInfo);
    final Date d = fmt.parse(versionInfo.getDate());
    Calendar cal = GregorianCalendar.getInstance();
    cal.setTime(d);

    //Count days of week; for NCI, this should be max Mondays in month
    int maxDayOfWeek = cal.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);

    String version = versionInfo.getVersion();
    char weekIndicator = version.charAt(version.length() - 1);
    
    boolean monthly = false;
    
    switch (weekIndicator) {
      case 'e':
        monthly = true;//has to be monthly version
        break;
      case 'd'://monthly version, if month has only 4 days of week (for ex: Monday) only
        if (maxDayOfWeek == 4) monthly = true;
        break;
      default://case a,b,c
        break;
    }
    
    if (monthly) term.getTags().put("monthly", "true");
    else term.getTags().put("weekly", "true");
    
    return term;
  }
  
  /**
   * Returns the terminology.
   *
   * @param sparqlQueryManagerService the sparql query manager service
   * @param terminology the terminology
   * @return the terminology
   * @throws Exception the exception
   */
  public static Terminology getTerminology(
    final SparqlQueryManagerService sparqlQueryManagerService, final String terminology)
    throws Exception {
    for (final Terminology t : getTerminologies(sparqlQueryManagerService)) {
      if (t.getTerminology().equals(terminology) && t.getLatest() != null && t.getLatest()) {
        return t;
      } else if (t.getTerminologyVersion().equals(terminology)) {
        return t;
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, terminology + " not found");
  }

  /**
   * Returns the latest terminology.
   *
   * @param sparqlQueryManagerService the sparql query manager service
   * @return the terminology
   * @throws Exception the exception
   */
  public static Terminology getLatestTerminology(
    final SparqlQueryManagerService sparqlQueryManagerService)
    throws Exception {
    
    List<Terminology> terminologies = getTerminologies(sparqlQueryManagerService);
    if (CollectionUtils.isEmpty(terminologies))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No terminology found!");
    
    Optional<Terminology> latest = terminologies.stream().filter(t -> t.getLatest()).findFirst();
    if (!latest.isPresent()) 
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No terminology found with latest flag!");
    
    return latest.get();
  }
  
  /**
   * As set.
   *
   * @param <T> the
   * @param values the values
   * @return the sets the
   */
  public static <T> Set<T> asSet(@SuppressWarnings("unchecked") final T... values) {
    final Set<T> set = new HashSet<>(values.length);
    for (final T value : values) {
      if (value != null) {
        set.add(value);
      }
    }
    return set;
  }

}
