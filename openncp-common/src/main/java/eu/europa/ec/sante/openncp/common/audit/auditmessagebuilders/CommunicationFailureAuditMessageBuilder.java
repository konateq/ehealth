package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunicationFailureAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommunicationFailureAuditMessageBuilder.class);

    @Override
    public AuditMessage build(final EventLog eventLog) {

        final ObjectFactory of = new ObjectFactory();
        AuditMessage message = of.createAuditMessage();
        addAuditSource(message, "N/A");
        addEventIdentification(message,
                eventLog.getEventType(),
                eventLog.getEI_EventDateTime(),
                eventLog.getEI_EventOutcomeIndicator());
        addHumanRequestor(message, eventLog.getHR_UserID(), eventLog.getHR_AlternativeUserID(), eventLog.getHR_RoleID(),
                true, eventLog.getSourceip());
        addService(message, eventLog.getSC_UserID(), true, AuditConstant.SERVICE_CONSUMER, AuditConstant.SERVICE_CONSUMER_DISPLAY_NAME, eventLog.getSourceip());
        addService(message, eventLog.getSP_UserID(), false, AuditConstant.SERVICE_PROVIDER, AuditConstant.SERVICE_PROVIDER_DISPLAY_NAME, eventLog.getTargetip());
        for (final String ptParticipantObjectID : eventLog.getPT_ParticipantObjectIDs()) {
            addParticipantObject(message, ptParticipantObjectID, Short.valueOf("1"), Short.valueOf("1"),
                    "Patient", "2", AuditConstant.RFC3881, "Patient Number",
                    "Patient Number", eventLog.getQueryByParameter(), eventLog.getHciIdentifier());
        }
        addError(message, eventLog.getEM_ParticipantObjectID(), eventLog.getEM_ParticipantObjectDetail(), Short.valueOf("2"),
                Short.valueOf("3"), "9", "errormsg", "");
        return message;
    }
}
