package eu.europa.ec.sante.openncp.core.common.fhir.context;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.common.CountryCode;
import eu.europa.ec.sante.openncp.core.common.fhir.interceptors.CountryCodeInterceptor;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IIdType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper around the {@link RequestDetails}
 */
@Domain
public interface DispatchContext {
    NcpSide getNcpSide();

    RequestDetails getHapiRequestDetails();

    HttpServletRequest getServletRequest();

    HttpServletResponse getServletResponse();

    default CountryCode getCountryCode() {
        final String countryCode = getHapiRequestDetails().getHeader(CountryCodeInterceptor.COUNTRY_CODE_HEADER_KEY);
        if (countryCode == null) {
            throw new IllegalArgumentException(String.format("There was no [%1$s] header found, please add a header with key [%1$s] that contains a valid ISO 3166-1 alpha-2 code.", CountryCodeInterceptor.COUNTRY_CODE_HEADER_KEY));
        }
        return CountryCode.of(countryCode);
    }

    default Optional<JwtToken> getJwtTokenFromRequest() {
        return JwtToken.extractFrom(getServletRequest());
    }

    default RestOperationTypeEnum getRestOperationType() {
        return getHapiRequestDetails().getRestOperationType();
    }

    default Optional<String[]> getParameter(String parameterName) {
        return Optional.ofNullable(getHapiRequestDetails().getParameters().get(parameterName));
    }

    default Optional<FhirSupportedResourceType> getSupportedResourceType() {
        return FhirSupportedResourceType.ofRequestPath(getHapiRequestDetails());
    }

    default String getResourceType() {
        return getSupportedResourceType()
                .map(FhirSupportedResourceType::getRequest)
                .map(FhirSupportedResourceType.Request::getPath)
                .orElseGet(() -> getHapiRequestDetails().getResourceName());
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
        final String serverBaseUrl = getHapiRequestDetails().getFhirServerBase();
        final String resourceName = idElement.getResourceType();
        return idElement.withServerBase(serverBaseUrl, resourceName).getValue();
    }


}
