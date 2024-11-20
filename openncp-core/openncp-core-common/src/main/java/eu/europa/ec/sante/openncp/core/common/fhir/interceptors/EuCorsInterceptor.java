package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;

@Component
public class EuCorsInterceptor extends CorsInterceptor {

    public EuCorsInterceptor() {
        super(createConfig());
    }

    private static CorsConfiguration createConfig() {
        final CorsConfiguration config = new CorsConfiguration();

        config.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
        config.addAllowedOrigin("*");

        config.addExposedHeader("Location");
        config.addExposedHeader("Content-Location");
        config.setAllowedMethods(Arrays.asList("GET", "OPTIONS", "POST"));

        return config;
    }
}
