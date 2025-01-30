package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.*;
import eu.europa.ec.sante.openncp.core.common.fhir.context.FhirSupportedResourceType;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class PatientResponseAuditEventProducer implements AuditEventProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientResponseAuditEventProducer.class);
    public static final Predicate<IBaseResource> RESOURCE_IS_PATIENT = resource -> resource.getIdElement().getResourceType().equalsIgnoreCase(ResourceType.Patient.getPath());
    private final AuditEventBuilder auditEventBuilder;

    public PatientResponseAuditEventProducer(final AuditEventBuilder auditEventBuilder) {
        this.auditEventBuilder = Validate.notNull(auditEventBuilder, "AuditEventBuilder must not be null.");
    }

    @Override
    public boolean accepts(final AuditableEvent auditableEvent) {
        final boolean accepts = auditableEvent != null
                && auditableEvent.getEuRequestDetails().isPatient()
                && auditableEvent.resourceIsOfType(FhirSupportedResourceType.BUNDLE, FhirSupportedResourceType.PATIENT);

        LOGGER.debug("[{}] auditable event [{}]", BooleanUtils.toString(accepts, "Accepted", "Rejected"), auditableEvent);
        return accepts;
    }

    @Override
    public List<AuditEvent> produce(final AuditableEvent auditableEvent) {
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

        return auditEventDataList.stream().map(auditEventBuilder::build).collect(Collectors.toList());
    }

    private AuditEventData.MetaData createMetaData(final AuditableEvent auditableEvent) {
        return ImmutableMetaData.builder()
                .recordDateTime(auditableEvent.getTimestamp())
                .correlationId(LogContext.getCorrelationId())
                .build();
    }

    private List<AuditEventData.ParticipantData> createParticipants() {

        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final AuditSecurityInfo auditSecurityInfo = (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();

        //TODO build proper participant data
        final AuditEventData.ParticipantData serviceConsumer = ImmutableParticipantData.builder()
                .id(usernamePasswordAuthenticationToken.getName())
                .roleCode(auditSecurityInfo.getSamlDetails().getHcpAssertion()
                        .map(AssertionDetails::getElement)
                        .map(SoapElementHelper::getRoleID)
                        .orElse(StringUtils.EMPTY))
                .requestor(false)
                .network(auditSecurityInfo.getRequestIp())
                .build();

        final AuditEventData.ParticipantData serviceProvider = ImmutableParticipantData.builder()
                .id((String)usernamePasswordAuthenticationToken.getCredentials())
                .roleCode("provider role unknown")
                .requestor(true)
                .network(auditSecurityInfo.getHostIp())
                .build();

        return List.of(serviceConsumer, serviceProvider);
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
                    .metaData(createMetaData(auditableEvent))
                    .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                    .profile(BalpProfileEnum.BASIC_QUERY)
                    .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                    .addAllParticipants(participants)
                    .build();
        } else {
            auditEventData = ImmutableAuditEventData.builder()
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
            final String dataResourceId = auditableEvent.getEuRequestDetails().createFullyQualifiedResourceReference(resource.getIdElement());
            final Set<String> patientIds = auditableEvent.extractResourceIds(RESOURCE_IS_PATIENT);

            final List<AuditEventData> auditEventDataList = new ArrayList<>();
            if (patientIds.isEmpty()) {
                // this is a basic read so create a basic read audit event
                final AuditEventData.EntityData resourceEntity = AuditEventData.EntityData.ofResource(dataResourceId);
                auditEventDataList.add(ImmutableAuditEventData.builder()
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
