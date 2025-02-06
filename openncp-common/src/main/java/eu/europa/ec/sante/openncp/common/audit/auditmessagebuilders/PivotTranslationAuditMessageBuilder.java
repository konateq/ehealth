package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventActionCode;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class PivotTranslationAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PivotTranslationAuditMessageBuilder.class);
    @Override
    public AuditMessage build(final EventLog eventLog) {
        AuditMessage message = null;
        try {
            final ObjectFactory of = new ObjectFactory();
            message = of.createAuditMessage();
            addAuditSource(message, eventLog.getAS_AuditSourceId());
            addEventIdentification(message, eventLog.getEventType(), eventLog.getEI_TransactionName(), EventActionCode.EXECUTE.getCode(),
                    eventLog.getEI_EventDateTime(), eventLog.getEI_EventOutcomeIndicator(), eventLog.getNcpSide());

            addService(message, eventLog.getSP_UserID(), true, AuditConstant.SERVICE_CONSUMER, AuditConstant.CODE_SYSTEM_EHDSI,
                    AuditConstant.SERVICE_CONSUMER_DISPLAY_NAME, eventLog.getTargetip());
        } catch (final Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        if (message != null) {
            // Event Target
            addEventTarget(message, eventLog.getEventTargetParticipantObjectIds(), Short.valueOf("4"), Short.valueOf("5"),
                    "in", "eHealth DSI Translation", "Input Data");
            addEventTarget(message, Arrays.asList(eventLog.getEventTargetAdditionalObjectId()), Short.valueOf("4"), Short.valueOf("5"),
                    "out", "eHealth DSI Translation", "Output Data");
        }
        return message;
    }
}
