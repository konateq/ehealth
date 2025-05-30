package eu.europa.ec.sante.openncp.core.server.nc.mock.openehr;

import eu.europa.ec.sante.openncp.core.common.openehr.domain.AdhocQueryExecute;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.CompositionRequest;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.ResultSet;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.TemplateRequest;
import eu.europa.ec.sante.openncp.core.common.openehr.service.AqlQueryService;
import org.apache.commons.codec.binary.Base64;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    @Value("${openehr.endpoints.query.user}")
    private String user;
    @Value("${openehr.endpoints.query.pass}")
    private String pass;
    @Value("${openehr.endpoints.query.token}")
    private String token;

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
        headers.add(HttpHeaders.AUTHORIZATION, getAuthHeaders());

        HttpEntity<AdhocQueryExecute> entity = new HttpEntity<>(adhocQueryExecute, headers);

        ResponseEntity<ResultSet> response = restTemplate.exchange(endpointUrl + "/query/aql", HttpMethod.POST, entity, ResultSet.class);
        return response.getBody();
    }

    @Override
    public List<String> getAvailableTemplatesForPatient(TemplateRequest templateRequest, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, getAuthHeaders());

        String query = "SELECT " +
                       "c/archetype_details/template_id/value " +
                       "FROM " +
                       "EHR e " +
                       "CONTAINS COMPOSITION c " +
                       "WHERE " +
                       "e/ehr_id/value = \"$ehr_id\" "; // better
                       //"e/ehr_id/value = $ehr_id "; // ehrbase

        AdhocQueryExecute adhocQueryExecute = new AdhocQueryExecute(query);
        adhocQueryExecute.addQueryParameters("ehr_id", templateRequest.getPatientId());
        HttpEntity<AdhocQueryExecute> entity1 = new HttpEntity<>(adhocQueryExecute, headers);

        ResponseEntity<ResultSet> response = restTemplate.exchange(endpointUrl + "/query/aql", HttpMethod.POST, entity1, ResultSet.class);
        List<String> templateNames = List.of();
        if (response.getBody() != null) {
            templateNames = response.getBody().getRows().stream()
                    .map(e -> e.get(0).toString())
                    .collect(Collectors.toList());
        }

        headers.add("Accept", "application/openehr.wt+json");
        HttpEntity<AdhocQueryExecute> entity2 = new HttpEntity<>(headers);

        List<String> templates = new ArrayList<>();
        templateNames.forEach(templateName -> {
            ResponseEntity<String> response2 = restTemplate.exchange(
                    endpointUrl + " /definition/template/adl1.4/" + templateName, HttpMethod.GET, entity2, String.class);
            if (response2.getBody() != null) {
                templates.add(response2.getBody()
                        .replace("rm_type", "rmType") // better
                        .replace("template_id", "templateId") // better
                        .replace("localized_names", "localizedNames") // better
                );
            }
        });

        return templates;
    }

    @Override
    public List<String> getOpenEhrCompositions(CompositionRequest compositionRequest, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, getAuthHeaders());

        String query = "SELECT " +
                       "c/uid/value " +
                       "FROM " +
                       "EHR e " +
                       "CONTAINS COMPOSITION c " +
                       "WHERE " +
                       "e/ehr_id/value = \"$ehr_id\" " + // better
                       //"e/ehr_id/value = $ehr_id " + // ehrbase
                       "AND " +
                       "c/archetype_details/template_id/value = \"$template\" "; // better
                       //"c/archetype_details/template_id/value = $template "; // ehrbase

        AdhocQueryExecute adhocQueryExecute = new AdhocQueryExecute(query);
        adhocQueryExecute.addQueryParameters("ehr_id", compositionRequest.getPatientId());
        adhocQueryExecute.addQueryParameters("template", compositionRequest.getTemplateName());
        HttpEntity<AdhocQueryExecute> entity1 = new HttpEntity<>(adhocQueryExecute, headers);

        ResponseEntity<ResultSet> response = restTemplate.exchange(endpointUrl + "/query/aql", HttpMethod.POST, entity1, ResultSet.class);
        List<String> uids = List.of();
        if (response.getBody() != null) {
            uids = response.getBody().getRows().stream()
                    .map(e -> e.get(0).toString())
                    .collect(Collectors.toList());
        }

        headers.add("Accept", "application/openehr.wt.flat+json"); // better
        //headers.add("Accept", "application/openehr.wt.flat.schema+json"); // ehrbase
        HttpEntity<AdhocQueryExecute> entity2 = new HttpEntity<>(headers);

        List<String> compositions = new ArrayList<>();
        uids.forEach(uid -> {
            ResponseEntity<String> response2 = restTemplate.exchange(
                    endpointUrl + "/ehr/" + compositionRequest.getPatientId() + "/composition/" + uid, HttpMethod.GET, entity2, String.class);
            if (response2.getBody() != null) {
                compositions.add(response2.getBody());
            }
        });

        return compositions;
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

    private String getAuthHeaders() {
        if (token != null && !token.equals("")) {
            return "Bearer " + token;
        } else {
            String plainCreds = user + ":" + pass;
            byte[] plainCredsBytes = plainCreds.getBytes();
            byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            String base64Creds = new String(base64CredsBytes);

            return "Basic " + base64Creds;
        }
    }
}
