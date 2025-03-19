package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventActionCode;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NOKAssertionAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NOKAssertionAuditMessageBuilder.class);

    @Override
    public AuditMessage build(final EventLog eventLog) {
        final AuditMessage message = createBaseAssertionAuditMessage(eventLog);
        for (final String ptParticipantObjectID : eventLog.getPT_ParticipantObjectIDs()) {
            addParticipantObject(message, ptParticipantObjectID, Short.valueOf("1"), Short.valueOf("10"), "Guarantor",
                    "7", AuditConstant.RFC3881, "Guarantor Number",
                    "Patient Number", eventLog.getQueryByParameter(), eventLog.getHciIdentifier());
        }
        addEventTarget(message, eventLog.getEventTargetParticipantObjectIds(), Short.valueOf("2"), null,
                "NokA", AuditConstant.CODE_SYSTEM_EHDSI_SECURITY, "NOK Assertion");
        return message;
    }
}
