package gov.nih.nci.evs.api.configuration;

import java.util.regex.Pattern;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

/** Translates the cluster-manager timeout parameter for temporary OpenSearch 1.x deployments. */
final class OpenSearchLegacyCompatibilityInterceptor implements HttpRequestInterceptor {

  /** Match only query parameter names, leaving paths, values, and encoding unchanged. */
  private static final Pattern CLUSTER_MANAGER_TIMEOUT =
      Pattern.compile("(?<=[?&])cluster_manager_timeout=");

  /* see superclass */
  @Override
  public void process(
      final HttpRequest request, final EntityDetails entity, final HttpContext context) {
    final String path = request.getPath();
    if (path != null && path.contains("cluster_manager_timeout=")) {
      request.setPath(CLUSTER_MANAGER_TIMEOUT.matcher(path).replaceAll("master_timeout="));
    }
  }
}
