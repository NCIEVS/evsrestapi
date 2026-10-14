package gov.nih.nci.evs.api.configuration;

import java.io.IOException;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logs low-level OpenSearch requests sent by the HTTP Components 5 client. */
final class EvsOpenSearchRequestLoggingInterceptor implements HttpRequestInterceptor {

  /** Share the template logger so existing OpenSearch logging configuration still applies. */
  private static final Logger logger = LoggerFactory.getLogger(EvsOpenSearchRestTemplate.class);

  /* see superclass */
  @Override
  public void process(
      final HttpRequest request, final EntityDetails entity, final HttpContext context)
      throws HttpException, IOException {
    if (!logger.isDebugEnabled()) {
      return;
    }

    final String requestUri = request.getRequestUri();
    logger.debug(
        "opensearch request = {} {} index={} contentType={} payload={}",
        request.getMethod(),
        getRequestUrl(request),
        getRequestIndex(getRequestPath(requestUri)),
        entity == null ? "<none>" : entity.getContentType(),
        "<unavailable from the HTTP Components 5 async interceptor>");
  }

  /**
   * Get the full URL when the request includes a scheme and authority.
   *
   * @param request the request
   * @return the full URL, or the original request URI
   */
  private String getRequestUrl(final HttpRequest request) {
    final String requestUri = request.getRequestUri();
    if (requestUri.startsWith("http://") || requestUri.startsWith("https://")) {
      return requestUri;
    }
    if (request.getScheme() != null && request.getAuthority() != null) {
      return request.getScheme() + "://" + request.getAuthority() + requestUri;
    }
    return requestUri;
  }

  /**
   * Extract the path without decoding escaped characters or parsing it as a strict URI.
   *
   * @param requestUri the relative or absolute request URI
   * @return the raw request path
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
   * Extract the index name, excluding cluster-level endpoints such as {@code /_cluster/health}.
   *
   * @param requestPath the request path
   * @return the index name, or {@code <none>}
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
