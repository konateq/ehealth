package epsos.ccd.gnomon.auditmanager.auditmessagebuilders;

import epsos.ccd.gnomon.auditmanager.EventActionCode;
import epsos.ccd.gnomon.auditmanager.EventLog;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditConstant;
import net.RFC3881.AuditMessage;
import net.RFC3881.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NOKAssertionAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NOKAssertionAuditMessageBuilder.class);

    @Override
    public AuditMessage build(final EventLog eventLog) {
        AuditMessage message = null;
        try {
            final ObjectFactory of = new ObjectFactory();
            message = of.createAuditMessage();
            // Audit Source
            addAuditSource(message, eventLog.getAS_AuditSourceId());
            // Event Identification
            addEventIdentification(message, eventLog.getEventType(), eventLog.getEI_TransactionName(), EventActionCode.EXECUTE.getCode(),
                    eventLog.getEI_EventDateTime(), eventLog.getEI_EventOutcomeIndicator(), eventLog.getNcpSide());
            // Point Of Care
            addPointOfCare(message, eventLog.getPC_UserID(), eventLog.getPC_RoleID(), true,
                    "1.3.6.1.4.1.12559.11.10.1.3.2.2.2", eventLog.getSourceip());
            // Human Requestor
            addHumanRequestor(message, eventLog.getHR_UserID(), eventLog.getHR_AlternativeUserID(), eventLog.getHR_RoleID(),
                    true, eventLog.getSourceip());
            addService(message, eventLog.getSC_UserID(), true, AuditConstant.SERVICE_CONSUMER, AuditConstant.CODE_SYSTEM_EHDSI, AuditConstant.SERVICE_CONSUMER_DISPLAY_NAME,
                    eventLog.getSourceip());
            addService(message, eventLog.getSP_UserID(), false, AuditConstant.SERVICE_PROVIDER, AuditConstant.CODE_SYSTEM_EHDSI, AuditConstant.SERVICE_PROVIDER_DISPLAY_NAME,
                    eventLog.getTargetip());
            for (final String ptParticipantObjectID : eventLog.getPT_ParticipantObjectIDs()) {
                addParticipantObject(message, ptParticipantObjectID, Short.valueOf("1"), Short.valueOf("10"), "Guarantor",
                        "7", AuditConstant.RFC3881, "Guarantor Number",
                        "Patient Number", eventLog.getQueryByParameter(), eventLog.getHciIdentifier());
            }
        } catch (final Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        if (message != null) {
            addEventTarget(message, eventLog.getEventTargetParticipantObjectIds(), Short.valueOf("2"), null,
                    "NokA", AuditConstant.CODE_SYSTEM_EHDSI_SECURITY, "NOK Assertion");
        }
        return message;
    }
}
