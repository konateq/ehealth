package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.BalpProfileEnum;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.ImmutableAuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.context.FhirSupportedResourceType;
import org.apache.commons.lang3.BooleanUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Component
public class DocumentReferenceAuditEventProducer extends AbstractAuditEventProducer implements AuditEventProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentReferenceAuditEventProducer.class);
    public static final Predicate<IBaseResource> RESOURCE_IS_DOCUMENT_REFERENCE = resource -> resource.getIdElement().getResourceType().equalsIgnoreCase(ResourceType.DocumentReference.getPath());

    @Override
    public boolean accepts(final AuditableEvent auditableEvent) {
        final boolean accepts = auditableEvent != null
                && auditableEvent.resourceIsOfType(FhirSupportedResourceType.DOCUMENT_REFERENCE);
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

        auditableEvent.getResource().map(iBaseResource -> (Resource) iBaseResource).ifPresent(resource -> {
            final String bundleId = resource.getId();
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

            if (resource.getResourceType() == ResourceType.Bundle) {
                ((Bundle) resource).getEntry().stream()
                        .map(Bundle.BundleEntryComponent::getResource)
                        .filter(entryResource -> entryResource.getResourceType().equals(ResourceType.DocumentReference))
                        .map(entryResource -> (DocumentReference) entryResource)
                        .map(documentReferenceResource -> {
                            final String resourceId = documentReferenceResource.getId();
                            final AuditEventData.EntityData entityData = AuditEventData.EntityData.ofResource(resourceId);

                            return ImmutableAuditEventData.builder()
                                    .auditResourceType(ResourceType.DocumentReference.name())
                                    .metaData(this.createMetaData(auditableEvent))
                                    .restOperationType(auditableEvent.getEuRequestDetails().getRestOperationType())
                                    .profile(operation)
                                    .fhirServerBase(auditableEvent.getEuRequestDetails().getHapiRequestDetails().getFhirServerBase())
                                    .addAllParticipants(participants)
                                    .subject(AuditEventData.SubjectData.forPatient(
                                            documentReferenceResource.getSubject().getReference()))
                                    .addEntity(entityData)
                                    .build();
                        }).forEach(auditEventDataList::add);
            }
        });

        return auditEventDataList;
    }
}
