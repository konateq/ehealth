package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.core.common.fhir.audit.*;
import eu.europa.ec.sante.openncp.core.common.fhir.context.FhirSupportedResourceType;
import org.apache.commons.lang3.BooleanUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class PatientAuditEventProducer extends AbstractAuditEventProducer implements AuditEventProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientAuditEventProducer.class);
    public static final Predicate<IBaseResource> RESOURCE_IS_PATIENT = resource -> resource.getIdElement().getResourceType().equalsIgnoreCase(ResourceType.Patient.getPath());

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
        final List<AuditEventData> auditEventDataList;
        switch (auditableEvent.getEuRequestDetails().getRestOperationType()) {
            case SEARCH_TYPE:
            case SEARCH_SYSTEM:
            case GET_PAGE:
                auditEventDataList = List.of(handleSearch(auditableEvent));
                break;
            case VREAD:
            case READ:
                auditEventDataList = handleRead(auditableEvent);
                break;
            default:
                LOGGER.error("Unsupported fhir REST operation type [{}]", auditableEvent.getEuRequestDetails().getRestOperationType());
                //TODO what to do here exactly? create a file with the error? we cannot let the audit event create exceptions that will interfere with the response.
                return Collections.emptyList();
        }

        return auditEventDataList;
    }

    private AuditEventData handleSearch(final AuditableEvent auditableEvent) {
        final List<AuditEventData.ParticipantData> participants = createParticipants();
        final Set<String> patientIds = auditableEvent.extractResourceIds(RESOURCE_IS_PATIENT);
        final List<AuditEventData.EntityData> patientEntities = patientIds.stream()
                .map(patientId -> ImmutableEntityData.builder()
                        .id(patientId)
                        .type(ImmutableEntityType.of(BalpConstants.CS_AUDIT_ENTITY_TYPE_1_PERSON, Optional.of(BalpConstants.CS_AUDIT_ENTITY_TYPE_1_PERSON_DISPLAY)))
                        .role(ImmutableEntityRole.of(BalpConstants.CS_OBJECT_ROLE_1_PATIENT, Optional.of(BalpConstants.CS_OBJECT_ROLE_1_PATIENT_DISPLAY)))
                        .display("Patient")
                        .build())
                .collect(Collectors.toList());
        final AuditEventData auditEventData;
        if (patientEntities.isEmpty()) {
            auditEventData = ImmutableAuditEventData.builder()
                    .auditResourceType(ResourceType.Patient.name())
                    .metaData(createMetaData(auditableEvent))
                    .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                    .profile(BalpProfileEnum.BASIC_QUERY)
                    .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                    .addAllParticipants(participants)
                    .build();
        } else {
            auditEventData = ImmutableAuditEventData.builder()
                    .auditResourceType(ResourceType.Patient.name())
                    .metaData(createMetaData(auditableEvent))
                    .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                    .profile(BalpProfileEnum.PATIENT_QUERY)
                    .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                    .addAllParticipants(participants)
                    .addAllEntities(patientEntities)
                    .build();
        }

        return auditEventData;
    }

    private List<AuditEventData> handleRead(final AuditableEvent auditableEvent) {
        final List<AuditEventData.ParticipantData> participants = createParticipants();

        return auditableEvent.getResource().map(resource -> {
            final String dataResourceId = ((Resource)resource).getId();
            final Set<String> patientIds = auditableEvent.extractResourceIds(RESOURCE_IS_PATIENT);

            final List<AuditEventData> auditEventDataList = new ArrayList<>();
            if (patientIds.isEmpty()) {
                // this is a basic read so create a basic read audit event
                final AuditEventData.EntityData resourceEntity = AuditEventData.EntityData.ofResource(dataResourceId);
                auditEventDataList.add(ImmutableAuditEventData.builder()
                        .auditResourceType(ResourceType.Patient.name())
                        .metaData(createMetaData(auditableEvent))
                        .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                        .profile(BalpProfileEnum.BASIC_READ)
                        .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                        .addAllParticipants(participants)
                        .addEntity(resourceEntity)
                        .build());
            } else {
                // If the resource is in the Patient compartment, create one audit event for each compartment owner
                patientIds.stream()
                        .map(patientId -> {
                            final AuditEventData.EntityData resourceEntityData = AuditEventData.EntityData.ofResource(dataResourceId);
                            final AuditEventData.EntityData patientEntityData = AuditEventData.EntityData.ofPatient(patientId);

                            return ImmutableAuditEventData.builder()
                                    .auditResourceType(ResourceType.Patient.name())
                                    .metaData(createMetaData(auditableEvent))
                                    .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                                    .profile(BalpProfileEnum.PATIENT_READ)
                                    .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                                    .addAllParticipants(participants)
                                    .addEntity(resourceEntityData)
                                    .addEntity(patientEntityData)
                                    .build();

                        })
                        .forEach(auditEventDataList::add);
            }
            return auditEventDataList;
        }).orElse(Collections.emptyList());
    }
}
