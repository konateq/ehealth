package eu.europa.ec.sante.openncp.core.common.fhir.context;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.common.CountryCode;
import eu.europa.ec.sante.openncp.core.common.fhir.interceptors.CountryCodeInterceptor;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wrapper around the {@link RequestDetails}
 */
@Domain
public interface DispatchContext {
    NcpSide getNcpSide();

    Optional<RequestDetails> getHapiRequestDetails();

    HttpServletRequest getServletRequest();

    HttpServletResponse getServletResponse();

    default CountryCode getCountryCode() {
        final String countryCode = getServletRequest().getHeader(CountryCodeInterceptor.COUNTRY_CODE_HEADER_KEY);
        if (countryCode == null) {
            throw new IllegalArgumentException(String.format("There was no [%1$s] header found, please add a header with key [%1$s] that contains a valid ISO 3166-1 alpha-2 code.", CountryCodeInterceptor.COUNTRY_CODE_HEADER_KEY));
        }
        return CountryCode.of(countryCode);
    }

    default Optional<JwtToken> getJwtTokenFromRequest() {
        return JwtToken.extractFrom(getServletRequest());
    }

    default Optional<RestOperationTypeEnum> getHapiRestOperationType() {
        return getHapiRequestDetails().map(RequestDetails::getRestOperationType);
    }

    default Optional<String[]> getParameter(final String parameterName) {
        final List<String> values = getParameterMap().get(parameterName);
        return Optional.ofNullable(values)
                .map(list -> list.toArray(String[]::new));
    }

    default MultiValueMap<String, String> getParameterMap() {
        final Map<String, String[]> parameterMap = getHapiRequestDetails()
                .map(RequestDetails::getParameters)
                .orElseGet(() -> getServletRequest().getParameterMap());

        return new LinkedMultiValueMap<>(
                parameterMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue()))));

    }

    default Optional<FhirSupportedResourceType> getSupportedResourceType() {
        return FhirSupportedResourceType.findFhirSupportedResourceType(this);
    }

    default String getResourceName() {
        return getHapiRequestDetails()
                .map(RequestDetails::getResourceName)
                .orElseGet(() -> getServletRequest().getContextPath());
    }

    default String getRequestPath() {
        return getHapiRequestDetails()
                .map(RequestDetails::getRequestPath)
                .orElseGet(() -> {
                    final String path = getServletRequest().getRequestURI(); // for example /fhir/documentReference
                    final String[] segments = path.split("/");
                    return segments.length > 0 ? segments[segments.length - 1] : null; // Return the last segment
                });
    }

    default String getSpecificResourceType() {
        return FhirSupportedResourceType.findFhirSupportedResourceType(this)
                .map(FhirSupportedResourceType::name)
                .orElseGet(this::getResourceName);
    }

    default boolean isPatient() {
        return getSupportedResourceType()
                .filter(fhirSupportedResourceType -> FhirSupportedResourceType.PATIENT == fhirSupportedResourceType)
                .isPresent();
    }

    default boolean isDocumentReference() {
        return getSupportedResourceType()
                .filter(fhirSupportedResourceType -> FhirSupportedResourceType.DOCUMENT_REFERENCE == fhirSupportedResourceType)
                .isPresent();
    }

    /**
     * Creates a fully qualified resource reference from an {@link IIdType} by injecting the server's base URL into the ID
     */
    default String createFullyQualifiedResourceReference(final IIdType idElement) {
        Validate.notNull(idElement, "IdElement must not be null");
        final String serverBaseUrl = getHapiRequestDetails()
                .map(RequestDetails::getFhirServerBase)
                .orElseGet(() -> getServletRequest().getServerName());
        final String resourceName = idElement.getResourceType();
        return idElement.withServerBase(serverBaseUrl, resourceName).getValue();
    }


}
