package eu.europa.ec.sante.openncp.core.server.nc.mock.openehr;

import eu.europa.ec.sante.openncp.core.common.openehr.domain.AdhocQueryExecute;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.ResultSet;
import eu.europa.ec.sante.openncp.core.common.openehr.service.AqlQueryService;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.WebRequest;

import javax.annotation.PostConstruct;

/**
 * {@link AqlQueryService} implementation that proxies the requests to an external openEHR AQL endpoint.
 *
 * @author Renaud Subiger
 * @since 9.0
 */
@Service
public class AqlQueryServiceProxy implements AqlQueryService {

    @Value("${openehr.endpoints.query.url}")
    private String endpointUrl;

    private RestTemplate restTemplate;

    /**
     * Initializes the REST client.
     */
    @PostConstruct
    public void initialize() {
        restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(createHttpClient()))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet executeAdhocQuery(AdhocQueryExecute adhocQueryExecute, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            headers.add(HttpHeaders.AUTHORIZATION, request.getHeader(HttpHeaders.AUTHORIZATION));
        }
        HttpEntity<AdhocQueryExecute> entity = new HttpEntity<>(adhocQueryExecute, headers);

        ResponseEntity<ResultSet> response = restTemplate.exchange(endpointUrl + "/aql", HttpMethod.POST, entity, ResultSet.class);
        return response.getBody();
    }

    /**
     * Creates an HTTP client.
     *
     * @return the HTTP client
     */
    private HttpClient createHttpClient() {
        try {
            return HttpClients.custom()
                    .setUserAgent("openEHR HttpClient")
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize an HTTP client for QueryServiceProxy", ex);
        }
    }
}
