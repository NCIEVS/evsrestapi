package gov.nih.nci.evs.api.configuration;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the actual low-level OpenSearch HTTP request sent by the REST client.
 *
 * <p>This interceptor runs after Spring Data OpenSearch has converted a {@code Query} into an HTTP
 * request, so it sees the request that can be replayed outside Java: method, URL, query-string
 * parameters, index path, and serialized JSON payload.
 */
final class EvsOpenSearchRequestLoggingInterceptor implements HttpRequestInterceptor {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(EvsOpenSearchRestTemplate.class);

  /** The maximum number of payload characters to write to the log. */
  private static final int MAX_LOGGED_PAYLOAD_LENGTH = 100_000;

  /** The OpenSearch bulk/msearch newline-delimited JSON media type. */
  private static final String NDJSON_MIME_TYPE = "application/x-ndjson";

  /* see superclass */
  @Override
  public void process(final HttpRequest request, final HttpContext context)
      throws HttpException, IOException {
    // This interceptor is invoked for every OpenSearch request. Keep the normal INFO path cheap.
    if (!logger.isDebugEnabled()) {
      return;
    }

    // Request URI is the path and query string the low-level REST client is about to send.
    final String requestUri = request.getRequestLine().getUri();
    final String requestPath = getRequestPath(requestUri);
    final String payload = getPayload(request);

    logger.debug(
        "opensearch request:"
            + "\n  method = {}"
            + "\n  url = {}"
            + "\n  parameters = {}"
            + "\n  index = {}"
            + "\n  payload =\n{}",
        request.getRequestLine().getMethod(),
        getRequestUrl(requestUri, context),
        getRequestParameters(requestUri),
        getRequestIndex(requestPath),
        payload);
  }

  /**
   * Get the request payload while replacing the consumed entity so the request can still be sent.
   *
   * <p>Reading an Apache HTTP entity consumes its stream. After logging, we must put an equivalent
   * entity back on the request; otherwise OpenSearch would receive an empty body.
   *
   * @param request the request
   * @return the payload text to log, or {@code <none>} when there is no body
   * @throws IOException if the entity cannot be read
   */
  private String getPayload(final HttpRequest request) throws IOException {
    if (!(request instanceof HttpEntityEnclosingRequest entityRequest)) {
      return "<none>";
    }

    final HttpEntity entity = entityRequest.getEntity();
    if (entity == null) {
      return "<none>";
    }

    final ContentType contentType = ContentType.get(entity);
    if (!isLoggableContentType(contentType)) {
      return "<payload not logged: content type " + contentType.getMimeType() + ">";
    }

    final Charset charset =
        contentType == null || contentType.getCharset() == null
            ? StandardCharsets.UTF_8
            : contentType.getCharset();

    // EntityUtils reads and closes the original entity content stream.
    final byte[] payloadBytes = EntityUtils.toByteArray(entity);
    final String payload = new String(payloadBytes, charset);

    // Replace the consumed entity with the full payload and headers before the request continues.
    final ByteArrayEntity replacement = new ByteArrayEntity(payloadBytes);
    if (entity.getContentType() != null) {
      replacement.setContentType(entity.getContentType());
    }
    replacement.setChunked(entity.isChunked());
    if (entity.getContentEncoding() != null) {
      replacement.setContentEncoding(entity.getContentEncoding());
    }
    entityRequest.setEntity(replacement);

    return truncatePayload(payload);
  }

  /**
   * Determine whether a request entity can be safely decoded as text for logging.
   *
   * @param contentType the entity content type
   * @return {@code true} if the payload should be decoded and logged
   */
  private boolean isLoggableContentType(final ContentType contentType) {
    if (contentType == null) {
      return true;
    }

    return ContentType.APPLICATION_JSON.getMimeType().equalsIgnoreCase(contentType.getMimeType())
        || NDJSON_MIME_TYPE.equalsIgnoreCase(contentType.getMimeType());
  }

  /**
   * Limit the payload text written to the log while leaving the actual request body unchanged.
   *
   * @param payload the decoded payload
   * @return the payload text to log
   */
  private String truncatePayload(final String payload) {
    if (payload.length() <= MAX_LOGGED_PAYLOAD_LENGTH) {
      return payload;
    }

    return payload.substring(0, MAX_LOGGED_PAYLOAD_LENGTH)
        + "\n<... payload truncated after "
        + MAX_LOGGED_PAYLOAD_LENGTH
        + " characters>";
  }

  /**
   * Get the full request URL.
   *
   * <p>The Apache request line usually contains only a relative URI like {@code /index/_search}, so
   * the target host from the HTTP context is used to make the logged URL directly replayable.
   *
   * @param requestUri the URI from the request line
   * @param context the request context
   * @return the full request URL when the target host is known, otherwise the request URI
   */
  private String getRequestUrl(final String requestUri, final HttpContext context) {
    if (requestUri.startsWith("http://") || requestUri.startsWith("https://")) {
      return requestUri;
    }

    final HttpHost targetHost = HttpCoreContext.adapt(context).getTargetHost();
    return targetHost == null ? requestUri : targetHost.toURI() + requestUri;
  }

  /**
   * Get the query-string parameters from the request URI.
   *
   * @param requestUri the URI from the request line
   * @return the raw query-string parameters, or {@code <none>} when absent
   */
  private String getRequestParameters(final String requestUri) {
    final int queryIndex = requestUri.indexOf('?');
    if (queryIndex < 0 || queryIndex == requestUri.length() - 1) {
      return "<none>";
    }
    return requestUri.substring(queryIndex + 1);
  }

  /**
   * Get the path portion from a relative or absolute request URI.
   *
   * @param requestUri the URI from the request line
   * @return the request path
   */
  private String getRequestPath(final String requestUri) {
    int pathIndex = 0;
    final int schemeIndex = requestUri.indexOf("://");
    if (schemeIndex >= 0) {
      pathIndex = requestUri.indexOf('/', schemeIndex + 3);
      if (pathIndex < 0) {
        return "/";
      }
    }

    final int queryIndex = requestUri.indexOf('?', pathIndex);
    return queryIndex < 0
        ? requestUri.substring(pathIndex)
        : requestUri.substring(pathIndex, queryIndex);
  }

  /**
   * Extract the first path segment as the index when the request path is index-scoped.
   *
   * <p>Search requests are typically {@code /index/_search}; cluster APIs such as {@code
   * /_cluster/health} do not have an index and are reported as {@code <none>}.
   *
   * @param requestPath the path portion of the request URI
   * @return the index path segment, or {@code <none>} when the request is not index-scoped
   */
  private String getRequestIndex(final String requestPath) {
    final String normalizedPath = requestPath.replaceFirst("^/+", "");
    if (normalizedPath.isBlank()) {
      return "<none>";
    }

    final String firstSegment = normalizedPath.split("/", 2)[0];
    return firstSegment.startsWith("_") ? "<none>" : firstSegment;
  }
}
