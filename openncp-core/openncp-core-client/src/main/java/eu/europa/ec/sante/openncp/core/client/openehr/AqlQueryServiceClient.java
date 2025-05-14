package eu.europa.ec.sante.openncp.core.client.openehr;

import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.core.common.HttpsClientConfiguration;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.AdhocQueryExecute;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.ResultSet;
import eu.europa.ec.sante.openncp.core.common.openehr.service.AqlQueryService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.WebRequest;

@Service
public class AqlQueryServiceClient implements AqlQueryService {

    private static final String COUNTRY_CODE_HEADER = "CountryCode";

    private final DynamicDiscoveryService dynamicDiscoveryService;

    public AqlQueryServiceClient(DynamicDiscoveryService dynamicDiscoveryService) {
        this.dynamicDiscoveryService = dynamicDiscoveryService;
    }

//    FIXME: This method does not work as Constants.SC_KEYSTORE_PATH is not defined yet
//    /**
//     * Initializes the REST client.
//     */
//    @PostConstruct
//    public void initialize() {
//        restTemplate = new RestTemplateBuilder()
//                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(createHttpClient()))
//                .build();
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet executeAdhocQuery(AdhocQueryExecute queryExecute, WebRequest request) {
        String url = getTargetUrl(request);
        HttpHeaders headers = new HttpHeaders();
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            headers.add(HttpHeaders.AUTHORIZATION, request.getHeader(HttpHeaders.AUTHORIZATION));
        }
        HttpEntity<AdhocQueryExecute> entity = new HttpEntity<>(queryExecute, headers);

        ResponseEntity<ResultSet> response = getRestTemplate().exchange(url, HttpMethod.POST, entity, ResultSet.class);
        return response.getBody();
    }

    private RestTemplate getRestTemplate() {
        return new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(createHttpClient()))
                .build();
    }

    /**
     * Creates an HTTP client with SSL support.
     *
     * @return the HTTP client
     */
    private CloseableHttpClient createHttpClient() {
        try {
            return HttpClients.custom()
                    .setSSLSocketFactory(HttpsClientConfiguration.buildSSLConnectionSocketFactory())
                    .setUserAgent("OpenNCP HTTP client")
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize an HTTP client for QueryServiceRestClient", ex);
        }
    }

    /**
     * Retrieves the target URL for the openEHR Query service.
     *
     * @param request the web request
     * @return the target URL
     */
    private String getTargetUrl(WebRequest request) {
        String countryCode = request.getHeader(COUNTRY_CODE_HEADER);
        if (countryCode == null) {
            throw new IllegalArgumentException("Country code is missing from the request headers");
        }
        return dynamicDiscoveryService.getEndpointUrl(countryCode, RegisteredService.OPENEHR_QUERY_SERVICE);
    }
}
