package eu.europa.ec.sante.openncp.core.common.fhir.audit;

import eu.europa.ec.sante.openncp.common.audit.eventidentification.EventIDBuilder;
import eu.europa.ec.sante.openncp.common.audit.eventidentification.EventIdentificationContentsBuilder;
import eu.europa.ec.sante.openncp.common.audit.eventidentification.EventTypeCodeBuilder;
import net.RFC3881.dicom.*;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.AuditEvent;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeFactory;

@Component
public class AuditMessageAdapter {
    private final DatatypeFactory datatypeFactory;

    public AuditMessageAdapter(final DatatypeFactory datatypeFactory) {
        this.datatypeFactory = Validate.notNull(datatypeFactory, "datatypeFactory must not be null");
    }

    public AuditMessage convertFhirAuditEventToIHEAuditMessage(final AuditEvent fhirAuditEvent) {
        Validate.notNull(fhirAuditEvent, "The fhir auditEvent must not be null");
        final AuditMessage iheAuditMessage = new AuditMessage();


        final EventID eventID = new EventIDBuilder()
                .codeSystemName("FHIR???")
                .csdCode("FHIR")
                .displayName("FHIR")
                .originalText("FHIR")
                .build();

        final  EventTypeCode eventTypeCode = new EventTypeCodeBuilder()
                .codeSystemName(fhirAuditEvent.getType().getCode())
                .csdCode(fhirAuditEvent.getType().getCode())
                .displayName(fhirAuditEvent.getType().getCode())
                .originalText(fhirAuditEvent.getType().getCode())
                .build();


        final EventIdentificationContents eventIdentification = new EventIdentificationContentsBuilder()
                .eventActionCode(fhirAuditEvent.getAction() != null ? convertActionCode(fhirAuditEvent.getAction()): null)
                .eventDateTime(datatypeFactory.newXMLGregorianCalendar(fhirAuditEvent.getRecordedElement().getValueAsCalendar()))
                .eventID(eventID)
                .eventTypeCode(eventTypeCode)
                .build();

        iheAuditMessage.setEventIdentification(eventIdentification);


        if (fhirAuditEvent.getAgent() != null) {
            for (final AuditEvent.AuditEventAgentComponent agent : fhirAuditEvent.getAgent()) {
                final ActiveParticipantContents activeParticipant = createActiveParticipantContents(agent);
                iheAuditMessage.getActiveParticipant().add(activeParticipant);
            }
        }

        if (fhirAuditEvent.getSource() != null) {
            final AuditSourceIdentificationContents auditSource = new AuditSourceIdentificationContents();
            if (fhirAuditEvent.getSource().getObserver() != null) {
                auditSource.setAuditSourceID(fhirAuditEvent.getSource().getObserver().getReference());
            }
            auditSource.setAuditEnterpriseSiteID(fhirAuditEvent.getSource().getSite());
            iheAuditMessage.setAuditSourceIdentification(auditSource);
        }

        if (fhirAuditEvent.getEntity() != null) {
            for (final AuditEvent.AuditEventEntityComponent entity : fhirAuditEvent.getEntity()) {
                final ParticipantObjectIdentificationContents participantObjectIdentificationContents = createParticipantObjectIdentificationContents(entity);
                iheAuditMessage.getParticipantObjectIdentification().add(participantObjectIdentificationContents);
            }
        }

        return iheAuditMessage;
    }

    private ParticipantObjectIdentificationContents createParticipantObjectIdentificationContents(final AuditEvent.AuditEventEntityComponent entity) {
        final ParticipantObjectIdentificationContents participantObject = new ParticipantObjectIdentificationContents();
        if (entity.getWhat() != null) {
            participantObject.setParticipantObjectID(entity.getWhat().getReference());
        }
        if (entity.getType() != null) {
            participantObject.setParticipantObjectTypeCode(entity.getType().getCode());
        }
        if (entity.getRole() != null) {
            participantObject.setParticipantObjectTypeCodeRole(entity.getRole().getCode());
        }
        if (entity.getLifecycle() != null) {
            participantObject.setParticipantObjectDataLifeCycle(entity.getLifecycle().getCode());
        }
        return participantObject;
    }

    private ActiveParticipantContents createActiveParticipantContents(final AuditEvent.AuditEventAgentComponent agent) {
        final ActiveParticipantContents activeParticipant = new ActiveParticipantContents();
        if (agent.getWho() != null) {
            activeParticipant.setUserID(agent.getWho().getReference());
        }
        activeParticipant.setAlternativeUserID(agent.getAltId());
        activeParticipant.setUserName(agent.getName());
        if (agent.getNetwork() != null) {
            activeParticipant.setNetworkAccessPointID(agent.getNetwork().getAddress());
        }
        return activeParticipant;
    }

    private String convertActionCode(final AuditEvent.AuditEventAction action) {
        switch (action) {
            case C:
                return "C";
            case R:
                return "R";
            case U:
                return "U";
            case D:
                return "D";
            case E:
                return "E";
            default:
                return null;
        }
    }
}
