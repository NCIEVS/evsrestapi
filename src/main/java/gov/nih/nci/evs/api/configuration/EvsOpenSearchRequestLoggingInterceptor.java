package gov.nih.nci.evs.api.configuration;

import java.io.IOException;
import java.net.URI;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logs low-level OpenSearch requests sent by the HTTP Components 5 client. */
final class EvsOpenSearchRequestLoggingInterceptor implements HttpRequestInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(EvsOpenSearchRestTemplate.class);

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

  private String getRequestPath(final String requestUri) {
    final String path = URI.create(requestUri).getPath();
    return path == null ? requestUri : path;
  }

  private String getRequestIndex(final String requestPath) {
    final String[] pathParts = requestPath.split("/");
    return pathParts.length > 1 && !pathParts[1].isBlank() ? pathParts[1] : "<none>";
  }
}
