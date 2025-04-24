package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.BalpProfileEnum;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.ImmutableAuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.ImmutableIdentifier;
import eu.europa.ec.sante.openncp.core.common.fhir.context.FhirSupportedResourceType;
import org.apache.commons.lang3.BooleanUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PatientAuditEventProducer extends AbstractAuditEventProducer implements AuditEventProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientAuditEventProducer.class);

    @Override
    public boolean accepts(final AuditableEvent auditableEvent) {
        final boolean accepts = auditableEvent != null
                && auditableEvent.getEuRequestDetails().isPatient()
                && auditableEvent.resourceIsOfType(FhirSupportedResourceType.BUNDLE, FhirSupportedResourceType.PATIENT);

        LOGGER.debug("[{}] auditable event [{}]", BooleanUtils.toString(accepts, "Accepted", "Rejected"), auditableEvent);
        return accepts;
    }

    @Override
    public List<AuditEventData> produce(final AuditableEvent auditableEvent) {
        switch (auditableEvent.getEuRequestDetails().getRestOperationType()) {
            case SEARCH_TYPE:
            case SEARCH_SYSTEM:
            case GET_PAGE:
                return this.handleSearch(auditableEvent);
            case VREAD:
            case READ:
                return this.handleRead(auditableEvent);
            default:
                LOGGER.error("Unsupported fhir REST operation type [{}]", auditableEvent.getEuRequestDetails().getRestOperationType());
                //TODO what to do here exactly? create a file with the error? we cannot let the audit event create exceptions that will interfere with the response.
                return Collections.emptyList();
        }
    }

    private List<AuditEventData> handleSearch(final AuditableEvent auditableEvent) {
        return this.handle(auditableEvent, BalpProfileEnum.BASIC_QUERY);
    }
    private List<AuditEventData> handleRead(final AuditableEvent auditableEvent) {
        return this.handle(auditableEvent, BalpProfileEnum.BASIC_READ);
    }


    private List<AuditEventData> handle(final AuditableEvent auditableEvent, final BalpProfileEnum profile) {
        final List<AuditEventData.ParticipantData> participants = this.createParticipants();

        return auditableEvent.getResource()
                .filter(iBaseResource -> iBaseResource instanceof Bundle)
                .map(iBaseResource -> (Bundle) iBaseResource)
                .map(bundle -> bundle.getEntry()
                        .stream()
                        .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType() == ResourceType.Patient)
                        .map(bundleEntryComponent -> (Patient) bundleEntryComponent.getResource())
                        .map(patientEntity ->
                                (AuditEventData) ImmutableAuditEventData.builder()
                                        .auditResourceType(ResourceType.Patient.name())
                                        .metaData(this.createMetaData(auditableEvent))
                                        .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                                        .profile(profile)
                                        .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                                        .addAllParticipants(participants)
                                        .addEntity(AuditEventData.EntityData.ofPatient(
                                                patientEntity.getId(),
                                                patientEntity.getIdentifier()
                                                        .stream()
                                                        .map(identifier -> ImmutableIdentifier.of(identifier.getSystem(), identifier.getValue()))
                                                        .findFirst()
                                                        .orElseThrow(() -> new RuntimeException("No identifier found for patient"))))
                                        .build())
                        .collect(Collectors.toList())).orElse(Collections.emptyList());
    }
}
