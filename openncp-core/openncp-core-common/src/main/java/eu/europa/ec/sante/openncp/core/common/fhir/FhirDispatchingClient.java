package eu.europa.ec.sante.openncp.core.common.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class FhirDispatchingClient {
    private final IGenericClient genericClient;

    public FhirDispatchingClient(final IGenericClient genericClient) {
        this.genericClient = genericClient;
    }

    public Bundle dispatchReadAsBundle(final EuRequestDetails requestDetails, final String JWTToken) {
        final String uri = getPassThroughUrl(requestDetails);
        if (requestDetails.getRestOperationType() == RestOperationTypeEnum.SEARCH_TYPE) {
            if (JWTToken != null) {
                Bundle bundleResult = genericClient.search().byUrl(uri).withAdditionalHeader("Authorization", JWTToken).returnBundle(Bundle.class).execute();
                return bundleResult;
            } else {
                return genericClient.search().byUrl(uri).returnBundle(Bundle.class).execute();
            }
        }
        if (requestDetails.getRestOperationType() == RestOperationTypeEnum.READ) {
            final IBaseResource response;
            if (JWTToken != null) {
                response = genericClient.read().resource(requestDetails.getResourceType()).withUrl(uri).withAdditionalHeader("Authorization", JWTToken).execute();
            } else {
                response = genericClient.read().resource(requestDetails.getResourceType()).withUrl(uri).execute();
            }
            if (response instanceof Bundle) {
                return (Bundle) response;
            } else {
                throw new IllegalArgumentException(String.format("Response resource is expected to be a bundle, but was [%s]", response.getClass().getSimpleName()));
            }
        }

        throw new UnsupportedOperationException("Currently only the \"search\" and \"read\" operations are supported");
    }

    public MethodOutcome dispatchWrite(final EuRequestDetails requestDetails, final String JWTToken) {
        requestDetails.getHapiRequestDetails().getB
        if (requestDetails.getRestOperationType() == RestOperationTypeEnum.CREATE) {
            ICreateTyped createType = genericClient.create().resource(requestDetails.getResourceType()).
            if (JWTToken != null) {
                createType = createType.withAdditionalHeader("Authorization", JWTToken);
            }
            return createType.execute();
        }
        throw new UnsupportedOperationException("Only the \"write\" operations are supported");
    }

    private String getPassThroughUrl(final EuRequestDetails requestDetails) {
        final MultiValueMap<String, String> parameterMap = new LinkedMultiValueMap<>(
                requestDetails.getHapiRequestDetails().getParameters().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue()))));

        final String uri = UriComponentsBuilder.fromHttpUrl(genericClient.getServerBase())
                .path(requestDetails.getHapiRequestDetails().getRequestPath())
                .queryParams(parameterMap)
                .build()
                .toUriString();

        return uri;
    }
}
