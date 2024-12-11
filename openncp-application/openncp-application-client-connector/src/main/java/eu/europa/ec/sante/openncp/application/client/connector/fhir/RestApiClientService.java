package eu.europa.ec.sante.openncp.application.client.connector.fhir;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RestApiClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiClientService.class);

    private final RestTemplate restTemplate;

    private final ConfigurationManager configurationManager;

    private final KeyStoreManager keyStoreManager;

    private final String basePath;

    public RestApiClientService(final RestTemplateBuilder restTemplateBuilder, final ConfigurationManager configurationManager, final KeyStoreManager keyStoreManager) {
        this.configurationManager = configurationManager;
        this.keyStoreManager = keyStoreManager;
        this.basePath = configurationManager.getProperty("FHIR_REST_CLIENT_API");
        final SSLContext sslContext = getSSLContext();

        final CloseableHttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .build();
    }

    public ResponseEntity<String> search(final String countryCode, final String jwtToken, final Map<String, Set<String>> searchParams, final String resourcePath) {
        final HttpHeaders headers = getDefaultHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set("CountryCode", countryCode);

        final HttpEntity<Map<String, Object>> newRequest = new HttpEntity<>(headers);

        final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(basePath + resourcePath);
        searchParams.forEach((key, values) -> values.forEach(value -> uriBuilder.queryParam(key, value)));
        final URI uri = uriBuilder.encode().build().toUri();

        final ResponseEntity<String> response = this.restTemplate.exchange(uri, HttpMethod.GET, newRequest, String.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    public ResponseEntity<String> read(final String countryCode, final String jwtToken, final String id, final String resourcePath) {
        final HttpHeaders headers = getDefaultHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set("CountryCode", countryCode);

        final HttpEntity<Map<String, Object>> newRequest = new HttpEntity<>(headers);

        final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(basePath + resourcePath + "/" + id);
        final URI uri = uriBuilder.encode().build().toUri();

        final ResponseEntity<String> response = this.restTemplate.exchange(uri, HttpMethod.GET, newRequest, String.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    public ResponseEntity<String> post(final String countryCode, final String jwtToken, final Map<String, Object> payload, final String resourcePath) {
        final HttpHeaders headers = getDefaultHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set("CountryCode", countryCode);

        final HttpEntity<Map<String, Object>> newRequest = new HttpEntity<>(payload, headers);

        final String urlWithParams = basePath + resourcePath;

        final ResponseEntity<String> response = this.restTemplate.exchange(urlWithParams, HttpMethod.POST, newRequest, String.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private HttpHeaders getDefaultHeaders(final String correlationId) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-Correlation-ID", correlationId);
        return headers;
    }

    private HttpHeaders getDefaultHeaders() {
        return getDefaultHeaders(UUID.randomUUID().toString());
    }

    private SSLContext getSSLContext() {

        final SSLContext sslContext;
        try {
            final String sigKeystorePassword = configurationManager.getProperty(Constant.NCP_SIG_KEYSTORE_PASSWORD);

            sslContext = SSLContext.getInstance("TLSv1.2");

            final var keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStoreManager.getKeyStore(), sigKeystorePassword.toCharArray());

            final var trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStoreManager.getTrustStore());

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext;

        } catch (final KeyManagementException | UnrecoverableKeyException | KeyStoreException |
                       NoSuchAlgorithmException e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            return null;
        }
    }
}
