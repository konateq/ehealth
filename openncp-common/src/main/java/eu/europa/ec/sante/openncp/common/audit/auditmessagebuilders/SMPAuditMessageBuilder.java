package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMPAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SMPAuditMessageBuilder.class);

    @Override
    public AuditMessage build(final EventLog eventLog) {
        final ObjectFactory of = new ObjectFactory();
        AuditMessage message = of.createAuditMessage();
        addEventIdentification(message, eventLog.getEventType(),
                 eventLog.getEI_EventDateTime(),
                eventLog.getEI_EventOutcomeIndicator());
        addService(message, eventLog.getSC_UserID(), true, AuditConstant.SERVICE_CONSUMER,
                AuditConstant.SERVICE_CONSUMER_DISPLAY_NAME, eventLog.getSourceip());
        addService(message, eventLog.getSP_UserID(), false, AuditConstant.SERVICE_PROVIDER,
                AuditConstant.SERVICE_PROVIDER_DISPLAY_NAME, eventLog.getTargetip());
        addAuditSource(message, eventLog.getAS_AuditSourceId());
        addError(message, eventLog.getEM_ParticipantObjectID(), eventLog.getEM_ParticipantObjectDetail(), Short.valueOf("2"),
                Short.valueOf("3"), "9", "errormsg", "PatientSource");
        addEventTarget(message, eventLog.getEventTargetParticipantObjectIds(), Short.valueOf("2"), null,
                "SMP", AuditConstant.CODE_SYSTEM_EHDSI_SECURITY, "SignedServiceMetadata");
        return message;
    }
}
