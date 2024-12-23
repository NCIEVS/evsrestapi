package gov.nih.nci.evs.api.util;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

/** Utilities for handling the "include" flag, and converting EVSConcept to Concept. */
@Component
public final class TerminologyUtils {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(TerminologyUtils.class);

  /** The stardog properties. */
  @Autowired StardogProperties stardogProperties;

  /** The application properties. */
  @Autowired ApplicationProperties applicationProperties;

  /** The license cache. */
  private static Map<String, String> licenseCache =
      new LinkedHashMap<String, String>(1000 * 4 / 3, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, String> eldest) {
          return size() > 1000;
        }
      };

  /**
   * Returns all terminologies.
   *
   * @param sparqlQueryManagerService the sparql query service
   * @return the terminologies
   * @throws Exception Signals that an exception has occurred.
   */
  public List<Terminology> getTerminologies(SparqlQueryManagerService sparqlQueryManagerService)
      throws Exception {
    return sparqlQueryManagerService.getTerminologies(stardogProperties.getDb());
  }

  /**
   * Returns terminologies loaded to elasticsearch.
   *
   * @param esQueryService elastic query service
   * @return the list of terminology objects
   * @throws Exception Signals that an exception has occurred.
   */
  public List<Terminology> getIndexedTerminologies(ElasticQueryService esQueryService)
      throws Exception {
    // get index metadata for terminologies completely loaded in es
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    if (CollectionUtils.isEmpty(iMetas)) return Collections.emptyList();

    return iMetas.stream().map(m -> m.getTerminology()).collect(Collectors.toList());
  }

  /**
   * Returns the stale terminologies.
   *
   * @param dbs the dbs
   * @param terminology the terminology
   * @return the stale terminologies
   * @throws Exception the exception
   */
  public List<IndexMetadata> getStaleStardogTerminologies(
      final List<String> dbs,
      final Terminology terminology,
      SparqlQueryManagerService sparqlQueryManagerService,
      ElasticQueryService esQueryService)
      throws Exception {
    // get index metadata for terminologies completely loaded in es
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    if (CollectionUtils.isEmpty(iMetas)) {
      return Collections.emptyList();
    }

    final Map<String, Terminology> stardogMap = new HashMap<>();

    // Collect terminologies that are in stardog
    for (final String db : dbs) {
      List<Terminology> terminologies = sparqlQueryManagerService.getTerminologies(db);
      terminologies.stream().forEach(t -> stardogMap.putIfAbsent(t.getTerminologyVersion(), t));
    }

    // Stale means matching current terminology, not loaded via RRF, and in NOT
    // in stardog
    return iMetas.stream()
        .filter(
            m ->
                m.getTerminology().getTerminology().equals(terminology.getTerminology())
                    && !m.getTerminology().getMetadata().getLoader().equals("rrf")
                    && !stardogMap.containsKey(m.getTerminologyVersion()))
        .collect(Collectors.toList());
  }

  /**
   * Returns the terminology.
   *
   * <p>Set {@literal true} for {@code indexedOnly} param to lookup in indexed terminologies.
   *
   * @param terminology the terminology
   * @param sparqlQueryManagerService the sparql query service
   * @return the terminology
   * @throws Exception the exception
   */
  public Terminology getTerminology(
      final String terminology, SparqlQueryManagerService sparqlQueryManagerService)
      throws Exception {
    List<Terminology> terminologies = getTerminologies(sparqlQueryManagerService);
    return findTerminology(terminology, terminologies);
  }

  /**
   * Get the indexed terminology
   *
   * @param terminology search terminology
   * @param esQueryService elastic query service
   * @return the Terminology
   * @throws Exception the exception
   */
  public Terminology getIndexedTerminology(
      final String terminology, ElasticQueryService esQueryService) throws Exception {
    List<Terminology> terminologies = getIndexedTerminologies(esQueryService);
    return findTerminology(terminology, terminologies);
  }

  /**
   * Helper method to search for the target terminology in the list of terminologies.
   *
   * @param terminology target terminology
   * @param terminologies list of terminologies to search through
   * @return the Terminology
   */
  private Terminology findTerminology(final String terminology, List<Terminology> terminologies) {
    // Find latest monthly match
    final Terminology latestMonthly =
        terminologies.stream()
            .filter(
                t ->
                    t.getTerminology().equals(terminology)
                        && "ncit".equals(terminology)
                        && "true".equals(t.getTags().get("monthly"))
                        && t.getLatest() != null
                        && t.getLatest())
            .findFirst()
            .orElse(null);
    if (latestMonthly != null) {
      return latestMonthly;
    }

    // Find terminologyVersion match
    final Terminology tv =
        terminologies.stream()
            .filter(t -> t.getTerminologyVersion().equals(terminology))
            .findFirst()
            .orElse(null);
    if (tv != null) {
      return tv;
    }

    // Find "latest" match
    final Terminology latest =
        terminologies.stream()
            .filter(
                t ->
                    t.getTerminology().equals(terminology)
                        && t.getLatest() != null
                        && t.getLatest())
            .findFirst()
            .orElse(null);
    if (latest != null) {
      return latest;
    }

    // Find the "first"
    final Terminology first =
        terminologies.stream()
            .filter(t -> t.getTerminology().equals(terminology))
            .findFirst()
            .orElse(null);
    if (first != null) {
      return first;
    }

    // IF we get this far, something is weird, show all terminologies
    terminologies.stream().forEach(t -> logger.info("  " + t.getTerminologyVersion() + " = " + t));
    throw new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Terminology not found = " + terminology);
  }

  /**
   * Returns the terminology name without the version.
   *
   * @param terminology the terminology
   * @return the terminology name
   * @throws Exception the exception
   */
  public String getTerminologyName(final String terminology) throws Exception {
    return terminology.replaceFirst("(?<!snomedct)_.*", "");
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
    if (terminology.getMetadata() != null
        && db != null
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
      Calendar cal = Calendar.getInstance();
      cal.setTime(d);
      // Count days of week; for NCI, this should be max Mondays in month
      int maxDayOfWeek = cal.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);

      String version = terminology.getVersion();
      char weekIndicator = version.charAt(version.length() - 1);
      switch (weekIndicator) {
        case 'e':
          monthly = true; // has to be monthly version
          break;
        case 'd': // monthly version, if month has only 4 days of week (for ex:
          // Monday) only
          if (maxDayOfWeek == 4) monthly = true;
          break;
        default: // case a,b,c
          break;
      }
    }
    if (monthly) {
      terminology.getTags().put("monthly", "true");
    }

    // Every version is also a weekly
    terminology.getTags().put("weekly", "true");
  }

  /**
   * Check license.
   *
   * @param terminology the terminology
   * @param license the license info
   * @throws Exception the exception
   */
  public void checkLicense(final Terminology terminology, final String license) throws Exception {

    // Nothing to do if there is no license requirement.
    if (terminology.getMetadata().getLicenseText() == null) {
      return;
    }

    final String licenseUrl =
        "https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/LICENSE.md";

    // Check the license key and fail
    if (license == null) {

      // Override mechanism to support disabling the license check.
      if (terminology.getMetadata().getLicenseCheck() == null
          || terminology.getMetadata().getLicenseCheck().equals("DISABLED")) {
        return;
      }

      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "API calls for terminology='"
              + terminology.getTerminology()
              + "' require an X-EVSRESTAPI-License-Key header, visit "
              + licenseUrl
              + " for more information.");
    }

    // Allow the UI license to bypass this.
    if (license.equals(applicationProperties.getUiLicense())) {
      return;
    }

    // If cached as success already, continue to allow
    if (licenseCache.containsKey(terminology.getTerminology() + license)) {
      if ("true".equals(licenseCache.get(terminology.getTerminology() + license))) {
        return;
      } else {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Invalid X-EVSRESTAPI-License-Key header for this terminology, visit "
                + licenseUrl
                + " for more information.");
      }
    }
    // Handle MDR Style - meddraId:meddraApiKey
    if (terminology.getTerminology().equals("mdr")) {

      // Verify license
      final String[] tokens = license.split(":");
      if (tokens.length != 2) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "API calls for terminology='"
                + terminology.getTerminology()
                + "' require an X-EVSRESTAPI-License-Key header with 2 parts"
                + " 'meddraID:meddraApiKey', visit "
                + licenseUrl
                + " for more information.");
      }
      final String id = tokens[0];
      final String apiKey = tokens[1];

      final String[] parts = terminology.getMetadata().getLicenseCheck().split(";");
      final String method = parts[0];
      final String uri = parts[1];
      final String contentType = parts[2];
      final Map<String, String> config = new HashMap<>();
      config.put("id", id);
      config.put("apiKey", apiKey);
      final String payload = new StringSubstitutor(config).replace(parts[3]);
      try {
        checkLicenseHttp(method, uri, contentType, payload, licenseUrl);
      } catch (Exception e) {
        licenseCache.put(terminology.getTerminology() + license, "false");
        throw e;
      }

      licenseCache.put(terminology.getTerminology() + license, "true");
    }
  }

  /**
   * Check license http.
   *
   * @param method the method
   * @param uri the uri
   * @param contentType the content type
   * @param payload the payload
   * @param licenseUrl the license url
   * @throws Exception the exception
   */
  public void checkLicenseHttp(
      final String method,
      final String uri,
      final String contentType,
      final String payload,
      final String licenseUrl)
      throws Exception {

    try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {

      HttpUriRequest request = null;

      if (method.equals("POST")) {
        // Create an instance of HttpPost
        final HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader("Content-type", contentType);
        httpPost.setEntity(new StringEntity(payload));
        request = httpPost;
      }
      // Handle GET when needed
      // else {}

      // Unexpected
      else {
        throw new Exception("Unsupported method = " + method);
      }

      // Execute the POST request
      try (final CloseableHttpResponse response = httpClient.execute(request)) {
        // Get the response entity
        final HttpEntity entity = response.getEntity();

        // Get the response status code
        final int statusCode = response.getStatusLine().getStatusCode();
        final String responseContent = EntityUtils.toString(entity);
        if (statusCode >= 300) {
          logger.error("uri = " + uri);
          logger.error("payload = " + payload);
          logger.error("Unexpected response = " + responseContent);
          throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "Invalid X-EVSRESTAPI-License-Key header for this terminology, visit "
                  + licenseUrl
                  + " for more information.");
        }
      }
    }
  }

  /** Clear cache. */
  public static void clearCache() {
    licenseCache.clear();
  }
}
