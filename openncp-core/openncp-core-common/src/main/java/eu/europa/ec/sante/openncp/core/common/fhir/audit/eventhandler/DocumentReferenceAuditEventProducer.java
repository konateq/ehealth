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
public class DocumentReferenceAuditEventProducer extends AbstractAuditEventProducer implements AuditEventProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentReferenceAuditEventProducer.class);
    public static final Predicate<IBaseResource> RESOURCE_IS_DOCUMENT_REFERENCE = resource -> resource.getIdElement().getResourceType().equalsIgnoreCase(ResourceType.DocumentReference.getPath());
    private final AuditEventBuilder auditEventBuilder;


    public DocumentReferenceAuditEventProducer(final AuditEventBuilder auditEventBuilder) {
        this.auditEventBuilder = Validate.notNull(auditEventBuilder, "AuditEventBuilder must not be null.");
    }


    @Override
    public boolean accepts(final AuditableEvent auditableEvent) {
        final boolean accepts = auditableEvent != null
                && auditableEvent.resourceIsOfType(FhirSupportedResourceType.DOCUMENT_REFERENCE);
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
                auditEventDataList = handleSearch(auditableEvent);
                break;
            case VREAD:
            case CREATE:
                auditEventDataList = handleCreate(auditableEvent);
                break;
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

    private List<AuditEventData> handleSearch(final AuditableEvent auditableEvent) {
        return commonHandler(auditableEvent, BalpProfileEnum.BASIC_QUERY);

    }

    private List<AuditEventData> handleRead(final AuditableEvent auditableEvent) {
        return commonHandler(auditableEvent, BalpProfileEnum.BASIC_READ);
    }


    private List<AuditEventData> handleCreate(final AuditableEvent auditableEvent) {
        return commonHandler(auditableEvent, BalpProfileEnum.BASIC_CREATE);
    }


    private List<AuditEventData> commonHandler(final AuditableEvent auditableEvent, BalpProfileEnum operation) {
        final List<AuditEventData.ParticipantData> participants = createParticipants();

        return auditableEvent.getResource().map(resource -> {
            final String dataResourceId = auditableEvent.getEuRequestDetails().createFullyQualifiedResourceReference(resource.getIdElement());

            final Set<String> documentReferenceIds = auditableEvent.extractResourceIds(RESOURCE_IS_DOCUMENT_REFERENCE);

            final List<AuditEventData> auditEventDataList = new ArrayList<>();
            if (documentReferenceIds.isEmpty()) {
                final AuditEventData.EntityData domainResourceEntity = AuditEventData.EntityData.ofResource(dataResourceId);
                auditEventDataList.add(ImmutableAuditEventData.builder()
                        .metaData(createMetaData(auditableEvent))
                        .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                        .profile(operation)
                        .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                        .addAllParticipants(participants)
                        .addEntity(domainResourceEntity)
                        .build());
            } else {
                documentReferenceIds.stream()
                        .map(patientId -> {
                            final AuditEventData.EntityData domainResourceEntityData = AuditEventData.EntityData.ofResource(dataResourceId);
                            return ImmutableAuditEventData.builder()
                                    .metaData(createMetaData(auditableEvent))
                                    .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                                    .profile(operation)
                                    .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                                    .addAllParticipants(participants)
                                    .addEntity(domainResourceEntityData)
                                    .build();

                        })
                        .forEach(auditEventDataList::add);
            }
            return auditEventDataList;
        }).orElse(Collections.emptyList());
    }

}
