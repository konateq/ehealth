package eu.europa.ec.sante.openncp.common.audit.transformer;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.AuditTrailUtils;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.common.audit.EventType;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import net.RFC3881.dicom.AuditMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import java.io.StringWriter;

@Component
public class EventLogToAuditMessageTransformer implements AuditMessageTransformer {
    private final static Logger LOGGER = LoggerFactory.getLogger(EventLogToAuditMessageTransformer.class);
    private JAXBContext jaxbContext;

    @Override
    public boolean accepts(Object message) {
        Validate.notNull(message);
        return message instanceof EventLog;
    }

    @Override
    public AuditMessage transform(final Object message) {
        final EventLog eventlog = (EventLog) message;
        Validate.notNull(message, "Eventlog message cannot be null.");

        LOGGER.debug("createAuditMessage(EventLog '{}')", eventlog.getEventType());
        //TODO: Check if the Audit Message return with a null value shall be considered as fatal?
        final AuditMessage auditMessage = eventlog.getEventType().buildAuditMessage(eventlog);

        //  Non Repudiation information are not relevant for SML/SMP process
        if (eventlog.getEventType() != EventType.SMP_QUERY
                && eventlog.getEventType() != EventType.SMP_PUSH) {

            AuditTrailUtils.getInstance().addNonRepudiationSection(auditMessage, eventlog.getReqM_ParticipantObjectID(),
                    eventlog.getReqM_ParticipantObjectDetail(), eventlog.getResM_ParticipantObjectID(),
                    eventlog.getResM_ParticipantObjectDetail());
        }

        //TODO: Check if the Audit Message return with a null value shall be considered as fatal?
        /* Invoke audit message validation services */
        if (GazelleValidation.isValidationEnable()) {
            if (auditMessage == null) {
                LOGGER.error("Validation of the Audit Message cannot proceed on a Null value!!!");
            } else {
                validateAuditMessage(eventlog, auditMessage);
            }
        }
        return auditMessage;
    }

    private void validateAuditMessage(final EventLog eventLog, final AuditMessage auditMessage) {

        LOGGER.debug("validateAuditMessage(EventLog '{}', AuditMessage '{}', PC UserId: '{}')", eventLog.getEventType(),
                auditMessage.getEventIdentification() != null ? auditMessage.getEventIdentification().getEventActionCode() : "N/A",
                eventLog.getPC_UserID());
        try {
            // Infer model according to NCP Side and EventCode
            NcpSide ncpSide = eventLog.getNcpSide();

            if (StringUtils.equals(eventLog.getEventType().getCode(), "EHDSI-CF")) {
                throw new UnsupportedOperationException("EventCode not supported.");
            }
            GazelleValidation.validateAuditMessage(convertAuditObjectToXML(auditMessage), eventLog.getEventType().getCode(), ncpSide);
        } catch (JAXBException e) {
            LOGGER.error("JAXBException: {}", e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("General exception: {}", e.getMessage(), e);
        }
    }

    private String convertAuditObjectToXML(final AuditMessage auditMessage) throws JAXBException {

        LOGGER.debug("Converting message - JAXB marshalling the Audit Object");
        StringWriter sw = new StringWriter();

        Marshaller marshaller = getJaxbContext().createMarshaller();
        try {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (PropertyException e) {
            LOGGER.error("Unable to format converted AuditMessage to XML: '{}'", e.getMessage(), e);
        }
        marshaller.marshal(auditMessage, sw);
        LOGGER.debug("Audit Messaged converted in XML stream");
        return sw.toString();
    }

    private JAXBContext getJaxbContext() {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(AuditMessage.class);
            } catch (JAXBException e) {
                throw new IllegalStateException(e);
            }
        }

        return jaxbContext;
    }
}
