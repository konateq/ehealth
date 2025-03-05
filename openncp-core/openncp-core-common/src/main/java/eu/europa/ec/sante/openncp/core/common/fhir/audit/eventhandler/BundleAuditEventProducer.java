package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventBuilder;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.BalpProfileEnum;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.ImmutableAuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.context.FhirSupportedResourceType;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class BundleAuditEventProducer extends AbstractAuditEventProducer implements AuditEventProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleAuditEventProducer.class);
    public static final Predicate<IBaseResource> RESOURCE_IS_BUNDLE = resource -> resource.getIdElement().getResourceType().equalsIgnoreCase(ResourceType.Bundle.getPath());
    private final AuditEventBuilder auditEventBuilder;


    public BundleAuditEventProducer(final AuditEventBuilder auditEventBuilder) {
        this.auditEventBuilder = Validate.notNull(auditEventBuilder, "AuditEventBuilder must not be null.");
    }


    @Override
    public boolean accepts(final AuditableEvent auditableEvent) {
        final boolean accepts = auditableEvent != null
                && auditableEvent.resourceIsOfType(FhirSupportedResourceType.BUNDLE);
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


    private List<AuditEventData> commonHandler(final AuditableEvent auditableEvent, final BalpProfileEnum operation) {
        final List<AuditEventData.ParticipantData> participants = createParticipants();
        final String bundleId = auditableEvent.getResource().map(resource -> resource.getIdElement().getValue()).orElseThrow();
        final List<AuditEventData.EntityData> patientEntities = ((Bundle) auditableEvent.getResource().get()).getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Patient)
                .map(resource -> (Patient) resource)
                .flatMap(patient -> patient.getIdentifier().stream())
                .map(identifier -> identifier.getSystem() + "|" + identifier.getValue())
                .map(AuditEventData.EntityData::ofPatient)
                .collect(Collectors.toList());

        final AuditEventData.EntityData domainResourceEntityData = AuditEventData.EntityData.ofResource(bundleId);

        return List.of(ImmutableAuditEventData.builder()
                .metaData(createMetaData(auditableEvent))
                .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                .profile(operation)
                .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                .addAllParticipants(participants)
                .addEntity(domainResourceEntityData)
                .addAllEntities(patientEntities)
                .build());
    }
}
