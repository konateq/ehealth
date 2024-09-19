package eu.europa.ec.sante.openncp.core.common.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.JwtToken;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.util.Validate;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class FhirDispatchingClient {
    private final IGenericClient genericClient;

    public FhirDispatchingClient(final IGenericClient genericClient) {
        this.genericClient = genericClient;
    }

    public Bundle dispatchSearch(final DispatchContext dispatchContext) {
        return dispatchOperation(
                dispatchContext,
                RestOperationTypeEnum.SEARCH_TYPE,
                (jwtToken, dispatchUri) -> genericClient.search()
                        .byUrl(dispatchUri.toString())
                        .withAdditionalHeader("Authorization", jwtToken.getAuthorizationHeaderValue())
                        .returnBundle(Bundle.class)
                        .execute()
        );
    }

    public Bundle dispatchRead(final DispatchContext dispatchContext) {
        return dispatchOperation(
                dispatchContext,
                RestOperationTypeEnum.READ,
                (jwtToken, dispatchUri) -> {
                    final IBaseResource response = genericClient.read()
                            .resource(dispatchContext.getResourceType())
                            .withUrl(dispatchUri.toString())
                            .withAdditionalHeader("Authorization", jwtToken.getAuthorizationHeaderValue())
                            .execute();
                    if (response instanceof Bundle) {
                        return (Bundle) response;
                    } else {
                        throw new IllegalArgumentException(String.format("Response resource is expected to be a bundle, but was [%s]", response.getClass().getSimpleName()));
                    }
                }
        );
    }

    public MethodOutcome dispatchWrite(final DispatchContext dispatchContext, Bundle bundleToCreate) {
        return dispatchOperation(
                dispatchContext,
                RestOperationTypeEnum.CREATE,
                (jwtToken, dispatchUri) -> genericClient.create()
                        .resource(bundleToCreate)
                        .withAdditionalHeader("Authorization", jwtToken.getAuthorizationHeaderValue())
                        .execute()
        );
    }

    private <R> R dispatchOperation(
            DispatchContext dispatchContext,
            RestOperationTypeEnum expectedOperation,
            BiFunction<JwtToken, URI, R> fhirDispatchOperation) {

        Validate.notNull(dispatchContext, "dispatchContext must not be null");
        Validate.notNull(expectedOperation, "expectedOperation must not be null");
        Validate.notNull(fhirDispatchOperation, "fhirDispatchOperation must not be null");

        // Validate the expected operation type
        if (dispatchContext.getRestOperationType() != expectedOperation) {
            throw new UnsupportedOperationException(String.format(
                    "This method only supports the \"%s\" operation but the dispatchContext has a [%s] operation",
                    expectedOperation, dispatchContext.getRestOperationType()));
        }

        final JwtToken jwtToken = dispatchContext.getJwtTokenFromRequest();
        final URI dispatchUri = this.getDispatchUri(dispatchContext);
        return fhirDispatchOperation.apply(jwtToken, dispatchUri);
    }

    private URI getDispatchUri(final DispatchContext dispatchContext) {
        final MultiValueMap<String, String> parameterMap = new LinkedMultiValueMap<>(
                dispatchContext.getHapiRequestDetails().getParameters().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue()))));

        return UriComponentsBuilder.fromHttpUrl(genericClient.getServerBase())
                .path(dispatchContext.getHapiRequestDetails().getRequestPath())
                .queryParams(parameterMap)
                .build()
                .toUri();
    }
}
