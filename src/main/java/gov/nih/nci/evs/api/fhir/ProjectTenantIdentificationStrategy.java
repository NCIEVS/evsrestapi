package gov.nih.nci.evs.api.fhir;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.i18n.HapiLocalizer;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.tenant.ITenantIdentificationStrategy;
import ca.uhn.fhir.util.UrlPathTokenizer;

/**
 * Multi tenant handler that includes project id.
 */
public class ProjectTenantIdentificationStrategy implements ITenantIdentificationStrategy {

    @SuppressWarnings("unused")
    private static Logger logger =
            LoggerFactory.getLogger(ProjectTenantIdentificationStrategy.class);

    /* see superclass */
    @Override
    public void extractTenant(final UrlPathTokenizer theUrlPathTokenizer,
        final RequestDetails theRequestDetails) {
        String tenantId = null;
        final boolean isSystemRequest = (theRequestDetails instanceof SystemRequestDetails);

        // If we were given no partition for a system request, use DEFAULT:
        if (!theUrlPathTokenizer.hasMoreTokens()) {
            if (isSystemRequest) {
                tenantId = "sandbox";
                theRequestDetails.setTenantId(tenantId);
            }
        }

        // We were given at least one URL token:
        else {

            // peek() won't consume this token:
            tenantId = defaultIfBlank(theUrlPathTokenizer.peek(), null);
            // If it's "metadata" or starts with "$", use DEFAULT partition and don't consume this
            // token:
            if (tenantId != null && (tenantId.equals("metadata") || tenantId.equals("api-docs")
                    || tenantId.equals("swagger-ui") || tenantId.startsWith("$"))) {
                tenantId = "DEFAULT";
                theRequestDetails.setTenantId(tenantId);
                // logger.debug(" no tenant ID found for metadata or system request; using
                // DEFAULT.");
            }

            // It isn't metadata or $, so assume that this first token is the partition name and
            // consume it:
            else {
                tenantId =
                        defaultIfBlank(theUrlPathTokenizer.nextTokenUnescapedAndSanitized(), null);
                if (tenantId != null) {
                    // Burn the next "/fhir" token
                    theUrlPathTokenizer.nextTokenUnescapedAndSanitized();
                    // Set the tenant id
                    theRequestDetails.setTenantId(tenantId);
                    // logger.debug(" found tenant ID {} in request string", tenantId);
                }
            }
        }

        // If we get to this point without a tenant, it's an invalid request:
        if (tenantId == null) {
            final HapiLocalizer localizer =
                    theRequestDetails.getServer().getFhirContext().getLocalizer();
            throw new InvalidRequestException(Msg.code(307)
                    + localizer.getMessage(RestfulServer.class, "rootRequest.multitenant"));
        }
    }

    /* see superclass */
    @Override
    public String massageServerBaseUrl(final String fhirServerBase,
        final RequestDetails requestDetails) {
        // This is how we rewrite "link" and "resource" URLs to match tenant info.
        if (requestDetails.getTenantId() != null) {
            final String url = fhirServerBase + "/" + requestDetails.getTenantId();
            // logger.info(" massage server base url = " + url);
            return url;
        }
        return fhirServerBase;
    }
}
