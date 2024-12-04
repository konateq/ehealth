package eu.europa.ec.sante.openncp.core.common.fhir.audit;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import eu.europa.ec.sante.openncp.common.immutables.Domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Domain
public interface AuditEventData {

    MetaData getMetaData();

    RestOperationTypeEnum getRestOperationType();

    BalpProfileEnum getProfile();

    String getFhirServerBase();

    List<ParticipantData> getParticipants();

    List<EntityData> getEntities();

    @Domain
    interface MetaData {
        Instant getRecordDateTime();

        String getCorrelationId();
    }

    @Domain
    interface ParticipantData {
        //AuditEvent.agent.who.identifier
        String getId();

        //AuditEvent.agent.who.display
        Optional<String> getDisplay();

        // [KJW] AuditEvent.agent.role, RoleIDCode in IHE
        String getRoleCode();

        boolean isRequestor();

        Optional<String> getNetwork();
    }

    @Domain
    interface EntityData {
        String getId();

        Optional<String> getDisplay();

        EntityType getType();

        EntityRole getRole();

        static EntityData ofPatient(final String patientId) {
            return ImmutableEntityData.builder()
                    .id(patientId)
                    .type(ImmutableEntityType.of(BalpConstants.CS_AUDIT_ENTITY_TYPE_1_PERSON, Optional.of(BalpConstants.CS_AUDIT_ENTITY_TYPE_1_PERSON_DISPLAY)))
                    .role(ImmutableEntityRole.of(BalpConstants.CS_OBJECT_ROLE_1_PATIENT, Optional.of(BalpConstants.CS_OBJECT_ROLE_1_PATIENT_DISPLAY)))
                    .display("Patient")
                    .build();
        }

        static EntityData ofResource(final String resourceId) {
            return ImmutableEntityData.builder()
                    .id(resourceId)
                    .type(ImmutableEntityType.of(BalpConstants.CS_AUDIT_ENTITY_TYPE_2_SYSTEM_OBJECT, Optional.of(BalpConstants.CS_AUDIT_ENTITY_TYPE_2_SYSTEM_OBJECT_DISPLAY)))
                    .role(ImmutableEntityRole.of(BalpConstants.CS_OBJECT_ROLE_24_QUERY, Optional.of(BalpConstants.CS_OBJECT_ROLE_24_QUERY_DISPLAY)))
                    .build();
        }

        static EntityData ofDocumentReferenceResource(final String resourceId) {
            return ImmutableEntityData.builder()
                    .id(resourceId)
                    .type(ImmutableEntityType.of(BalpConstants.CS_AUDIT_ENTITY_TYPE_2_SYSTEM_OBJECT, Optional.of(BalpConstants.CS_AUDIT_ENTITY_TYPE_2_SYSTEM_OBJECT_DISPLAY)))
                    .role(ImmutableEntityRole.of(BalpConstants.CS_OBJECT_ROLE_4_DOMAIN_RESOURCE, Optional.of(BalpConstants.CS_OBJECT_ROLE_4_DOMAIN_RESOURCE_DISPLAY)))
                    .build();
        }

        @Domain
        interface EntityType {
            String getCode();

            Optional<String> getDisplay();
        }

        @Domain
        interface EntityRole {
            String getCode();

            Optional<String> getDisplay();
        }
    }
}
