package eu.europa.ec.sante.openncp.core.client.dicom;

import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.core.common.dicom.DicomDispatchingService;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.interceptors.CorrelationIdInterceptor;
import eu.europa.ec.sante.openncp.core.common.fhir.interceptors.CountryCodeInterceptor;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;

@Service
public class NcpDicomDispatchingService implements DicomDispatchingService {
    private final DynamicDiscoveryService dynamicDiscoveryService;
    private final RestTemplate restTemplate;

    public NcpDicomDispatchingService(final DynamicDiscoveryService dynamicDiscoveryService, final RestTemplate restTemplate) throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, KeyManagementException {
        this.dynamicDiscoveryService = Validate.notNull(dynamicDiscoveryService, "dynamicDiscoveryService must not be null");
        this.restTemplate = Validate.notNull(restTemplate, "restTemplate must not be null");
    }

    @Override
    public byte[] dispatchDicomFile(final DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID) {
        //the dispatch method will use the same path params as the incoming request for the outgoing response, so we currently don't need the dicom UID variables
        return dispatch(dispatchContext, byte[].class);
    }


    @Override
    public String dispatchDicomMetadata(final DispatchContext dispatchContext, final String studyUID, final String seriesUID) {
        //the dispatch method will use the same path params as the incoming request for the outgoing response, so we currently don't need the dicom UID variables
        return dispatch(dispatchContext, String.class);
    }

    @Override
    public byte[] dispatchDicomRenderedImage(final DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID, final String frameNumber) {
        //the dispatch method will use the same path params as the incoming request for the outgoing response, so we currently don't need the dicom UID variables
        return dispatch(dispatchContext, byte[].class);
    }

    @Override
    public InputStream dispatchDicomPixelData(final DispatchContext dispatchContext, final String studyId, final String seriesId, final String instanceId, final String frameNumber) {
        //the dispatch method will use the same path params as the incoming request for the outgoing response, so we currently don't need the dicom UID variables
        return dispatch(dispatchContext, InputStream.class);
    }

    @Override
    public InputStream dispatchDicomBulkData(final DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID, final String bulkDataID) {
        //the dispatch method will use the same path params as the incoming request for the outgoing response, so we currently don't need the dicom UID variables
        return dispatch(dispatchContext, InputStream.class);
    }

    private HttpHeaders buildHeaders(final DispatchContext dispatchContext) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationIdInterceptor.X_CORRELATION_ID_HEADER_KEY, LogContext.getCorrelationId());
        headers.set(CountryCodeInterceptor.COUNTRY_CODE_HEADER_KEY, dispatchContext.getCountryCode().value());

        Optional.ofNullable(dispatchContext.getServletRequest().getContentType()).ifPresent(contentType -> headers.set("Content-Type", contentType));
        Optional.ofNullable(dispatchContext.getServletRequest().getHeader("Accept")).ifPresent(accept -> headers.add("Accept", accept));
        dispatchContext.getJwtTokenFromRequest().ifPresent(jwtToken -> headers.set("Authorization", jwtToken.getAuthorizationHeaderValue()));

        return headers;
    }

    private URI buildUri(final DispatchContext dispatchContext) {
        final String endpointUrl = dynamicDiscoveryService.getEndpointUrl(dispatchContext.getCountryCode().value(), RegisteredService.FHIR_SERVICE);

        final HttpServletRequest servletRequest = dispatchContext.getServletRequest();
        final String requestUri = servletRequest.getRequestURI();

        final int dicomIndex = requestUri.indexOf("/dicom");
        if (dicomIndex == -1) {
            throw new IllegalArgumentException("URL does not contain /dicom path");
        }
        final String dicomPath = requestUri.substring(dicomIndex);

        // FIXME we replace the /fhir/ call because the endpointUrl (from the dynamicDiscoveryService) returns the fhir service url
        return UriComponentsBuilder.fromHttpUrl(endpointUrl.replaceAll("/fhir/", ""))
                .path(dicomPath)
                .query(servletRequest.getQueryString())
                .build(false) // false to not encode URI twice
                .toUri();
    }

    private <T> T dispatch(final DispatchContext dispatchContext, final Class<T> responseClass) {
        final HttpHeaders headers = buildHeaders(dispatchContext);
        final URI uri = buildUri(dispatchContext);

        final ResponseEntity<T> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseClass
        );

        return handleAndReturnResponse(response);
    }

    private <T> T handleAndReturnResponse(final ResponseEntity<T> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            //TODO proper error handling
            throw new RuntimeException(response.getStatusCode() + " " + response.getBody());
        }
    }
}
