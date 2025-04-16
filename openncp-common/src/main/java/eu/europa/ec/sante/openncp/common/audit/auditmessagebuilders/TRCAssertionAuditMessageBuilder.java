package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ObjectFactory;

public class TRCAssertionAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    @Override
    public AuditMessage build(final EventLog eventLog) {
        final AuditMessage message = createBaseAssertionAuditMessage(eventLog);
        for (final String ptParticipantObjectID : eventLog.getPT_ParticipantObjectIDs()) {
            addParticipantObject(message, ptParticipantObjectID, Short.valueOf("1"), Short.valueOf("1"), "PatientSource",
                    "2", AuditConstant.RFC3881, "Patient Number",
                    "Patient Number", eventLog.getQueryByParameter(), eventLog.getHciIdentifier());
        }
        addEventTarget(message, eventLog.getEventTargetParticipantObjectIds(), Short.valueOf("2"), null,
                "TrcA", AuditConstant.CODE_SYSTEM_EHDSI_SECURITY, "TRC Assertion");
        return message;
    }
}
