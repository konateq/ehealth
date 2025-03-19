package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventActionCode;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HCPAuthenticationAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HCPAuthenticationAuditMessageBuilder.class);

    @Override
    public AuditMessage build(final EventLog eventLog) {
        final AuditMessage message = createBaseAssertionAuditMessage(eventLog);
        // Event Target
        addEventTarget(message, eventLog.getEventTargetParticipantObjectIds(), Short.valueOf("2"), null,
                "IdA", AuditConstant.CODE_SYSTEM_EHDSI_SECURITY, "HCP Identity Assertion");
        return message;
    }
}
