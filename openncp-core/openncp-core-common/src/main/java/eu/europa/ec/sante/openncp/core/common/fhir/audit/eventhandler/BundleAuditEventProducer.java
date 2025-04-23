package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.BalpProfileEnum;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.ImmutableAuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.context.FhirSupportedResourceType;
import org.apache.commons.lang3.BooleanUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class BundleAuditEventProducer extends AbstractAuditEventProducer implements AuditEventProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleAuditEventProducer.class);
    public static final Predicate<IBaseResource> RESOURCE_IS_BUNDLE = resource -> resource.getIdElement().getResourceType().equalsIgnoreCase(ResourceType.Bundle.getPath());

    @Override
    public boolean accepts(final AuditableEvent auditableEvent) {
        final boolean accepts = auditableEvent != null
                && auditableEvent.resourceIsOfType(FhirSupportedResourceType.BUNDLE);
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
                auditEventDataList = this.handleSearch(auditableEvent);
                break;
            case VREAD:
            case CREATE:
                auditEventDataList = this.handleCreate(auditableEvent);
                break;
            case READ:
                auditEventDataList = this.handleRead(auditableEvent);
                break;
            default:
                LOGGER.error("Unsupported fhir REST operation type [{}]", auditableEvent.getEuRequestDetails().getRestOperationType());
                //TODO what to do here exactly? create a file with the error? we cannot let the audit event create exceptions that will interfere with the response.
                return Collections.emptyList();
        }

        return auditEventDataList;
    }

    private List<AuditEventData> handleSearch(final AuditableEvent auditableEvent) {
        return this.commonHandler(auditableEvent, BalpProfileEnum.BASIC_QUERY);

    }

    private List<AuditEventData> handleRead(final AuditableEvent auditableEvent) {
        return this.commonHandler(auditableEvent, BalpProfileEnum.BASIC_READ);
    }


    private List<AuditEventData> handleCreate(final AuditableEvent auditableEvent) {
        return this.commonHandler(auditableEvent, BalpProfileEnum.BASIC_CREATE);
    }


    private List<AuditEventData> commonHandler(final AuditableEvent auditableEvent, final BalpProfileEnum operation) {
        final List<AuditEventData.ParticipantData> participants = this.createParticipants();
        final List<AuditEventData> auditEventDataList = new ArrayList<>();
        final String bundleId = auditableEvent.getResource().map(resource -> ((Resource) resource).getId()).orElseThrow();
        final AuditEventData.EntityData domainResourceEntity = AuditEventData.EntityData.ofResource(bundleId);
        auditEventDataList.add(ImmutableAuditEventData.builder()
                .auditResourceType(ResourceType.Bundle.name())
                .metaData(this.createMetaData(auditableEvent))
                .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                .profile(operation)
                .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                .addAllParticipants(participants)
                .addEntity(domainResourceEntity)
                .build());

        auditableEvent.getResource().ifPresent(resource -> {
            if (resource instanceof Bundle) {
                final Bundle bundle = (Bundle) resource;
                final List<ImmutableAuditEventData> auditEventData = bundle.getEntry().stream()
                        .map(Bundle.BundleEntryComponent::getResource)
                        .map(entryResource -> {
                            final String resourceId = entryResource.getId();
                            final AuditEventData.EntityData domainResourceEntityData = AuditEventData.EntityData.ofResource(resourceId);
                            return ImmutableAuditEventData.builder()
                                    .auditResourceType(entryResource.getResourceType().name())
                                    .metaData(this.createMetaData(auditableEvent))
                                    .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                                    .profile(operation)
                                    .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                                    .addAllParticipants(participants)
                                    .addEntity(domainResourceEntityData)
                                    .build();
                        }).collect(Collectors.toList());

                auditEventDataList.addAll(auditEventData);
            }
        });
        return auditEventDataList;
    }
}
