package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.*;
import eu.europa.ec.sante.openncp.core.common.fhir.context.FhirSupportedResourceType;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class MedicalImagingAuditEventProducer extends AbstractAuditEventProducer implements AuditEventProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalImagingAuditEventProducer.class);
    public static final Predicate<IBaseResource> RESOURCE_IS_DOCUMENT_REFERENCE = resource -> resource.getIdElement().getResourceType().equalsIgnoreCase(ResourceType.DocumentReference.getPath());
    public static final Predicate<IBaseResource> RESOURCE_IS_PATIENT = resource -> resource.getIdElement().getResourceType().equalsIgnoreCase(ResourceType.Patient.getPath());
    private final AuditEventBuilder auditEventBuilder;

    public MedicalImagingAuditEventProducer(final AuditEventBuilder auditEventBuilder) {
        this.auditEventBuilder = Validate.notNull(auditEventBuilder, "AuditEventBuilder must not be null.");
    }

    @Override
    public boolean accepts(final AuditableEvent auditableEvent) {
        final boolean accepts = auditableEvent != null
                && auditableEvent.getDispatchContext().isMedicalImaging()
                && auditableEvent.resourceIsOfType(FhirSupportedResourceType.BUNDLE, FhirSupportedResourceType.DOCUMENT_REFERENCE);

        LOGGER.debug("[{}] auditable event [{}]", BooleanUtils.toString(accepts, "Accepted", "Rejected"), auditableEvent);
        return accepts;
    }

    @Override
    public List<AuditEvent> produce(final AuditableEvent auditableEvent) {
        final List<AuditEventData> auditEventDataList;
        if (auditableEvent.getDispatchContext().getHapiRequestDetails().isPresent()) {
            switch (auditableEvent.getDispatchContext().getHapiRestOperationType().get()) {
                case SEARCH_TYPE:
                    auditEventDataList = handleSearch(auditableEvent);
                    break;
                case SEARCH_SYSTEM:
                case GET_PAGE:
                case CREATE:
                case READ:
                default:
                    LOGGER.error("Unsupported fhir REST operation type [{}]", auditableEvent.getDispatchContext().getHapiRestOperationType());
                    //TODO what to do here exactly? create a file with the error? we cannot let the audit event create exceptions that will interfere with the response.
                    return Collections.emptyList();
            }
            return auditEventDataList.stream().map(auditEventBuilder::build).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private List<AuditEventData> handleSearch(final AuditableEvent auditableEvent) {
        final List<AuditEventData.ParticipantData> participants = createParticipants();
        return auditableEvent.getResource().map(resource -> {
            final String dataResourceId = auditableEvent.getDispatchContext().createFullyQualifiedResourceReference(resource.getIdElement());

            final Set<String> documentReferenceIds = auditableEvent.extractResourceIds(RESOURCE_IS_DOCUMENT_REFERENCE);

            final List<AuditEventData> auditEventDataList = new ArrayList<>();
            if (documentReferenceIds.isEmpty()) {
                final AuditEventData.EntityData domainResourceEntity = AuditEventData.EntityData.ofResource(dataResourceId);
                auditEventDataList.add(ImmutableAuditEventData.builder()
                        .metaData(createMetaData(auditableEvent))
                        .restOperationType(auditableEvent.getDispatchContext().getHapiRestOperationType().orElse(RestOperationTypeEnum.META))
                        .profile(BalpProfileEnum.BASIC_QUERY)
                        .fhirServerBase(auditableEvent.getDispatchContext().getHapiRequestDetails().map(RequestDetails::getFhirServerBase).orElse(StringUtils.EMPTY))
                        .addAllParticipants(participants)
                        .addEntity(domainResourceEntity)
                        .build());
            } else {
                documentReferenceIds.stream()
                        .map(patientId -> {
                            final AuditEventData.EntityData domainResourceEntityData = AuditEventData.EntityData.ofResource(dataResourceId);
                            final AuditEventData.EntityData patientEntityData = AuditEventData.EntityData.ofPatient(patientId);
                            return ImmutableAuditEventData.builder()
                                    .metaData(createMetaData(auditableEvent))
                                    .restOperationType(auditableEvent.getDispatchContext().getHapiRestOperationType().orElse(RestOperationTypeEnum.META))
                                    .profile(BalpProfileEnum.BASIC_QUERY)
                                    .fhirServerBase(auditableEvent.getDispatchContext().getHapiRequestDetails().map(RequestDetails::getFhirServerBase).orElse(StringUtils.EMPTY))
                                    .addAllParticipants(participants)
                                    .addEntity(domainResourceEntityData)
                                    .addEntity(patientEntityData)
                                    .build();

                        })
                        .forEach(auditEventDataList::add);
            }
            return auditEventDataList;
        }).orElse(Collections.emptyList());
    }
}

