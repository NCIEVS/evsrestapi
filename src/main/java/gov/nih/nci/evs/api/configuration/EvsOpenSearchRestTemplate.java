package gov.nih.nci.evs.api.configuration;

import java.util.concurrent.ThreadLocalRandom;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.AbstractElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Custom {@link OpenSearchRestTemplate} with bounded retries and query logging.
 *
 * <p>As of spring-data-elasticsearch version 3.2.0, only one query method logs query and honors
 * spring data logging property. This custom implementation also retries transient HTTP 429 and 502
 * responses from the shared OpenSearch client.
 *
 * @author Arun
 */
public class EvsOpenSearchRestTemplate extends OpenSearchRestTemplate {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(EvsOpenSearchRestTemplate.class);

  private static final int MAX_ATTEMPTS = 5;
  private static final long INITIAL_RETRY_DELAY_MS = 500;

  @FunctionalInterface
  interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  private final RestHighLevelClient client;
  private final Sleeper sleeper;

  /**
   * Instantiates a {@link EvsOpenSearchRestTemplate} from the specified parameters.
   *
   * @param client the client
   */
  public EvsOpenSearchRestTemplate(final RestHighLevelClient client) {
    this(client, Thread::sleep);
  }

  EvsOpenSearchRestTemplate(RestHighLevelClient client, Sleeper sleeper) {
    super(client);
    this.client = client;
    this.sleeper = sleeper;
  }

  private EvsOpenSearchRestTemplate(
      RestHighLevelClient client, ElasticsearchConverter converter, Sleeper sleeper) {
    super(client, converter);
    this.client = client;
    this.sleeper = sleeper;
  }

  @Override
  protected AbstractElasticsearchTemplate doCopy() {
    EvsOpenSearchRestTemplate copy =
        new EvsOpenSearchRestTemplate(client, elasticsearchConverter, sleeper);
    copy.requestFactory = requestFactory;
    return copy;
  }

  @Override
  public <T> T execute(ClientCallback<T> callback) {
    for (int attempt = 1; ; attempt++) {
      try {
        return super.execute(callback);
      } catch (RuntimeException error) {
        int status = retryableStatus(error);
        if (status == 0 || attempt >= MAX_ATTEMPTS) {
          throw error;
        }

        long delay = INITIAL_RETRY_DELAY_MS << (attempt - 1);
        delay += ThreadLocalRandom.current().nextLong(delay / 4 + 1);
        logger.warn(
            "OpenSearch {} returned HTTP {}; retrying attempt {} of {} in {} ms",
            requestTarget(error),
            status,
            attempt + 1,
            MAX_ATTEMPTS,
            delay);
        try {
          sleeper.sleep(delay);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(
              "Interrupted while retrying OpenSearch request", interrupted);
        }
      }
    }
  }

  private static int retryableStatus(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      int status = 0;
      if (cause instanceof OpenSearchStatusException statusException) {
        status = statusException.status().getStatus();
      } else if (cause instanceof ResponseException responseException
          && responseException.getResponse() != null) {
        status = responseException.getResponse().getStatusLine().getStatusCode();
      }
      if (status == 429 || status == 502) {
        return status;
      }
    }
    return 0;
  }

  private static String requestTarget(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof ResponseException responseException
          && responseException.getResponse() != null
          && responseException.getResponse().getRequestLine() != null) {
        var request = responseException.getResponse().getRequestLine();
        String uri = request.getUri();
        int queryStart = uri.indexOf('?');
        return request.getMethod() + " " + (queryStart < 0 ? uri : uri.substring(0, queryStart));
      }
    }
    return "request";
  }

  /* see superclass */
  @Override
  public <T> SearchHits<T> search(
      final Query query, final Class<T> clazz, final IndexCoordinates index) {
    // Keep the original Spring Data query log for NativeSearchQuery callers. The HTTP interceptor
    // logs the full replayable OpenSearch request after this query has been serialized.
    if (logger.isDebugEnabled()
        && query instanceof NativeSearchQuery nativeSearchQuery
        && nativeSearchQuery.getQuery() != null) {
      logger.debug("  opensearch query = \n{}", nativeSearchQuery.getQuery());
    }

    return super.search(query, clazz, index);
  }
}
