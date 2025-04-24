package eu.europa.ec.sante.openncp.core.common.fhir.audit;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.codesystems.V3RoleClass;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.springframework.stereotype.Component;

@Component
public class DefaultAuditEventBuilder implements AuditEventBuilder {

    private static final String AUDIT_EVENT_CANONICAL_URL = "http://hl7.org/fhir/StructureDefinition/AuditEvent";

    @Override
    public AuditEvent build(final AuditEventData auditEventData) {
        Validate.notNull(auditEventData, "Audit event data must not be null.");

        // This is based on the code at https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-storage/src/main/java/ca/uhn/fhir/storage/interceptor/balp/BalpAuditCaptureInterceptor.java#L173
        RestOperationTypeEnum restOperationType = auditEventData.getRestOperationType();
        if (restOperationType == RestOperationTypeEnum.GET_PAGE) {
            restOperationType = RestOperationTypeEnum.SEARCH_TYPE;
        }

        final AuditEvent auditEvent = new AuditEvent();
        final BalpProfileEnum eventProfile = auditEventData.getProfile();
        auditEvent.getMeta().addProfile(AUDIT_EVENT_CANONICAL_URL);
        auditEvent
                .getText()
                .setDiv(new XhtmlNode().setValue("<div>Audit Event</div>"))
                .setStatus(org.hl7.fhir.r4.model.Narrative.NarrativeStatus.GENERATED);
        auditEvent
                .getType()
                .setSystem(BalpConstants.CS_AUDIT_EVENT_TYPE)
                .setCode("rest")
                .setDisplay("Restful Operation");
        auditEvent
                .addSubtype()
                .setSystem(BalpConstants.CS_RESTFUL_INTERACTION)
                .setCode(restOperationType.getCode())
                .setDisplay(restOperationType.getCode());
        auditEvent.setAction(eventProfile.getAction());
        auditEvent.setOutcome(AuditEvent.AuditEventOutcome._0);
        auditEvent.getRecordedElement().setValueAsString(auditEventData.getMetaData().getRecordDateTime().toString());

        auditEvent.getSource().getObserver().setDisplay(auditEventData.getFhirServerBase());

        auditEventData.getParticipants().forEach(participantData -> {
            final AuditEvent.AuditEventAgentComponent agent = auditEvent.addAgent();
            final Coding agentTypeCode;
            if (participantData.isRequestor()) {
                agentTypeCode = eventProfile.getAgentClientTypeCoding();
            } else {
                agentTypeCode = eventProfile.getAgentServerTypeCoding();
            }
            agent.getType().addCoding(agentTypeCode);
            participantData.getDisplay().ifPresent(participantDisplay -> agent.getWho().setDisplay(participantDisplay));
            agent.getWho().getIdentifier().setValue(participantData.getId());
            agent.setRequestor(participantData.isRequestor());
            participantData.getNetwork().ifPresent(participantNetwork -> agent.getNetwork()
                    .setAddress(participantNetwork)
                    .setType(BalpConstants.AUDIT_EVENT_AGENT_NETWORK_TYPE_IP_ADDRESS));
        });

        auditEventData.getSubject().ifPresent(subjectData -> {
            final AuditEvent.AuditEventAgentComponent userAgent = auditEvent.addAgent();
            subjectData.getType()
                    .map(V3RoleClass::fromCode)
                    .ifPresent(roleClass ->
                            userAgent
                            .getType()
                            .addCoding()
                            .setSystem(roleClass.getSystem())
                            .setCode(roleClass.toCode()));

            userAgent.setWho(new Reference(subjectData.getId()));
            userAgent.setRequestor(subjectData.isRequestor());
        });

        final AuditEvent.AuditEventEntityComponent entityCorrelationId = auditEvent.addEntity();
        entityCorrelationId.setName("X-Correlation-ID");
        entityCorrelationId.getWhat().getIdentifier().setValue(auditEventData.getMetaData().getCorrelationId());

        auditEventData.getEntities().forEach(entityData -> {
            final AuditEvent.AuditEventEntityComponent entity = auditEvent.addEntity();
            entity.getWhat().setReference(entityData.getReference().orElse(StringUtils.EMPTY));
            entityData.getIdentifier().ifPresent(identifier -> entity.getWhat().setIdentifier(new Identifier()
                    .setSystem(identifier.getSystem())
                    .setValue(identifier.getValue())));
            entityData.getDisplay().ifPresent(entityDisplay -> entity.getWhat().setDisplay(entityDisplay));
        });

        return auditEvent;
    }
}
