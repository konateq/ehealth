package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ObjectFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentificationServiceAuditMessageBuilder extends AbstractAuditMessageBuilder implements AuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationServiceAuditMessageBuilder.class);

    @Override
    public AuditMessage build(final EventLog eventLog) {
        final AuditMessage message;
        // If patient id mapping has occurred (there is a patient source ID), use patient mapping audit scheme
        if (CollectionUtils.isNotEmpty(eventLog.getPS_ParticipantObjectIDs())) {
            message = createAuditTrailForPatientMapping(eventLog);
        } else {
            message = createAuditTrailForHCPAssurance(eventLog);
        }

        addParticipantObject(message, eventLog.getHciIdentifier(), Short.valueOf("2"), Short.valueOf("24"),
                "Patient", "ITI-55", "IHE Transactions", "Patient Number",
                "Cross Gateway Patient Discovery", eventLog.getQueryByParameter(), eventLog.getHciIdentifier());

        return message;
    }

    private AuditMessage createAuditTrailForPatientMapping(final EventLog eventLog) {

        AuditMessage message = null;
        try {
            final ObjectFactory of = new ObjectFactory();
            message = of.createAuditMessage();
            addEventIdentification(message, eventLog.getEventType(), eventLog.getEI_TransactionName(),
                    eventLog.getEI_EventActionCode(), AuditConstant.EVENT_ID_IHE_TRANSACTIONS_CODE_SYSTEM, eventLog.getEI_EventDateTime(),
                    eventLog.getEI_EventOutcomeIndicator(), eventLog.getNcpSide());
            addHumanRequestor(message, eventLog.getHR_UserID(), eventLog.getHR_AlternativeUserID(), eventLog.getHR_RoleID(),
                    true, eventLog.getSourceip());
            addService(message, eventLog.getSC_UserID(), true, AuditConstant.SERVICE_CONSUMER, AuditConstant.CODE_SYSTEM_EHDSI,
                    AuditConstant.SERVICE_CONSUMER_DISPLAY_NAME, eventLog.getSourceip());
            addService(message, eventLog.getSP_UserID(), false, AuditConstant.SERVICE_PROVIDER, AuditConstant.CODE_SYSTEM_EHDSI,
                    AuditConstant.SERVICE_PROVIDER_DISPLAY_NAME, eventLog.getTargetip());
            addService(message, eventLog.getSP_UserID(), false, "MasterPatientIndex", AuditConstant.CODE_SYSTEM_EHDSI,
                    "Master Patient Index", eventLog.getTargetip());
            addAuditSource(message, eventLog.getAS_AuditSourceId());
            for (final String psParticipantObjectID : eventLog.getPS_ParticipantObjectIDs()) {
                addParticipantObject(message, psParticipantObjectID, Short.valueOf("1"), Short.valueOf("1"),
                        "PatientSource", "2", AuditConstant.DICOM, "Patient Number",
                        "Patient Number", eventLog.getQueryByParameter(), eventLog.getHciIdentifier());
            }
            for (final String ptParticipantObjectID : eventLog.getPT_ParticipantObjectIDs()) {
                addParticipantObject(message, ptParticipantObjectID, Short.valueOf("1"), Short.valueOf("1"),
                        "PatientTarget", "2", AuditConstant.DICOM, "Patient Number",
                        "Patient Number", eventLog.getQueryByParameter(), eventLog.getHciIdentifier());
            }
            addError(message, eventLog.getEM_ParticipantObjectID(), eventLog.getEM_ParticipantObjectDetail(), Short.valueOf("2"),
                    Short.valueOf("3"), "9", "errormsg");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(message.toString());
            }
        } catch (final Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return message;
    }


}
