package eu.europa.ec.sante.openncp.core.common.fhir.context;

import eu.europa.ec.sante.openncp.common.Loinc;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CompositionLabReportMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DiagnosticReportLabMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.PatientMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ServiceRequestLabMyHealthEu;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public enum FhirSupportedResourceType {
    PATIENT(new PathRequestMatcher("Patient"), CustomFhirResource.of(PatientMyHealthEu.class)),
    COMPOSITION(new PathRequestMatcher("Composition"), CustomFhirResource.of(CompositionLabReportMyHealthEu.class)),
    SERVICE_REQUEST(new PathRequestMatcher("ServiceRequest"), CustomFhirResource.of(ServiceRequestLabMyHealthEu.class)),
    DIAGNOSTIC_REPORT(new PathRequestMatcher("DiagnosticReport"), CustomFhirResource.of(DiagnosticReportLabMyHealthEu.class)),
    BUNDLE(new PathRequestMatcher("Bundle"), CustomFhirResource.none()),
    DOCUMENT_REFERENCE(new PathRequestMatcher("DocumentReference"), CustomFhirResource.none()),
    LAB_RESULT(new PathAndFhirTypeLoincRequestMatcher("DocumentReference", Loinc.LAB_RESULT), CustomFhirResource.none()),
    MEDICAL_IMAGING(new PathAndFhirTypeLoincRequestMatcher("DocumentReference", Loinc.MEDICAL_IMAGE_STUDY), CustomFhirResource.none()),
    DICOM(new PathRequestMatcher("Dicom"), CustomFhirResource.none()),
    METADATA(new PathRequestMatcher("metadata"), CustomFhirResource.none());

    private final RequestMatcher requestMatcher;
    private final CustomFhirResource customFhirResource;

    FhirSupportedResourceType(final RequestMatcher requestMatcher, final CustomFhirResource customFhirResource) {
        this.requestMatcher = Validate.notNull(requestMatcher, "requestMatcher cannot be null");
        this.customFhirResource = Validate.notNull(customFhirResource, "customFhirResource cannot be null");
    }

    public RequestMatcher getRequestMatcher() {
        return requestMatcher;
    }

    public CustomFhirResource getCustomFhirResource() {
        return customFhirResource;
    }

    /**
     * Finds the first matching FhirSupportedResourceType based on the DispatchContext.
     * Matches from PathAndFhirTypeRequestMatcher are prioritized over PathRequestMatcher.
     *
     * @param dispatchContext the DispatchContext to match
     * @return an Optional containing the prioritized matching FhirSupportedResourceType, if any
     */
    public static Optional<FhirSupportedResourceType> findFhirSupportedResourceType(final DispatchContext dispatchContext) {
        return Arrays.stream(values())
                .filter(resourceType -> resourceType.getRequestMatcher().matches(dispatchContext))
                .min(Comparator.comparingInt(resourceType ->
                        resourceType.getRequestMatcher() instanceof PathAndFhirTypeLoincRequestMatcher ? 0 : 1));
    }
}
