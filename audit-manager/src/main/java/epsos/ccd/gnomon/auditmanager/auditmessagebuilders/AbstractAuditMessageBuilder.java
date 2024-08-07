package epsos.ccd.gnomon.auditmanager.auditmessagebuilders;

import epsos.ccd.gnomon.auditmanager.EventLog;
import epsos.ccd.gnomon.auditmanager.EventType;
import epsos.ccd.gnomon.auditmanager.eventidentification.EventIDBuilder;
import epsos.ccd.gnomon.auditmanager.eventidentification.EventIdentificationContentsBuilder;
import epsos.ccd.gnomon.auditmanager.eventidentification.EventTypeCodeBuilder;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditConstant;
import net.RFC3881.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.List;

public abstract class AbstractAuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuditMessageBuilder.class);

    protected AuditMessage addParticipantObject(AuditMessage auditMessage, String participantId, Short participantCode,
                                              Short participantRole, String participantName, String PS_ObjectCode, String PS_ObjectCodeName,
                                              String PS_ObjectCodeValue, String PS_originalText,
                                              String PS_getQueryByParameterPayload, String PS_getHciIdentifierPayload) {

        var participantObjectIdentification = new ParticipantObjectIdentificationContents();
        participantObjectIdentification.setParticipantObjectID(participantId);
        participantObjectIdentification.setParticipantObjectTypeCode(participantCode.toString());
        participantObjectIdentification.setParticipantObjectTypeCodeRole(participantRole.toString());

        ParticipantObjectIDTypeCode codedValue = new ParticipantObjectIDTypeCode();
        codedValue.setCsdCode(PS_ObjectCode);
        codedValue.setCodeSystemName(PS_ObjectCodeName);
        codedValue.setDisplayName(PS_ObjectCodeValue);
        codedValue.setOriginalText(PS_originalText);

        // SystemObject and Query
        if(participantCode == 2 && (participantRole == 3 || participantRole == 24)) {
            ParticipantObjectDetail participantObjectDetail = new ParticipantObjectDetail();
            // 'ihe:homeCommunityID' or 'Repository Unique Id'
            participantObjectDetail.setType("ihe:homeCommunityID");
            try {
                if (StringUtils.isNotBlank(PS_getHciIdentifierPayload)) {
                    participantObjectDetail.setValue(PS_getHciIdentifierPayload.getBytes("UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                participantObjectDetail.setValue(new byte[]{2});
                LOGGER.debug("Error addParticipantObject() - participantObjectDetail: '{}'", e.getMessage());
            }
            try {
                if (StringUtils.isNotBlank(PS_getQueryByParameterPayload)) {
                    participantObjectIdentification.setParticipantObjectQuery(PS_getQueryByParameterPayload.getBytes("UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                participantObjectIdentification.setParticipantObjectQuery(new byte[]{2});
                LOGGER.debug("Error addParticipantObject() - participantObjectIdentification: '{}'", e.getMessage());
            }
            participantObjectIdentification.getParticipantObjectDetail().add(participantObjectDetail);
        }

        participantObjectIdentification.setParticipantObjectIDTypeCode(codedValue);
        auditMessage.getParticipantObjectIdentification().add(participantObjectIdentification);
        return auditMessage;
    }

    protected AuditMessage createAuditTrailForHCPAssurance(EventLog eventLog) {

        AuditMessage message = null;
        try {
            ObjectFactory of = new ObjectFactory();
            message = of.createAuditMessage();
            addEventIdentification(message, eventLog.getEventType(), eventLog.getEI_TransactionName(),
                    eventLog.getEI_EventActionCode(), eventLog.getEI_EventDateTime(),
                    eventLog.getEI_EventOutcomeIndicator(), eventLog.getNcpSide());
            addPointOfCare(message, eventLog.getPC_UserID(), eventLog.getPC_RoleID(), getUserIsRequestor(eventLog),
                    "1.3.6.1.4.1.12559.11.10.1.3.2.2.2");
            addHumanRequestor(message, eventLog.getHR_UserID(), eventLog.getHR_AlternativeUserID(), eventLog.getHR_RoleID(),
                    getUserIsRequestor(eventLog));
            addService(message, eventLog.getSC_UserID(), true, AuditConstant.SERVICE_CONSUMER,
                    AuditConstant.CODE_SYSTEM_EHDSI, "Service Consumer"); // eventLog.getSourceip()
            addService(message, eventLog.getSP_UserID(), false, AuditConstant.SERVICE_PROVIDER,
                    AuditConstant.CODE_SYSTEM_EHDSI, "Service Provider"); // eventLog.getTargetip()
            addAuditSource(message, eventLog.getAS_AuditSourceId());
            addParticipantObject(message, eventLog.getPT_ParticipantObjectID(), Short.valueOf("1"), Short.valueOf("1"),
                    "Patient", "2", AuditConstant.DICOM, "Patient Number",
                    "Cross Gateway Patient Discovery", eventLog.getQueryByParameter(), eventLog.getHciIdentifier());
            addError(message, eventLog.getEM_ParticipantObjectID(), eventLog.getEM_ParticipantObjectDetail(), Short.valueOf("2"),
                    Short.valueOf("3"), "9", "errormsg");
        } catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return message;
    }

    protected boolean getUserIsRequestor(final EventLog eventLog) {
        if (eventLog.getEventType().equals(EventType.DISPENSATION_SERVICE_INITIALIZE) || eventLog.getEventType().equals(EventType.DISPENSATION_SERVICE_DISCARD)) {
            return eventLog.getNcpSide().equals(NcpSide.NCP_B) ? false : true;
        } else {
            return eventLog.getNcpSide().equals(NcpSide.NCP_B) ? true : false;
        }
    }

    /**
     * @param auditMessage
     * @param userId
     * @param userIsRequester
     * @param code
     * @param codeSystem
     * @param displayName
     * @return
     */
    protected AuditMessage addService(AuditMessage auditMessage, String userId, boolean userIsRequester, String code,
                                    String codeSystem, String displayName) {

        InetAddressValidator validator = InetAddressValidator.getInstance();
        if (StringUtils.isBlank(userId)) {
            LOGGER.warn("No Service, as this is Service Consumer");
            throw new IllegalArgumentException("Both ServiceConsumer User ID and ServiceProvider User ID must exist!");
        } else {
            ActiveParticipantContents activeParticipant = new ActiveParticipantContents();
            activeParticipant.setUserID(userId);
            activeParticipant.setAlternativeUserID(userId);
            activeParticipant.setUserIsRequestor(userIsRequester);

            RoleIDCode serviceConsumerRoleId = new RoleIDCode();
            serviceConsumerRoleId.setCsdCode(code);
            serviceConsumerRoleId.setCodeSystemName(codeSystem);
            serviceConsumerRoleId.setDisplayName(displayName);
            serviceConsumerRoleId.setOriginalText(displayName);
            activeParticipant.getRoleIDCode().add(serviceConsumerRoleId);
            auditMessage.getActiveParticipant().add(activeParticipant);
        }
        return auditMessage;
    }

    /**
     * @param auditMessage
     * @param userId
     * @param userIsRequester
     * @param code
     * @param codeSystem
     * @param displayName
     * @param ipAddress
     * @return
     */
    protected AuditMessage addService(AuditMessage auditMessage, String userId, boolean userIsRequester, String code,
                                    String codeSystem, String displayName, String ipAddress) {

        InetAddressValidator validator = InetAddressValidator.getInstance();
        if (StringUtils.isBlank(userId)) {
            LOGGER.warn("No Service, as this is Service Consumer");
            throw new IllegalArgumentException("Both ServiceConsumer User ID and ServiceProvider User ID must exist!");
        } else {
            ActiveParticipantContents activeParticipant = new ActiveParticipantContents();
            activeParticipant.setNetworkAccessPointID(ipAddress);
            if (validator.isValidInet4Address(ipAddress) || validator.isValidInet6Address(ipAddress)) {
                activeParticipant.setNetworkAccessPointTypeCode("2");
            } else {
                activeParticipant.setNetworkAccessPointTypeCode("1");
            }
            activeParticipant.setUserID(userId);
            activeParticipant.setAlternativeUserID(userId);
            activeParticipant.setUserIsRequestor(userIsRequester);

            RoleIDCode serviceConsumerRoleId = new RoleIDCode();
            serviceConsumerRoleId.setCsdCode(code);
            serviceConsumerRoleId.setCodeSystemName(codeSystem);
            serviceConsumerRoleId.setDisplayName(displayName);
            serviceConsumerRoleId.setOriginalText(displayName);
            activeParticipant.getRoleIDCode().add(serviceConsumerRoleId);
            auditMessage.getActiveParticipant().add(activeParticipant);
        }
        return auditMessage;
    }

    /**
     * @param message
     * @param userId
     * @param roleId
     * @param userIsRequester
     * @param codeSystem
     * @return
     */
    protected AuditMessage addPointOfCare(AuditMessage message, String userId, String roleId, boolean userIsRequester, String codeSystem) {

        if (StringUtils.isBlank(userId)) {
            //LOGGER.debug("This is service provider and doesn't need Point of Care");
            ActiveParticipantContents participant = new ActiveParticipantContents();
            participant.setUserID("SP");
            participant.setAlternativeUserID("SP");
            participant.setNetworkAccessPointID("127.0.0.1");
            participant.setNetworkAccessPointTypeCode("1");
            participant.setUserIsRequestor(userIsRequester);

            RoleIDCode codedValue = new RoleIDCode();
            codedValue.setCsdCode("110152");
            codedValue.setCodeSystemName("DCM");
            codedValue.setOriginalText(codeSystem); // Should be "Destination Role ID"
            participant.getRoleIDCode().add(codedValue);
            message.getActiveParticipant().add(participant);
        } else {//ActiveParticipantType   participant2 = new ActiveParticipantType();//participant2.setNetworkAccessPointID();
            ActiveParticipantContents participant = new ActiveParticipantContents();
            participant.setUserID(userId);
            participant.setAlternativeUserID(userId);
            participant.setNetworkAccessPointID("127.0.0.1");
            participant.setNetworkAccessPointTypeCode("1");
            participant.setUserIsRequestor(userIsRequester);

            RoleIDCode codedValue = new RoleIDCode();
            codedValue.setCsdCode("110152");
            codedValue.setCodeSystemName("DCM");
            codedValue.setOriginalText(codeSystem); // Should be "Destination Role ID"
            participant.getRoleIDCode().add(codedValue);
            message.getActiveParticipant().add(participant);
        }
        return message;
    }

    /**
     * @param auditMessage
     * @param errorMessagePartObjectId
     * @param errorMessagePartObjectDetail
     * @param errorMessageCode
     * @param errorMessageCodeRole
     * @param errorMessageTypeCode
     * @param errorMessageQualifier
     * @return
     */
    AuditMessage addError(AuditMessage auditMessage, String errorMessagePartObjectId, byte[] errorMessagePartObjectDetail,
                          Short errorMessageCode, Short errorMessageCodeRole, String errorMessageTypeCode,
                          String errorMessageQualifier) {

        // Error Message handling for audit purpose
        if (StringUtils.isNotBlank(errorMessagePartObjectId)) {

            LOGGER.debug("Error Message Participant ID is: '{}'", errorMessagePartObjectId);
            ParticipantObjectIDTypeCode codedValueType = new ParticipantObjectIDTypeCode();
            codedValueType.setCsdCode(errorMessageTypeCode);
            codedValueType.setOriginalText("error message");
            codedValueType.setCodeSystemName("eHealth DSI Security");

            ParticipantObjectIdentificationContents participantObjectIdentificationType = new ParticipantObjectIdentificationContents();
            participantObjectIdentificationType.setParticipantObjectID(errorMessagePartObjectId);
            participantObjectIdentificationType.setParticipantObjectTypeCode(errorMessageCode.toString());
            participantObjectIdentificationType.setParticipantObjectTypeCodeRole(errorMessageCodeRole.toString());
            participantObjectIdentificationType.setParticipantObjectIDTypeCode(codedValueType);

            if (errorMessagePartObjectDetail != null) {
                ParticipantObjectDetail typeValuePairType = new ParticipantObjectDetail();
                typeValuePairType.setType(errorMessageQualifier);
                typeValuePairType.setValue(errorMessagePartObjectDetail);
                participantObjectIdentificationType.getParticipantObjectDetail().add(typeValuePairType);
            }
            auditMessage.getParticipantObjectIdentification().add(participantObjectIdentificationType);

        } else {
            LOGGER.debug("No Error Message reported by the auditing process!");
        }
        return auditMessage;
    }

    /**
     * @param auditMessage
     * @param userId
     * @param alternativeUserID
     * @param roleId
     * @param userIsRequester
     * @return
     */
    AuditMessage addHumanRequestor(AuditMessage auditMessage, String userId, String alternativeUserID,
                                   String roleId, boolean userIsRequester) {

        ActiveParticipantContents humanRequester = new ActiveParticipantContents();
        humanRequester.setUserID(userId);
        humanRequester.setAlternativeUserID(alternativeUserID);
        humanRequester.setNetworkAccessPointID("127.0.0.1");
        humanRequester.setNetworkAccessPointTypeCode("2");
        humanRequester.setUserIsRequestor(userIsRequester);

        RoleIDCode humanRequesterRoleId = new RoleIDCode();
        humanRequesterRoleId.setCsdCode("110153");
        humanRequesterRoleId.setOriginalText(roleId);
        humanRequesterRoleId.setCodeSystemName("DCM");
        humanRequesterRoleId.setOriginalText("Source Role ID");

        humanRequester.getRoleIDCode().add(humanRequesterRoleId);
        auditMessage.getActiveParticipant().add(humanRequester);
        return auditMessage;
    }

    /**
     * @param auditMessage
     * @param eventType
     * @param transactionName
     * @param eventActionCode
     * @param eventDateTime
     * @param eventOutcomeIndicator
     * @param ncpSide
     * @return
     */
    AuditMessage addEventIdentification(AuditMessage auditMessage,
                                        EventType eventType,
                                        String transactionName,
                                        String eventActionCode,
                                        XMLGregorianCalendar eventDateTime,
                                        BigInteger eventOutcomeIndicator,
                                        NcpSide ncpSide) {

        final EventID eventID = buildEventID(eventType, ncpSide, transactionName);

        final EventTypeCode eventTypeCode = new EventTypeCodeBuilder()
                .codeSystemName("IHE Transactions")
                .csdCode(eventType.getIheCode())
                .displayName(transactionName)
                .originalText(eventType.getIheTransactionName())
                .build();
        final EventTypeCode eventTypeCode2 = buildEventTypeCode(eventType);

        final EventIdentificationContents eventIdentification = new EventIdentificationContentsBuilder()
                .eventActionCode(eventActionCode)
                .eventDateTime(eventDateTime)
                .eventOutcomeIndicator(eventOutcomeIndicator.toString())
                .eventID(eventID)
                .eventTypeCode(eventTypeCode)
                .eventTypeCode(eventTypeCode2)
                .build();
        auditMessage.setEventIdentification(eventIdentification);
        return auditMessage;
    }

    /**
     * @param auditMessage
     * @param eventTargetObjectId
     * @param typeCode
     * @param typeCodeRole
     * @param errorMessageCode
     * @param action
     * @param objectDataLifeCycle
     * @return
     */
    AuditMessage addEventTarget(AuditMessage auditMessage, List<String> eventTargetObjectId, Short typeCode,
                                        Short typeCodeRole, String errorMessageCode, String action, Short objectDataLifeCycle,
                                        String EM_CodeSystemName, String EM_DisplayName) {

        LOGGER.debug("AuditMessage addEventTarget('{}','{}','{}','{}','{}','{}','{}')", auditMessage, eventTargetObjectId,
                typeCode, typeCodeRole, errorMessageCode, action, objectDataLifeCycle);
        for (String eventTargetId : eventTargetObjectId) {

            ParticipantObjectIdentificationContents em = new ParticipantObjectIdentificationContents();
            em.setParticipantObjectID(eventTargetId);
            em.setParticipantObjectTypeCode(typeCode.toString());
            em.setParticipantObjectTypeCodeRole(typeCodeRole.toString());
            ParticipantObjectIDTypeCode errorMessageCodedValueType = new ParticipantObjectIDTypeCode();
            errorMessageCodedValueType.setCsdCode(errorMessageCode);
            errorMessageCodedValueType.setCodeSystemName(EM_CodeSystemName);
            errorMessageCodedValueType.setOriginalText(EM_DisplayName);
            errorMessageCodedValueType.setDisplayName(EM_DisplayName);
            if (action.equals(AuditConstant.ACTION_DISCARD) || action.equals("Pin")) {
                em.setParticipantObjectDataLifeCycle(objectDataLifeCycle.toString());
            }
            em.setParticipantObjectIDTypeCode(errorMessageCodedValueType);
            auditMessage.getParticipantObjectIdentification().add(em);
        }
        return auditMessage;
    }

    /**
     * @param auditMessage
     * @param eventTargetObjectId
     * @param objectTypeCode
     * @param objectDataLifeCycle
     * @param EM_Code
     * @param EM_CodeSystemName
     * @param EM_DisplayName
     * @return
     */
    AuditMessage addEventTarget(AuditMessage auditMessage, List<String> eventTargetObjectId, Short objectTypeCode,
                                        Short objectDataLifeCycle, String EM_Code, String EM_CodeSystemName, String EM_DisplayName) {

        LOGGER.debug("AuditMessage addEventTarget('{}','{}','{}','{}','{}','{}','{}')", auditMessage, eventTargetObjectId,
                objectTypeCode, objectDataLifeCycle, EM_Code, EM_CodeSystemName, EM_DisplayName);

        for (String eventTargetId : eventTargetObjectId) {

            ParticipantObjectIdentificationContents eventTarget = new ParticipantObjectIdentificationContents();
            eventTarget.setParticipantObjectID(eventTargetId);
            eventTarget.setParticipantObjectTypeCode(objectTypeCode.toString());
            if (objectDataLifeCycle != null) {
                eventTarget.setParticipantObjectDataLifeCycle(objectDataLifeCycle.toString());
            }
            ParticipantObjectIDTypeCode eventTargetDescription = new ParticipantObjectIDTypeCode();
            eventTargetDescription.setCsdCode(EM_Code);
            eventTargetDescription.setCodeSystemName(EM_CodeSystemName);
            eventTargetDescription.setDisplayName(EM_DisplayName);
            eventTargetDescription.setOriginalText(EM_DisplayName);
            eventTarget.setParticipantObjectIDTypeCode(eventTargetDescription);
            auditMessage.getParticipantObjectIdentification().add(eventTarget);
        }
        return auditMessage;
    }

    private EventTypeCode buildEventTypeCode(EventType eventType) {
        EventTypeCode eventTypeCode = null;
        switch (eventType) {
            case SMP_QUERY:
                eventTypeCode = new EventTypeCodeBuilder()
                        .codeSystemName(eventType.getCode())
                        .csdCode("SMP")
                        .displayName("SMP::Query")
                        .originalText("SMP::Query")
                        .build();
                break;
            case SMP_PUSH:
                eventTypeCode = new EventTypeCodeBuilder()
                        .codeSystemName(eventType.getCode())
                        .csdCode("SMP")
                        .displayName("SMP::Push")
                        .originalText("SMP::Query")
                        .build();
                break;
        }
        return eventTypeCode;
    }

    private EventID buildEventID(EventType eventType, NcpSide ncpSide, String transactionName) {
        EventID eventID;
        switch (eventType) {
            case SMP_QUERY:
                eventID = new EventIDBuilder()
                        .codeSystemName("EHDSI-193")
                        .csdCode("SMP")
                        .displayName("SMP::Query")
                        .originalText("SMP::Query")
                        .build();
                break;
            case SMP_PUSH:
                eventID = new EventIDBuilder()
                        .codeSystemName("EHDSI-194")
                        .csdCode("SMP")
                        .displayName("SMP::Push")
                        .originalText("SMP::Push")
                        .build();
                break;
            default:
                eventID = new EventIDBuilder()
                        .codeSystemName("DCM")
                        .csdCode(getCsdCode(ncpSide, eventType))
                        .displayName(eventType.getIheTransactionName())
                        .originalText(getOriginalText(ncpSide, eventType))
                        .build();
        }
        return eventID;
    }

    /**
     * @param auditMessage
     * @param auditSource
     * @return
     */
    AuditMessage addAuditSource(AuditMessage auditMessage, String auditSource) {

        AuditSourceIdentificationContents auditSourceIdentification = new AuditSourceIdentificationContents();
        auditSourceIdentification.setAuditSourceID(auditSource);
        auditSourceIdentification.setAuditEnterpriseSiteID(auditSource);
        /*
        attribute code {
            "1" |                 ## End-user display device, diagnostic device
            "2" |                 ## Data acquisition device or instrument
            "3" |                 ## Web Server process or thread
            "4" |                 ## Application Server process or thread
            "5" |                 ## Database Server process or thread
            "6" |                 ## Security server, e.g., a domain controller
            "7" |                 ## ISO level 1-3 network component
            "8" |                 ## ISO level 4-6 operating software
            "9" |                 ## other
            token },              ## other values are allowed if a codeSystemName is present
        */
        AuditSourceTypeCode auditTypeSource = new AuditSourceTypeCode();
        auditTypeSource.setCsdCode("4");
        auditSourceIdentification.getAuditSourceTypeCode().add(auditTypeSource);

        auditMessage.setAuditSourceIdentification(auditSourceIdentification);
        return auditMessage;
    }

    private String getOriginalText(NcpSide ncpSide, EventType eventType) {
        switch (eventType) {
            case PATIENT_SERVICE_RETRIEVE:
            case ORDER_SERVICE_RETRIEVE:
            case ORCD_SERVICE_RETRIEVE:
                return ncpSide == NcpSide.NCP_A ? "Export" : "Import";
            case DISPENSATION_SERVICE_INITIALIZE:
            case DISPENSATION_SERVICE_DISCARD:
                return ncpSide == NcpSide.NCP_A ? "Import" : "Export";
            default:
                return "Query";
        }
    }

    private String getCsdCode(NcpSide ncpSide, EventType eventType) {
        switch (eventType) {
            case PATIENT_SERVICE_RETRIEVE:
            case ORDER_SERVICE_RETRIEVE:
            case ORCD_SERVICE_RETRIEVE:
                return ncpSide == NcpSide.NCP_A ? "110106" : "110107";
            case DISPENSATION_SERVICE_INITIALIZE:
            case DISPENSATION_SERVICE_DISCARD:
                return ncpSide == NcpSide.NCP_A ? "110107" : "110106";
            default:
                return "110112";
        }
    }
}
