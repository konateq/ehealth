package eu.europa.ec.sante.openncp.core.server.nc.mock.dicom;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class OrthancRestClientConfig {
    @Bean
    @Qualifier("orthancRestTemplate")
    public RestTemplate orthancRestTemplate() {
        return new RestTemplateBuilder()
                .interceptors(basicAuthInterceptor("orthanc", "orthanc"))
                .build();
    }

    private ClientHttpRequestInterceptor basicAuthInterceptor(String username, String password) {
        return (request, body, execution) -> {
            final String auth = username + ":" + password;
            final byte[] encodedAuth = Base64.getEncoder()
                    .encode(auth.getBytes(StandardCharsets.UTF_8));
            final String authHeader = "Basic " + new String(encodedAuth);

            request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
            return execution.execute(request, body);
        };
    }
}
