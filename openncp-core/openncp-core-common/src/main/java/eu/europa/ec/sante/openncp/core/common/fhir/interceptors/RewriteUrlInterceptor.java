package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

@Interceptor
@Component
public class RewriteUrlInterceptor implements FhirCustomInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RewriteUrlInterceptor.class);

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public void rewriteAttachmentUrl(
            final RequestDetails requestDetails,
            final ServletRequestDetails servletRequestDetails,
            final IBaseResource baseResource,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) {

        if (baseResource instanceof Bundle) {
            final Bundle bundle = (Bundle) baseResource;
            if (bundle.getType() == Bundle.BundleType.SEARCHSET) {
                final String replaceUrl = buildReplaceUrl(servletRequestDetails);
                bundle.getEntry().stream()
                        .map(Bundle.BundleEntryComponent::getResource)
                        .filter(resource -> resource.getResourceType() == ResourceType.DocumentReference)
                        .map(resource -> (DocumentReference) resource)
                        .map(DocumentReference::getContent)
                        .flatMap(Collection::stream)
                        .map(DocumentReference.DocumentReferenceContentComponent::getAttachment)
                        .filter(attachment -> attachment.getContentType().equalsIgnoreCase(Constants.CT_FHIR_JSON_NEW))
                        .forEach(attachment -> {
                            rewriteUrl(attachment, replaceUrl, "Bundle");
                            rewriteUrl(attachment, replaceUrl, "DocumentReference");
                        });
            }
        }
    }

    private String buildReplaceUrl(final ServletRequestDetails servletRequestDetails) {
        final HttpServletRequest httpServletRequest = servletRequestDetails.getServletRequest();
        final URIBuilder uriBuilder = new URIBuilder()
                .setScheme(httpServletRequest.getScheme())
                .setHost(httpServletRequest.getHeader(HttpHeaders.HOST))
                .setPath(httpServletRequest.getContextPath() + servletRequestDetails.getFhirServerBase() + '/');
        return uriBuilder.toString();
    }

    private void rewriteUrl(final Attachment attachment, final String replaceUrl, final String resourceType) {
        attachment.setUrl(attachment.getUrl().replaceAll("^.*?(?=" + resourceType + ")", replaceUrl));
    }
}