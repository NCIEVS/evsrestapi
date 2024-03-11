
package gov.nih.nci.evs.api.util;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * REST call utilities.
 */
public class RESTUtils {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(RESTUtils.class);

  /** The username. */
  private String username;

  /** The password. */
  private String password;

  /** The read timeout. */
  // private Duration readTimeout;

  /** The connect timeout. */
  // private Duration connectTimeout;

  /**
   * Instantiates an empty {@link RESTUtils}.
   */
  public RESTUtils() {
    // n/a
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
    // this.readTimeout = Duration.ofSeconds(readTimeout);
    // this.connectTimeout = Duration.ofSeconds(connectTimeout);
    // builder = new RestTemplateBuilder().basicAuthentication(username,
    // password)
    // .setReadTimeout(this.readTimeout).setConnectTimeout(this.connectTimeout);
  }

  /**
   * Run direct SPARQL queries with timeout.
   *
   * @param query the query
   * @param restURL the rest URL
   * @return the string
   */
  public String runSPARQL(String query, String restURL, Integer sparqlTimeout) throws Exception {

    try {
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
      ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
      // create the task for the executor
      Callable<String> task = () -> {
        return restTemplate.postForObject(restURL, entity, String.class);
      };
      try {
        // invoke postForObject with executor
        String result = executor.invokeAny(Arrays.asList(task), sparqlTimeout, TimeUnit.SECONDS);

        return result;

      } catch (TimeoutException e) {
        // Handle timeout exception
        throw new TimeoutException("SPARQL query timed out after " + sparqlTimeout + " second"
            + (sparqlTimeout > 1 ? "s" : "")
            + ". Consider changing your query to return fewer results.");
      } catch (Exception e) {
        // Handle other exceptions
        throw new Exception(
            "SPARQL query failed. Consider verifying your query syntax: \n" + e.getMessage());
      } finally {
        // Shutdown the executor
        executor.shutdown();
      }
    } catch (Exception e) {
      log.error("Unexpected error running query = \n" + query);
      throw e;
    }

  }

  /**
   * Run SPARQL.
   *
   * @param query the query
   * @param restURL the rest URL
   * @return the string
   */
  public String runSPARQL(String query, String restURL) {

    try {
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
    } catch (Exception e) {
      log.error("Unexpected error running query = \n" + query);
      throw e;
    }

  }

}
