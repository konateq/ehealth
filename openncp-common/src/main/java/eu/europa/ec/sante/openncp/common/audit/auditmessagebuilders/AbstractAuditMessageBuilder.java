package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.AuditConstant;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.common.audit.EventType;
import eu.europa.ec.sante.openncp.common.audit.eventidentification.EventIDBuilder;
import eu.europa.ec.sante.openncp.common.audit.eventidentification.EventIdentificationContentsBuilder;
import eu.europa.ec.sante.openncp.common.audit.eventidentification.EventTypeCodeBuilder;
import net.RFC3881.dicom.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class AbstractAuditMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuditMessageBuilder.class);
    private static final String LOCALHOST = "127.0.0.1";
    private static final String SMP_QUERY = "SMP::Query";
    private static final String SMP_PUSH = "SMP::Push";
    private static final String SMP = "SMP";

    protected AuditMessage addParticipantObject(final AuditMessage auditMessage, final String participantId, final Short participantCode,
                                                final Short participantRole, final String participantName, final String PS_ObjectCode, final String PS_ObjectCodeName,
                                                final String PS_ObjectCodeValue, final String PS_originalText,
                                                final String PS_getQueryByParameterPayload, final String PS_getHciIdentifierPayload) {

        final ParticipantObjectIdentificationContents participantObjectIdentification = new ParticipantObjectIdentificationContents();
        participantObjectIdentification.setParticipantObjectID(participantId);
        participantObjectIdentification.setParticipantObjectTypeCode(participantCode.toString());
        participantObjectIdentification.setParticipantObjectTypeCodeRole(participantRole.toString());

        final ParticipantObjectIDTypeCode codedValue = new ParticipantObjectIDTypeCode();
        codedValue.setCsdCode(PS_ObjectCode);
        codedValue.setCodeSystemName(PS_ObjectCodeName);
        codedValue.setDisplayName(PS_ObjectCodeValue);
        codedValue.setOriginalText(PS_originalText);

        // SystemObject and Query
        if(participantCode == 2 && (participantRole == 3 || participantRole == 24)) {
            final ParticipantObjectDetail participantObjectDetail = new ParticipantObjectDetail();
            // 'ihe:homeCommunityID' or 'Repository Unique Id'
            participantObjectDetail.setType("ihe:homeCommunityID");
            if (StringUtils.isNotBlank(PS_getHciIdentifierPayload)) {
                participantObjectDetail.setValue(PS_getHciIdentifierPayload.getBytes(StandardCharsets.UTF_8));
            }
            if (StringUtils.isNotBlank(PS_getQueryByParameterPayload)) {
                participantObjectIdentification.setParticipantObjectQuery(PS_getQueryByParameterPayload.getBytes(StandardCharsets.UTF_8));
            }
            participantObjectIdentification.getParticipantObjectDetail().add(participantObjectDetail);
        }

        participantObjectIdentification.setParticipantObjectIDTypeCode(codedValue);
        auditMessage.getParticipantObjectIdentification().add(participantObjectIdentification);
        return auditMessage;
    }

    protected AuditMessage createAuditTrailForHCPAssurance(final EventLog eventLog) {

        AuditMessage message = null;
        try {
            final ObjectFactory of = new ObjectFactory();
            message = of.createAuditMessage();
            addEventIdentification(message, eventLog.getEventType(), eventLog.getEI_TransactionName(),
                    eventLog.getEI_EventActionCode(), eventLog.getEI_EventDateTime(),
                    eventLog.getEI_EventOutcomeIndicator(), eventLog.getNcpSide());
            addPointOfCare(message, eventLog.getPC_UserID(), eventLog.getPC_RoleID(), false,
                    "1.3.6.1.4.1.12559.11.10.1.3.2.2.2");
            addHumanRequestor(message, eventLog.getHR_UserID(), eventLog.getHR_AlternativeUserID(), eventLog.getHR_RoleID(),
                    false);
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
        } catch (final Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return message;
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
    protected AuditMessage addService(final AuditMessage auditMessage, final String userId, final boolean userIsRequester, final String code,
                                      final String codeSystem, final String displayName) {

        if (StringUtils.isBlank(userId)) {
            LOGGER.warn("No Service, as this is Service Consumer");
            throw new IllegalArgumentException("Both ServiceConsumer User ID and ServiceProvider User ID must exist!");
        } else {
            final ActiveParticipantContents activeParticipant = new ActiveParticipantContents();
            activeParticipant.setUserID(userId);
            activeParticipant.setAlternativeUserID(userId);
            activeParticipant.setUserIsRequestor(userIsRequester);

            final RoleIDCode serviceConsumerRoleId = new RoleIDCode();
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
    protected AuditMessage addService(final AuditMessage auditMessage, final String userId, final boolean userIsRequester, final String code,
                                      final String codeSystem, final String displayName, final String ipAddress) {

        final InetAddressValidator validator = InetAddressValidator.getInstance();
        if (StringUtils.isBlank(userId)) {
            LOGGER.warn("No Service, as this is Service Consumer");
            throw new IllegalArgumentException("Both ServiceConsumer User ID and ServiceProvider User ID must exist!");
        } else {
            final ActiveParticipantContents activeParticipant = new ActiveParticipantContents();
            activeParticipant.setNetworkAccessPointID(ipAddress);
            if (validator.isValidInet4Address(ipAddress) || validator.isValidInet6Address(ipAddress)) {
                activeParticipant.setNetworkAccessPointTypeCode("2");
            } else {
                activeParticipant.setNetworkAccessPointTypeCode("1");
            }
            activeParticipant.setUserID(userId);
            activeParticipant.setAlternativeUserID(userId);
            activeParticipant.setUserIsRequestor(userIsRequester);

            final RoleIDCode serviceConsumerRoleId = new RoleIDCode();
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
    protected AuditMessage addPointOfCare(final AuditMessage message, final String userId, final String roleId, final boolean userIsRequester, final String codeSystem) {

        if (StringUtils.isBlank(userId)) {
            // This is service provider and doesn't need Point of Care
            final ActiveParticipantContents participant = new ActiveParticipantContents();
            participant.setUserID("SP");
            participant.setAlternativeUserID("SP");
            participant.setNetworkAccessPointID(LOCALHOST);
            participant.setNetworkAccessPointTypeCode("1");
            participant.setUserIsRequestor(userIsRequester);

            final RoleIDCode codedValue = new RoleIDCode();
            codedValue.setCsdCode("110152");
            codedValue.setCodeSystemName("DCM");
            codedValue.setOriginalText(codeSystem); // Should be "Destination Role ID"
            participant.getRoleIDCode().add(codedValue);
            message.getActiveParticipant().add(participant);
        } else {
            final ActiveParticipantContents participant = new ActiveParticipantContents();
            participant.setUserID(userId);
            participant.setAlternativeUserID(userId);
            participant.setNetworkAccessPointID(LOCALHOST);
            participant.setNetworkAccessPointTypeCode("1");
            participant.setUserIsRequestor(userIsRequester);

            final RoleIDCode codedValue = new RoleIDCode();
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
    AuditMessage addError(final AuditMessage auditMessage, final String errorMessagePartObjectId, final byte[] errorMessagePartObjectDetail,
                          final Short errorMessageCode, final Short errorMessageCodeRole, final String errorMessageTypeCode,
                          final String errorMessageQualifier) {

        // Error Message handling for audit purpose
        if (StringUtils.isNotBlank(errorMessagePartObjectId)) {

            LOGGER.debug("Error Message Participant ID is: '{}'", errorMessagePartObjectId);
            final ParticipantObjectIDTypeCode codedValueType = new ParticipantObjectIDTypeCode();
            codedValueType.setCsdCode(errorMessageTypeCode);
            codedValueType.setOriginalText("error message");
            codedValueType.setCodeSystemName("eHealth DSI Security");

            final ParticipantObjectIdentificationContents participantObjectIdentificationType = new ParticipantObjectIdentificationContents();
            participantObjectIdentificationType.setParticipantObjectID(errorMessagePartObjectId);
            participantObjectIdentificationType.setParticipantObjectTypeCode(errorMessageCode.toString());
            participantObjectIdentificationType.setParticipantObjectTypeCodeRole(errorMessageCodeRole.toString());
            participantObjectIdentificationType.setParticipantObjectIDTypeCode(codedValueType);

            if (errorMessagePartObjectDetail != null) {
                final ParticipantObjectDetail typeValuePairType = new ParticipantObjectDetail();
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
    AuditMessage addHumanRequestor(final AuditMessage auditMessage, final String userId, final String alternativeUserID,
                                   final String roleId, final boolean userIsRequester) {

        final ActiveParticipantContents humanRequester = new ActiveParticipantContents();
        humanRequester.setUserID(userId);
        humanRequester.setAlternativeUserID(alternativeUserID);
        humanRequester.setNetworkAccessPointID(LOCALHOST);
        humanRequester.setNetworkAccessPointTypeCode("2");
        humanRequester.setUserIsRequestor(userIsRequester);

        final RoleIDCode humanRequesterRoleId = new RoleIDCode();
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
    AuditMessage addEventIdentification(final AuditMessage auditMessage,
                                        final EventType eventType,
                                        final String transactionName,
                                        final String eventActionCode,
                                        final XMLGregorianCalendar eventDateTime,
                                        final BigInteger eventOutcomeIndicator,
                                        final NcpSide ncpSide) {

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
    AuditMessage addEventTarget(final AuditMessage auditMessage, final List<String> eventTargetObjectId, final Short typeCode,
                                final Short typeCodeRole, final String errorMessageCode, final String action, final Short objectDataLifeCycle,
                                final String EM_CodeSystemName, final String EM_DisplayName) {

        LOGGER.debug("AuditMessage addEventTarget('{}','{}','{}','{}','{}','{}','{}')", auditMessage, eventTargetObjectId,
                typeCode, typeCodeRole, errorMessageCode, action, objectDataLifeCycle);
        for (final String eventTargetId : eventTargetObjectId) {

            final ParticipantObjectIdentificationContents em = new ParticipantObjectIdentificationContents();
            em.setParticipantObjectID(eventTargetId);
            em.setParticipantObjectTypeCode(typeCode.toString());
            em.setParticipantObjectTypeCodeRole(typeCodeRole.toString());
            final ParticipantObjectIDTypeCode errorMessageCodedValueType = new ParticipantObjectIDTypeCode();
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
    AuditMessage addEventTarget(final AuditMessage auditMessage, final List<String> eventTargetObjectId, final Short objectTypeCode,
                                final Short objectDataLifeCycle, final String EM_Code, final String EM_CodeSystemName, final String EM_DisplayName) {

        LOGGER.debug("AuditMessage addEventTarget('{}','{}','{}','{}','{}','{}','{}')", auditMessage, eventTargetObjectId,
                objectTypeCode, objectDataLifeCycle, EM_Code, EM_CodeSystemName, EM_DisplayName);

        for (final String eventTargetId : eventTargetObjectId) {

            final ParticipantObjectIdentificationContents eventTarget = new ParticipantObjectIdentificationContents();
            eventTarget.setParticipantObjectID(eventTargetId);
            eventTarget.setParticipantObjectTypeCode(objectTypeCode.toString());
            if (objectDataLifeCycle != null) {
                eventTarget.setParticipantObjectDataLifeCycle(objectDataLifeCycle.toString());
            }
            final ParticipantObjectIDTypeCode eventTargetDescription = new ParticipantObjectIDTypeCode();
            eventTargetDescription.setCsdCode(EM_Code);
            eventTargetDescription.setCodeSystemName(EM_CodeSystemName);
            eventTargetDescription.setDisplayName(EM_DisplayName);
            eventTargetDescription.setOriginalText(EM_DisplayName);
            eventTarget.setParticipantObjectIDTypeCode(eventTargetDescription);
            auditMessage.getParticipantObjectIdentification().add(eventTarget);
        }
        return auditMessage;
    }

    private EventTypeCode buildEventTypeCode(final EventType eventType) {
        EventTypeCode eventTypeCode = null;
        switch (eventType) {
            case SMP_QUERY:
                eventTypeCode = new EventTypeCodeBuilder()
                        .codeSystemName(eventType.getCode())
                        .csdCode(SMP)
                        .displayName(SMP_QUERY)
                        .originalText(SMP_QUERY)
                        .build();
                break;
            case SMP_PUSH:
                eventTypeCode = new EventTypeCodeBuilder()
                        .codeSystemName(eventType.getCode())
                        .csdCode(SMP)
                        .displayName(SMP_PUSH)
                        .originalText(SMP_PUSH)
                        .build();
                break;
            default:
                break;
        }
        return eventTypeCode;
    }

    private EventID buildEventID(final EventType eventType, final NcpSide ncpSide, final String transactionName) {
        final EventID eventID;
        switch (eventType) {
            case SMP_QUERY:
                eventID = new EventIDBuilder()
                        .codeSystemName("EHDSI-193")
                        .csdCode(SMP)
                        .displayName(SMP_QUERY)
                        .originalText(SMP_QUERY)
                        .build();
                break;
            case SMP_PUSH:
                eventID = new EventIDBuilder()
                        .codeSystemName("EHDSI-194")
                        .csdCode(SMP)
                        .displayName(SMP_PUSH)
                        .originalText(SMP_PUSH)
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
    AuditMessage addAuditSource(final AuditMessage auditMessage, final String auditSource) {

        final AuditSourceIdentificationContents auditSourceIdentification = new AuditSourceIdentificationContents();
        auditSourceIdentification.setAuditSourceID(auditSource);
        auditSourceIdentification.setAuditEnterpriseSiteID(auditSource);
        /*
        attribute code
            "1" |                 ## End-user display device, diagnostic device
            "2" |                 ## Data acquisition device or instrument
            "3" |                 ## Web Server process or thread
            "4" |                 ## Application Server process or thread
            "5" |                 ## Database Server process or thread
            "6" |                 ## Security server, e.g., a domain controller
            "7" |                 ## ISO level 1-3 network component
            "8" |                 ## ISO level 4-6 operating software
            "9" |                 ## other
            token                 ## other values are allowed if a codeSystemName is present
        */
        final AuditSourceTypeCode auditTypeSource = new AuditSourceTypeCode();
        auditTypeSource.setCsdCode("4");
        auditSourceIdentification.getAuditSourceTypeCode().add(auditTypeSource);

        auditMessage.setAuditSourceIdentification(auditSourceIdentification);
        return auditMessage;
    }

    private String getOriginalText(final NcpSide ncpSide, final EventType eventType) {
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

    private String getCsdCode(final NcpSide ncpSide, final EventType eventType) {
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
