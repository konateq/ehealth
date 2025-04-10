package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NCPTrustedServiceListAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NCPTrustedServiceListAuditMessageBuilder.class);

    @Override
    public AuditMessage build(final EventLog eventLog) {
        final ObjectFactory of = new ObjectFactory();
        AuditMessage message = of.createAuditMessage();
        // Audit Source
        addAuditSource(message, eventLog.getAS_AuditSourceId());
        // Event Identification
        addEventIdentification(message, eventLog.getEventType(),
                 eventLog.getEI_EventDateTime(),
                eventLog.getEI_EventOutcomeIndicator());
        addService(message, eventLog.getSC_UserID(), true, AuditConstant.SERVICE_CONSUMER, AuditConstant.SERVICE_CONSUMER_DISPLAY_NAME, eventLog.getSourceip());
        addService(message, eventLog.getSP_UserID(), false, AuditConstant.SERVICE_PROVIDER, AuditConstant.SERVICE_PROVIDER_DISPLAY_NAME, eventLog.getTargetip());
        addEventTarget(message, eventLog.getEventTargetParticipantObjectIds(), Short.valueOf("2"), null,
                "NSL", AuditConstant.CODE_SYSTEM_EHDSI_SECURITY, "Trusted Service List");
        return message;
    }
}
