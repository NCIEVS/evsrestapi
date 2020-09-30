
package gov.nih.nci.evs.api.util;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * REST call utilities
 */
public class RESTUtils {

  /** The Constant log. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(RESTUtils.class);

  /** The username. */
  private String username;

  /** The password. */
  private String password;

  /** The read timeout. */
  private Duration readTimeout;

  /** The connect timeout. */
  private Duration connectTimeout;

  /** The builder. */
  private RestTemplateBuilder builder;

  /**
   * Instantiates an empty {@link RESTUtils}.
   */
  public RESTUtils() {
  }

  /**
   * Instantiates a {@link RESTUtils} from the specified parameters.
   *
   * @param username the username
   * @param password the password
   * @param readTimeout the read timeout
   * @param connectTimeout the connect timeout
   */
  public RESTUtils(String username, String password, long readTimeout, long connectTimeout) {
    this.username = username;
    this.password = password;
    this.readTimeout = Duration.ofSeconds(readTimeout);
    this.connectTimeout = Duration.ofSeconds(connectTimeout);
    builder = new RestTemplateBuilder().basicAuthentication(username, password)
        .setReadTimeout(this.readTimeout).setConnectTimeout(this.connectTimeout);
  }

  /**
   * Run SPARQL.
   *
   * @param query the query
   * @param restURL the rest URL
   * @return the string
   */
  public String runSPARQL(String query, String restURL) {

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
    restTemplate.getMessageConverters().add(0,
        new StringHttpMessageConverter(Charset.forName("UTF-8")));
    MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
    body.add("query", query);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(Arrays.asList(new MediaType("application", "sparql-results+json")));
    HttpEntity<?> entity = new HttpEntity<Object>(body, headers);
    String results = restTemplate.postForObject(restURL, entity, String.class);
    return results;
  }

  /**
   * Escape lucene special characters.
   *
   * @param before the before
   * @return the string
   */
  public static String escapeLuceneSpecialCharacters(String before) {
    if(null == before) {
      return "";
    }
    String patternString = "([+:!~*?/\\-/{}\\[\\]\\(\\)\\^\\\"])";
    Pattern pattern = Pattern.compile(patternString);
    Matcher matcher = pattern.matcher(before);
    StringBuffer buf = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(buf, before.substring(matcher.start(), matcher.start(1)) + "\\\\"
          + "\\\\" + matcher.group(1) + before.substring(matcher.end(1), matcher.end()));
    }
    String after = matcher.appendTail(buf).toString();
    return after;
  }
}
