package eu.europa.ec.sante.openncp.application.client.connector.request;

import ca.uhn.fhir.rest.param.DateRangeParam;
import eu.europa.ec.sante.openncp.common.Loinc;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.opensaml.saml.saml2.core.Assertion;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Domain
public interface MedicalImagingStudyRequest extends OpenNcpRequest {
    @Override
    Map<AssertionType, Assertion> getAssertions();

    @Override
    String getCountryCode();

    @Override
    PatientId getPatientId();

    Optional<String> getModalityCode();

    Optional<String> getBodyPartCode();

    Optional<DateRange> getProcedureDateBetweenRange();

    default Map<String, Set<String>> getSearchParameters() {
        final Map<String, Set<String>> payload = new HashMap<>();
        payload.put("patient.identifier", Set.of(getPatientId().getRoot() + "|" + getPatientId().getExtension()));
        payload.put("type", Set.of(Loinc.MEDICAL_IMAGE_STUDY.getFhirReference()));

        final Set<String> periodQueryParams = getProcedureDateBetweenRange()
                .map(dateRange -> {
                    final DateRangeParam dateRangeParam = new DateRangeParam();
                    dateRange.getFrom()
                            .map(lowerBoundLocalDate -> Date.from(lowerBoundLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()))
                            .map(dateRangeParam::setLowerBoundInclusive);
                    dateRange.getTo()
                            .map(upperBoundLocalDate -> Date.from(upperBoundLocalDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()))
                            .map(dateRangeParam::setUpperBoundInclusive);

                    return dateRangeParam;

                })
                .stream() // Convert Optional<DateRangeParam> into a Stream<DateRangeParam>
                .flatMap(dateRangeParam -> dateRangeParam.getValuesAsQueryTokens().stream()) // Flatten query tokens
                .map(dateParam -> dateParam.getPrefix().getValue() + dateParam.getValueAsString()) // Format tokens
                .collect(Collectors.toSet());
        if (!periodQueryParams.isEmpty()) {
            payload.put("period", periodQueryParams);
        }

        final Set<String> specificSearchParameters = Stream.of(
                        getModalityCode().map(modality -> "urn:oid:1.2.840.10008.6.1.19|" + modality),
                        getBodyPartCode().map(bodyPart -> "http://snomed.info/sct|" + bodyPart)
                )
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        if (!specificSearchParameters.isEmpty()) {
            payload.put("event", specificSearchParameters);
        }

        return payload;
    }
}
