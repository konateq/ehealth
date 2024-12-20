package eu.europa.ec.sante.openncp.core.server.nc.mock.dicom;

import eu.europa.ec.sante.openncp.core.server.nc.mock.util.MimeProcessor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rest client to more easily interface with our docker orthanc dicom server (https://orthanc.uclouvain.be/book/users/rest.html)
 */
@Component
public class OrthancRestClient {
    private static final String DICOM_SERVER_URL = "http://openncp-dicom-server:8042/dicom-web";
    private final RestTemplate restTemplate;

    public OrthancRestClient(@Qualifier("orthancRestTemplate") final RestTemplate restTemplate) {
        this.restTemplate = Validate.notNull(restTemplate, "orthancRestTemplate must not be null");
    }

    public byte[] downloadDicomFile(final String studyUid, final String seriesUid, final String instanceUid) {
        final HttpHeaders headers = new HttpHeaders();
        //headers.set(HttpHeaders.CONTENT_TYPE, "application/dicom");

        final List<String> pathSegments = getDefaultPathSegments(seriesUid, instanceUid);
        final Map<String, String> uriVariables = getDefaultUriVariables(studyUid, seriesUid, instanceUid);

        final ResponseEntity<byte[]> response = getResponse(headers, pathSegments, uriVariables, byte[].class);

        final HttpHeaders responseHeaders = response.getHeaders();
        final String contentDisposition = responseHeaders.getFirst(HttpHeaders.CONTENT_DISPOSITION);

        // Get the response body
        return MimeProcessor.removeMimeHeadersAndFooters(response.getBody());
    }

    public String downloadDicomMetadata(final String studyUid, final String seriesUid, final String instanceUid) {
        final HttpHeaders headers = new HttpHeaders();
        //headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        final List<String> pathSegments = getDefaultPathSegments(seriesUid, instanceUid);
        pathSegments.add("/metadata");
        final Map<String, String> uriVariables = getDefaultUriVariables(studyUid, seriesUid, instanceUid);

        final ResponseEntity<String> response = getResponse(headers, pathSegments, uriVariables, String.class);
        return response.getBody();
    }


    public byte[] downloadDicomRenderedImage(final String studyUid, final String seriesUid, final String instanceUid, final String frameNumber) {
        final HttpHeaders headers = new HttpHeaders();
        //headers.set(HttpHeaders.ACCEPT, MediaType.IMAGE_JPEG_VALUE);

        final List<String> pathSegments = getDefaultPathSegments(seriesUid, instanceUid);
        pathSegments.add("/frames/{frameNumber}");
        final Map<String, String> uriVariables = getDefaultUriVariables(studyUid, seriesUid, instanceUid);
        uriVariables.put("frameNumber", frameNumber);

        final ResponseEntity<byte[]> response = getResponse(headers, pathSegments, uriVariables, byte[].class);
        return response.getBody();
    }

    private <T> ResponseEntity<T> getResponse(final HttpHeaders headers, final List<String> pathSegments, final Map<String, String> uriVariables, final Class<T> responseType) {


        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(DICOM_SERVER_URL)
                .pathSegment(pathSegments.toArray(String[]::new));

        final ResponseEntity<T> response = restTemplate.exchange(
                uriComponentsBuilder.build(uriVariables),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType
        );
        return response;
    }

    private List<String> getDefaultPathSegments(final String seriesUid, final String instanceUid) {
        final List<String> pathSegments = new ArrayList<>();
        pathSegments.add("/studies/{studyUid}");
        if (StringUtils.isNotBlank(seriesUid)) {
            pathSegments.add("/series/{seriesUid}");
        }
        if (StringUtils.isNotBlank(instanceUid)) {
            pathSegments.add("/instances/{instanceUid}");
        }

        return pathSegments;
    }

    private Map<String, String> getDefaultUriVariables(final String studyUid, final String seriesUid, final String instanceUid) {
        Validate.notNull(studyUid, "study uid must not be null");
        final Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("studyUid", studyUid);
        if (StringUtils.isNotBlank(seriesUid)) {
            uriVariables.put("seriesUid", seriesUid);
        }
        if (StringUtils.isNotBlank(instanceUid)) {
            uriVariables.put("instanceUid", instanceUid);
        }
        return uriVariables;
    }
}
