package eu.europa.ec.sante.openncp.common.audit;

import eu.europa.ec.sante.openncp.common.audit.serialization.AuditLogSerializer;
import eu.europa.ec.sante.openncp.common.audit.utils.SecurityMgr;
import eu.europa.ec.sante.openncp.common.audit.utils.Utils;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerFactory;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ParticipantObjectDetail;
import net.RFC3881.dicom.ParticipantObjectIDTypeCode;
import net.RFC3881.dicom.ParticipantObjectIdentificationContents;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.bind.*;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class provides methods for constructing the audit message and for sending the syslog message to the repository.
 * Instances of the spSOS defined services, as for constructing and sending the syslog message to the repository.
 *
 * @author Kostas Karkaletsis
 */
public enum AuditTrailUtils {

    INSTANCE;
    private static final JAXBContext jaxbContext;
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditTrailUtils.class);
    private static final String EVENT_TYPE_CODE_N_A = "EventTypeCode(N/A)";

    static {
        try {
            jaxbContext = JAXBContext.newInstance(AuditMessage.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return
     */
    public static AuditTrailUtils getInstance() {
        return INSTANCE;
    }

    /**
     * @param auditmessage
     * @param sign
     * @return
     */
    public static synchronized String constructMessage(AuditMessage auditmessage, boolean sign) {

        String auditMessage = "";
        String eventTypeCode = EVENT_TYPE_CODE_N_A;
        try {
            eventTypeCode = auditmessage.getEventIdentification() != null ?
                    auditmessage.getEventIdentification().getEventTypeCode().get(0).getCsdCode() : EVENT_TYPE_CODE_N_A;
            LOGGER.debug("'{}' try to convert the message to xml using JAXB", eventTypeCode);
        } catch (NullPointerException e) {
            LOGGER.warn("Unable to log AuditMessageEventTypeCode.", e);
        }

        try {
            auditMessage = AuditTrailUtils.convertAuditObjectToXML(auditmessage);
        } catch (JAXBException e) {
            LOGGER.error(e.getMessage(), e);
        }
        INSTANCE.writeTestAudits(auditmessage, auditMessage);
        LOGGER.info("Message constructed: '{}'", eventTypeCode);

        boolean validated = false;

        try {
            LOGGER.debug("'{}' Validating Schema", auditmessage.getEventIdentification() != null ?
                    auditmessage.getEventIdentification().getEventID().getCsdCode() : EVENT_TYPE_CODE_N_A);
            validated = Utils.validateSchema(auditMessage);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        boolean forceWrite = Boolean.parseBoolean(Utils.getProperty("auditrep.forcewrite", "true", true));
        if (!validated) {
            LOGGER.debug("'{}' Message not validated", auditmessage.getEventIdentification() != null ?
                    auditmessage.getEventIdentification().getEventID().getCsdCode() : EVENT_TYPE_CODE_N_A);
            //auditmessage.getEventIdentification().getEventID().getCode() : "EventTypeCode(N/A)");
            if (!forceWrite) {
                auditMessage = "";
            }
        }
        if (validated || forceWrite) {

            if (validated) {
                LOGGER.debug("'{}' Audit Message validated", auditmessage.getEventIdentification() != null ?
                        auditmessage.getEventIdentification().getEventID().getCsdCode() : EVENT_TYPE_CODE_N_A);
                //auditmessage.getEventIdentification().getEventID().getCode() : "EventTypeCode(N/A)");
            } else {
                LOGGER.debug("'{}' Audit Message not validated", auditmessage.getEventIdentification() != null ?
                        auditmessage.getEventIdentification().getEventID().getCsdCode() : EVENT_TYPE_CODE_N_A);
                //auditmessage.getEventIdentification().getEventID().getCode() : "EventTypeCode(N/A)");
            }

            if (forceWrite && !validated) {
                LOGGER.debug("'{}' AuditManager is force to send the message. So trying ...",
                        auditmessage.getEventIdentification() != null ?
                                auditmessage.getEventIdentification().getEventID().getCsdCode() : EVENT_TYPE_CODE_N_A);
                //auditmessage.getEventIdentification().getEventID().getCode() : "EventTypeCode(N/A)");
            }

            try {
                // Validating XML according to XSD
                LOGGER.debug("'{}' XML stuff: Create Dom From String",
                        auditmessage.getEventIdentification() != null ?
                                auditmessage.getEventIdentification().getEventID().getCsdCode() : EVENT_TYPE_CODE_N_A);
                //auditmessage.getEventIdentification().getEventID().getCode() : "EventTypeCode(N/A)");
                Document doc = Utils.createDomFromString(auditMessage);
                if (sign) {

                    auditMessage = SecurityMgr.getSignedDocumentAsString(SecurityMgr.signDocumentEnveloped(doc));
                    LOGGER.debug("'{}' Audit Message signed",
                            auditmessage.getEventIdentification() != null ?
                                    auditmessage.getEventIdentification().getEventID().getCsdCode() : EVENT_TYPE_CODE_N_A);
                    //auditmessage.getEventIdentification().getEventID().getCode() : "EventTypeCode(N/A)");
                }
            } catch (Exception e) {
                auditMessage = "";
                LOGGER.error("'{}' Error signing doc: '{}'",
                        auditmessage.getEventIdentification() != null ?
                                auditmessage.getEventIdentification().getEventID().getCsdCode() : EVENT_TYPE_CODE_N_A,
                        //auditmessage.getEventIdentification().getEventID().getCode() : "EventTypeCode(N/A)",
                        e.getMessage(), e);
            }
        }
        return auditMessage;
    }

    /**
     * The method converts the audit message to xml format, having as input the Audit Message.
     * Uses the JAXB library to marshal the audit message object.
     *
     * @param auditMessage
     * @return
     * @throws JAXBException
     */
    public static synchronized String convertAuditObjectToXML(AuditMessage auditMessage) throws JAXBException {

        LOGGER.debug("Converting message - JAXB marshalling the Audit Object");
        StringWriter sw = new StringWriter();

        Marshaller marshaller = jaxbContext.createMarshaller();
        try {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (PropertyException e) {
            LOGGER.error("Unable to format converted AuditMessage to XML: '{}'", e.getMessage(), e);
        }
        marshaller.marshal(auditMessage, sw);
        LOGGER.debug("Audit Messaged converted in XML stream");
        return sw.toString();
    }

    public static synchronized AuditMessage convertXMLToAuditObject(InputStream xml) throws JAXBException {
        LOGGER.debug("Converting message - JAXB unmarshalling the Audit Object");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        AuditMessage auditMessage = (AuditMessage) unmarshaller.unmarshal(xml);
        LOGGER.debug("XML converted in Audit Messaged");
        return auditMessage;
    }

    /**
     * Method to create the audit message to be passed to the syslog client
     *
     * @param eventLog
     * @return the Audit Message object
     */
    public AuditMessage createAuditMessage(EventLog eventLog) {

        LOGGER.debug("createAuditMessage(EventLog '{}')", eventLog.getEventType());
        // Check if the Audit Message return with a null value shall be considered as fatal?
        AuditMessage message = eventLog.getEventType().buildAuditMessage(eventLog);

        //  Non Repudiation information are not relevant for SML/SMP process
        if (eventLog.getEventType() !=  EventType.SMP_QUERY
                && eventLog.getEventType() != EventType.SMP_PUSH) {

            AuditTrailUtils.getInstance().addNonRepudiationSection(message, eventLog.getReqM_ParticipantObjectID(),
                    eventLog.getReqM_ParticipantObjectDetail(), eventLog.getResM_ParticipantObjectID(),
                    eventLog.getResM_ParticipantObjectDetail());
        }

        // Check if the Audit Message return with a null value shall be considered as fatal?
        // Invoke audit message validation services
        if (GazelleValidation.isValidationEnable()) {
            if (message == null) {
                LOGGER.error("Validation of the Audit Message cannot proceed on a Null value!!!");
            } else {
                validateAuditMessage(eventLog, message);
            }
        }
        return message;
    }

    /**
     * @param auditMessage
     * @param participantIdRequest
     * @param participantDetailRequest
     * @param participantIdResponse
     * @param participantDetailResponse
     */
    public void addNonRepudiationSection(AuditMessage auditMessage, String participantIdRequest, byte[] participantDetailRequest,
                                         String participantIdResponse, byte[] participantDetailResponse) {

        // Based on the current guidelines and functional specifications, this is not clear enough if an evidence
        // has to be generated including the Non-Repudiation section (Type Value pair attributes - security header)
        // while the Audit Message has been considering NCP internal actions.

        // Request Participant
        ParticipantObjectIdentificationContents participantRequest = createParticipantObjectIdentification("req",
                participantIdRequest, participantDetailRequest);
        auditMessage.getParticipantObjectIdentification().add(participantRequest);

        // Response Participant
        ParticipantObjectIdentificationContents participantResponse = createParticipantObjectIdentification("rsp",
                participantIdResponse, participantDetailResponse);

        auditMessage.getParticipantObjectIdentification().add(participantResponse);
    }

    /**
     * @param action
     * @param participantObjectId
     * @param participantObjectDetail
     * @return
     */
    private ParticipantObjectIdentificationContents createParticipantObjectIdentification(String action, String participantObjectId,
                                                                                          byte[] participantObjectDetail) {

        ParticipantObjectIdentificationContents participantObjectIdentification = new ParticipantObjectIdentificationContents();
        participantObjectIdentification.setParticipantObjectID(participantObjectId);
        participantObjectIdentification.setParticipantObjectTypeCode("4");

        ParticipantObjectIDTypeCode codedValueType = new ParticipantObjectIDTypeCode();
        codedValueType.setCsdCode(action);
        codedValueType.setCodeSystemName("eHealth DSI Msg");
        codedValueType.setOriginalText(action);
        if (StringUtils.equals("rsp", action)) {
            codedValueType.setDisplayName("Response Message");
        } else {
            codedValueType.setDisplayName("Request Message");
        }

        participantObjectIdentification.setParticipantObjectIDTypeCode(codedValueType);

        if (ArrayUtils.isNotEmpty(participantObjectDetail)) {
            ParticipantObjectDetail detail = new ParticipantObjectDetail();
            detail.setType("securityheader");
            detail.setValue(participantObjectDetail);
            participantObjectIdentification.getParticipantObjectDetail().add(detail);
        }
        return participantObjectIdentification;
    }

    /**
     * @param auditmessage
     * @param auditmsg
     */
    private void writeTestAudits(AuditMessage auditmessage, String auditmsg) {

        String wta = ConfigurationManagerFactory.getConfigurationManager().getProperty("WRITE_TEST_AUDITS");
        LOGGER.debug("Writing test audits: '{}'", wta);
        if (StringUtils.equals(wta, "true")) {

            String tap = Utils.getProperty("TEST_AUDITS_PATH");
            try {
                if (auditmessage.getEventIdentification() != null && auditmessage.getEventIdentification().getEventTypeCode().get(0).getDisplayName() != null) {
                    Utils.writeXMLToFile(auditmsg, tap + (auditmessage.getEventIdentification().getEventTypeCode()
                            .get(0).getDisplayName().split("::"))[0] + "-" +
                            new SimpleDateFormat("yyyy.MM.dd'at'HH-mm-ss.SSS").format(new Date(System.currentTimeMillis())) + ".xml");
                }
            } catch (Exception e) {
                LOGGER.error("Exception: '{}'", e.getMessage(), e);
                try {
                    Utils.writeXMLToFile(auditmsg, tap + new SimpleDateFormat("yyyy.MM.dd'at'HH-mm-ss.SSS")
                            .format(new Date(System.currentTimeMillis())) + "-ERROR.xml");
                } catch (Exception ex) {
                    LOGGER.warn("Unable to write test audit: '{}'", ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * @param auditLogSerializer
     * @param auditMessage
     * @param facility
     * @param severity
     */
    public synchronized void sendATNASyslogMessage(AuditLogSerializer auditLogSerializer, AuditMessage auditMessage,
                                                   String facility, String severity) {

        LOGGER.info("[Audit Util] Starting new thread for sending message");
        MessageSender messageSender = new MessageSender();
        new Thread(() -> messageSender.send(auditLogSerializer, auditMessage, facility, severity)).start();
    }

    /**
     * Based on a given EventLog this method will infer about the correct model to apply for a message validation.
     *
     * @param eventLog an EventLog object containing information about the audit message.
     * @return the Audit Message Model
     */
    private void validateAuditMessage(EventLog eventLog, AuditMessage auditMessage) {

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
}
