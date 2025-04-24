package eu.europa.ec.sante.openncp.core.common.fhir.audit;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import org.hl7.fhir.r4.model.ResourceType;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Domain
public interface AuditEventData {
    String getAuditResourceType();

    MetaData getMetaData();

    RestOperationTypeEnum getRestOperationType();

    BalpProfileEnum getProfile();

    String getFhirServerBase();

    List<ParticipantData> getParticipants();

    List<EntityData> getEntities();

    Optional<SubjectData> getSubject();

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
    interface SubjectData {
        String getId();

        Optional<String> getType();

        List<String> getRoleCode();

        @Value.Default
        default boolean isRequestor() {
            return false;
        }

        static SubjectData forPatient(String reference) {
            return ImmutableSubjectData.builder()
                    .id(reference)
                    .type("PAT")
                    .addRoleCode("PAT")
                    .build();
        }
    }


    @Domain
    interface EntityData {
        Optional<Identifier> getIdentifier();

        Optional<String> getReference();

        Optional<String> getDisplay();

        static EntityData ofPatient(final String patientReference, final Identifier patientIdentifier) {
            return ImmutableEntityData.builder()
                    .identifier(patientIdentifier)
                    .reference(patientReference)
                    .display(ResourceType.Patient.name())
                    .build();
        }

        static EntityData ofResource(final String resourceReference, final Identifier resourceIdentifier) {
            return ImmutableEntityData.builder()
                    .identifier(resourceIdentifier)
                    .reference(resourceReference)
                    .build();
        }

        static EntityData ofResource(final String resourceReference) {
            return ImmutableEntityData.builder()
                    .reference(resourceReference)
                    .build();
        }

        @Domain
        interface Identifier {
            String getSystem();

            String getValue();
        }
    }
}
