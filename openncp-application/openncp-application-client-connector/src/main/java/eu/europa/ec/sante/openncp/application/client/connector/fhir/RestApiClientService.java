package eu.europa.ec.sante.openncp.application.client.connector.fhir;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Map;

@Service
public class RestApiClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiClientService.class);

    private final RestTemplate restTemplate;

    private final ConfigurationManager configurationManager;

    private final KeyStoreManager keyStoreManager;

    public RestApiClientService(RestTemplateBuilder restTemplateBuilder, ConfigurationManager configurationManager, KeyStoreManager keyStoreManager) {
        this.configurationManager = configurationManager;
        this.keyStoreManager = keyStoreManager;
        final SSLContext sslContext = getSSLContext();

        final CloseableHttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .build();
    }

    public ResponseEntity<String> search(final String countryCode, final String jwtToken, final Map<String, String> searchParams, String resourcePath) {
        HttpHeaders headers = getHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set("CountryCode", countryCode);

        HttpEntity<Map<String, Object>> newRequest;
        newRequest = new HttpEntity<>(headers);

        String urlWithParams = configurationManager.getProperty("FHIR_REST_CLIENT_API") + resourcePath + "?" + searchParams.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((param1, param2) -> param1 + "&" + param2)
                .orElse("");

        ResponseEntity<String> response =  this.restTemplate.exchange(urlWithParams, HttpMethod.GET, newRequest, String.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    private SSLContext getSSLContext() {

        SSLContext sslContext;
        try {
            String sigKeystorePassword = configurationManager.getProperty(Constant.NCP_SIG_KEYSTORE_PASSWORD);

            sslContext = SSLContext.getInstance("TLSv1.2");

            var keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStoreManager.getKeyStore(), sigKeystorePassword.toCharArray());

            var trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStoreManager.getTrustStore());

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext;

        } catch (KeyManagementException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            return null;
        }
    }
}
