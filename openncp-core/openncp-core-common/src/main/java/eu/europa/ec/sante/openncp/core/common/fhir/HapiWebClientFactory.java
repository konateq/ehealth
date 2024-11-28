package eu.europa.ec.sante.openncp.core.common.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.core.common.CountryCode;
import eu.europa.ec.sante.openncp.core.common.HttpsClientConfiguration;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import org.apache.commons.lang3.Validate;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

@Component
public class HapiWebClientFactory {
    private final FhirContext fhirContext;
    private final DynamicDiscoveryService dynamicDiscoveryService;
    private final List<IClientInterceptor> clientInterceptors;


    public HapiWebClientFactory(final FhirContext fhirContext, final DynamicDiscoveryService dynamicDiscoveryService, final List<IClientInterceptor> clientInterceptors) {
        this.fhirContext = Validate.notNull(fhirContext, "FhirContext must not be null");
        this.dynamicDiscoveryService = Validate.notNull(dynamicDiscoveryService, "DynamicDiscoveryService must not be null");
        this.clientInterceptors = Validate.notNull(clientInterceptors, "ClientInterceptors must not be null");
    }

    public FhirDispatchingClient createClient(final String endpoint) {
        final HttpClient httpClient;
        try {
            httpClient = HttpClients
                    .custom()
                    .setSSLSocketFactory(HttpsClientConfiguration.buildSSLConnectionSocketFactory())
                    .setUserAgent("OpenNCP http client")
                    .build();
        } catch (final UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyStoreException |
                       IOException | KeyManagementException e) {
            throw new RuntimeException(String.format("Error building the FHIR SSL client: %s", e.getMessage()), e);
        }

        fhirContext.getRestfulClientFactory().setHttpClient(httpClient);
        final IGenericClient genericClient = fhirContext.newRestfulGenericClient(endpoint);
        clientInterceptors.forEach(genericClient::registerInterceptor);

        return new FhirDispatchingClient(genericClient);
    }

    public FhirDispatchingClient createClient(final CountryCode countryCode) {
        final String endpointUrl = dynamicDiscoveryService.getEndpointUrl(countryCode.value(), RegisteredService.FHIR_SERVICE);
        return createClient(endpointUrl);
    }

    public FhirDispatchingClient createClient(final EuRequestDetails requestDetails) {
        return createClient(requestDetails.getCountryCode());
    }
}
