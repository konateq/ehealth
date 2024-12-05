package eu.europa.ec.sante.openncp.application.client.connector.request;

import eu.europa.ec.sante.openncp.common.Loinc;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.opensaml.saml.saml2.core.Assertion;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    Optional<LocalDateTime> getStudyDate();

    Optional<DateRange> getCreatedBetweenRange();

    default Map<String, Set<String>> getSearchParameters() {
        final Map<String, Set<String>> payload = new HashMap<>();
        payload.put("patient.identifier", Set.of(getPatientId().getRoot() + "|" + getPatientId().getExtension()));
        payload.put("type", Set.of(Loinc.MEDICAL_IMAGE_STUDY.getFhirReference()));

        final Set<String> specificSearchParameters = Stream.of(
                        getModalityCode().map(modality -> "urn:oid:1.2.840.10008.6.1.19|" + modality),
                        getBodyPartCode().map(bodyPart -> "http://snomed.info/sct|" + bodyPart)
                )
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());


        // TODO study date
        // TODO createdBetween

        if (!specificSearchParameters.isEmpty()) {
            payload.put("event", specificSearchParameters);
        }

        return payload;
    }
}
