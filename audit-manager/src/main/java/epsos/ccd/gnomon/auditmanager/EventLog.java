package epsos.ccd.gnomon.auditmanager;

import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManager;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * EventLog is a java object with the minimal input variables needed to construct an AuditMessage according to DICOM.
 * Java Bean object for the Event Log object.
 * Getters and setters for all the properties Constructors for OpenNCP audit schemas.
 *
 * @author Kostas Karkaletsis
 * @author Organization: Gnomon
 * @author mail:k.karkaletsis@gnomon.com.gr
 */
public class EventLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLog.class);
    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");

    // NCPSide A or B information required for online validation
    private NcpSide ncpSide;
    private String EI_TransactionNumber;  // Number of the transaction including the 'Epsos-' prefix
    private String EI_TransactionName;    // Name of the transaction, specified in the use cases diagram
    private XMLGregorianCalendar EI_EventDateTime;
    private String EI_EventActionCode;    //  C:create, R:Read,View,Print,Query, U:Update, D:Delete, E:Execute
    private BigInteger EI_EventOutcomeIndicator; // Possible values are: 0:full success,1:partial delivery,4:temporal or recoverable failures,8:permanent failure
    // Event Identification
    private EventType eventType;

    private String queryByParameter;      // Query by parameter

    private String hciIdentifier;

    // Point of Care
    private String PC_UserID;  // Point of Care: Oid of the department
    private String PC_RoleID;  // Point of Care: Role of the department

    // Human Requester
    private String HR_UserID;  // Identifier of the HCP initiated the event
    private String HR_AlternativeUserID; // Human readable name of the HCP as given in the Subject-ID
    private String HR_UserName;
    private String HR_RoleID;
    // Service Consumer NCP
    // The string encoded CN of the TLS certificate of the NCP triggered the epsos operation
    private String SC_UserID;
    // Service Provider
    // The string encoded CN of the TLS certificate of the NCP triggered the epsos operation
    private String SP_UserID;
    // Audit Source
    private String AS_AuditSourceId; // The authority that is legally responsible for the audit source
    // Patient Source
    private List<String> PS_ParticipantObjectIDs; // List of patient Code in HL7 format
    // Patient Target
    private List<String> PT_ParticipantObjectIDs; // Mapped PatientCode in HL7 format
    // Error Message
    private String EM_ParticipantObjectID;  // String-encoded error code
    private byte[] EM_ParticipantObjectDetail;  // Base64 encoded error message
    // Mapping Service
    private String MS_UserID;  // The string encoded OID of the service instance performed the mapping

    // List of string encoded UID of the returned document
    private List<String> eventTargetParticipantObjectIds = new ArrayList<>();
    // The string encoded UID of the Additional returned document
    private String eventTargetAdditionalObjectId;
    // Non-Repudiation
    private String ReqM_ParticipantObjectID;  // ReqM_ParticipantObjectID String-encoded UUID of the request message
    private byte[] ReqM_ParticipantObjectDetail;  // Base64 encoded error message
    private String ResM_ParticipantObjectID;   // ReqM_ParticipantObjectID String-encoded UUID of the response message
    private byte[] ResM_ParticipantObjectDetail;  // Base64 encoded error message
    // IP Addresses
    private String sourceip;
    private String targetip;


    public EventLog() {
    }

    /**
     * This method creates an EventLog object for use in HCP Authentication
     *
     * @param eventIdentificationActionCode       Possible values according to D4.5.6 are E,R,U,D
     * @param eventIdentificationTime             The datetime the event occurred
     * @param eventIdentificationOutcomeIndicator <br>
     *                                            0 for full success <br>
     *                                            4 in case of partial delivery <br>
     *                                            8 for temporal failures <br>
     *                                            12 for permanent failure <br>
     * @param PC_UserID                           Point of Care: Oid of the department
     * @param PC_RoleID                           Role of the department
     * @param HR_UserID                           Identifier of the HCP initiated the event
     * @param HR_RoleID                           Role of the HCP initiated the event
     * @param HR_AlternativeUserID                Human readable name of the HCP as given in
     *                                            the Subject-ID
     * @param SC_UserID                           The string encoded CN of the TLS certificate of the NCP
     *                                            triggered the epsos operation
     * @param SP_UserID                           The string encoded CN of the TLS certificate of the NCP
     *                                            processed the epsos operation
     * @param AS_AuditSourceId                    the iso3166-2 code of the country responsible for
     *                                            the audit source
     * @param eventTargetObjectId                 The string encoded UID of the returned document
     * @param ReqM_ParticipantObjectID            String-encoded UUID of the request
     *                                            message
     * @param ReqM_ParticipantObjectDetail        The value MUST contain the base64
     *                                            encoded security header.
     * @param ResM_ParticipantObjectID            String-encoded UUID of the response
     *                                            message
     * @param ResM_ParticipantObjectDetail        The value MUST contain the base64
     *                                            encoded security header.
     * @param sourceIp                            The IP Address of the source Gateway
     * @param targetIp                            The IP Address of the target Gateway
     */
    public static EventLog createEventLogHCPIdentity(final TransactionName eventIdentificationTransactionName, final EventActionCode eventIdentificationActionCode,
                                                     final XMLGregorianCalendar eventIdentificationTime, final EventOutcomeIndicator eventIdentificationOutcomeIndicator,
                                                     final String PC_UserID, final String PC_RoleID, final String HR_UserID, final String HR_RoleID,
                                                     final String HR_AlternativeUserID, final String SC_UserID, final String SP_UserID,
                                                     final String AS_AuditSourceId, final String eventTargetObjectId, final String ReqM_ParticipantObjectID,
                                                     final byte[] ReqM_ParticipantObjectDetail, final String ResM_ParticipantObjectID,
                                                     final byte[] ResM_ParticipantObjectDetail, final String sourceIp, final String targetIp,
                                                     final NcpSide ncpSide) {

        LOGGER.info("Creating EventLog for HCP Identification: '{}'-'{}'",
                eventIdentificationTransactionName, eventIdentificationActionCode);
        final EventLog eventLog = new EventLog();
        // Setup Event Identification information
        eventLog.setEI_TransactionName(eventIdentificationTransactionName);
        eventLog.setEI_EventActionCode(eventIdentificationActionCode);
        eventLog.setEI_EventDateTime(eventIdentificationTime);
        eventLog.setEI_EventOutcomeIndicator(eventIdentificationOutcomeIndicator);
        // Setup Event Identification information
        eventLog.setPC_UserID(nullToEmptyString(PC_UserID));
        eventLog.setPC_RoleID(nullToEmptyString(PC_RoleID));
        // Setup Event Identification information
        eventLog.setHR_UserID(nullToEmptyString(HR_UserID));
        eventLog.setHR_RoleID(nullToEmptyString(HR_RoleID));
        eventLog.setHR_AlternativeUserID(nullToEmptyString(HR_AlternativeUserID));
        // Setup Event Identification information
        eventLog.setSC_UserID(nullToEmptyString(SC_UserID));
        eventLog.setSP_UserID(nullToEmptyString(SP_UserID));
        eventLog.setAS_AuditSourceId(nullToEmptyString(AS_AuditSourceId));
        // Setup Event Identification information
        eventLog.getEventTargetParticipantObjectIds().add(nullToEmptyString(eventTargetObjectId));
        //  TODO: Audit - Event Target
        // Setup Event Identification information
        eventLog.setReqM_ParticipantObjectID(nullToEmptyString(ReqM_ParticipantObjectID));
        eventLog.setReqM_ParticipantObjectDetail(ReqM_ParticipantObjectDetail);
        eventLog.setResM_ParticipantObjectID(nullToEmptyString(ResM_ParticipantObjectID));
        eventLog.setResM_ParticipantObjectDetail(ResM_ParticipantObjectDetail);
        // Setup Event Identification information
        eventLog.setSourceip(nullToEmptyString(sourceIp));
        eventLog.setTargetip(nullToEmptyString(targetIp));
        // Setup Event Identification information
        eventLog.setNcpSide(ncpSide);
        eventLog.setQueryByParameter("");
        eventLog.setHciIdentifier("");

        if (LOGGER_CLINICAL.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            LOGGER_CLINICAL.debug("EventLog Details: '{}'", eventLog);
        }
        return eventLog;
    }

    /**
     * This method creates an EventLog object for use in NLS Import
     *
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param SC_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     triggered the epsos operation
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param eventTargetObjectId          The string encoded UUID of the returned document
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param sourceip                     The IP Address of the source Gateway
     * @param targetip                     The IP Address of the target Gateway
     */
    public static EventLog createEventLogNCPTrustedServiceList(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                               final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                               final String SC_UserID, final String SP_UserID, final String eventTargetObjectId,
                                                               final String ReqM_ParticipantObjectID, final byte[] ReqM_ParticipantObjectDetail,
                                                               final String ResM_ParticipantObjectID, final byte[] ResM_ParticipantObjectDetail,
                                                               final String sourceip, final String targetip) {

        LOGGER.info("Creating EventLog for NCP Trust Service List: '{}'-'{}'",
                EI_TransactionName, EI_EventActionCode);
        final EventLog eventLog = new EventLog();
        eventLog.setAS_AuditSourceId("EP-00");
        eventLog.setEI_TransactionName(EI_TransactionName);
        eventLog.setEI_EventActionCode(EI_EventActionCode);
        eventLog.setEI_EventDateTime(EI_EventDateTime);
        eventLog.setEI_EventOutcomeIndicator(EI_EventOutcomeIndicator);
        eventLog.setSC_UserID(nullToEmptyString(SC_UserID));
        eventLog.setSP_UserID(nullToEmptyString(SP_UserID));
        //  TODO: Audit - Event Target
        eventLog.getEventTargetParticipantObjectIds().add(nullToEmptyString(eventTargetObjectId));
        eventLog.setReqM_ParticipantObjectID(nullToEmptyString(ReqM_ParticipantObjectID));
        eventLog.setReqM_ParticipantObjectDetail(ReqM_ParticipantObjectDetail);
        eventLog.setResM_ParticipantObjectID(nullToEmptyString(ResM_ParticipantObjectID));
        eventLog.setResM_ParticipantObjectDetail(ResM_ParticipantObjectDetail);
        eventLog.setSourceip(nullToEmptyString(sourceip));
        eventLog.setTargetip(nullToEmptyString(targetip));
        eventLog.setQueryByParameter("");
        eventLog.setHciIdentifier("");

        if (LOGGER_CLINICAL.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            LOGGER_CLINICAL.debug("'{}'", eventLog);
        }
        return eventLog;
    }

    /**
     * This method creates an EventLog object for use in Pivot Translation
     *
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param eventTargetObjectIdIn        The string encoded UUID of the source document
     * @param eventTargetObjectIdOut       The string encoded UUID of the target document
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param targetip                     The IP Address of the target Gateway
     */
    public static EventLog createEventLogPivotTranslation(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                          final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                          final String SP_UserID, final String eventTargetObjectIdIn, final String eventTargetObjectIdOut,
                                                          final String ReqM_ParticipantObjectID, final byte[] ReqM_ParticipantObjectDetail,
                                                          final String ResM_ParticipantObjectID, final byte[] ResM_ParticipantObjectDetail,
                                                          final String targetip) {

        LOGGER.info("Creating EventLog for CDA Pivot Translation: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        final EventLog eventLog = new EventLog();
        // Set Audit Source
        final ConfigurationManager configurationManager = ConfigurationManagerFactory.getConfigurationManager();
        eventLog.setAS_AuditSourceId(configurationManager.getProperty("COUNTRY_PRINCIPAL_SUBDIVISION"));
        eventLog.setEI_TransactionName(EI_TransactionName);
        eventLog.setEI_EventActionCode(EI_EventActionCode);
        eventLog.setEI_EventDateTime(EI_EventDateTime);
        eventLog.setEI_EventOutcomeIndicator(EI_EventOutcomeIndicator);
        eventLog.setSP_UserID(nullToEmptyString(SP_UserID));
        eventLog.getEventTargetParticipantObjectIds().add(nullToEmptyString(eventTargetObjectIdIn));
        //  TODO: Audit - Event Target
        eventLog.setEventTargetAdditionalObjectId(nullToEmptyString(eventTargetObjectIdOut));
        eventLog.setReqM_ParticipantObjectID(nullToEmptyString(ReqM_ParticipantObjectID));
        eventLog.setReqM_ParticipantObjectDetail(ReqM_ParticipantObjectDetail);
        eventLog.setResM_ParticipantObjectID(nullToEmptyString(ResM_ParticipantObjectID));
        eventLog.setResM_ParticipantObjectDetail(ResM_ParticipantObjectDetail);
        eventLog.setTargetip(nullToEmptyString(targetip));
        eventLog.setQueryByParameter("");
        eventLog.setHciIdentifier("");

        if (LOGGER_CLINICAL.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            LOGGER_CLINICAL.debug("'{}'", eventLog);
        }
        return eventLog;
    }

    public static EventLog createEventLogPatientPrivacy(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                        final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                        final String HR_UserID, final String HR_AlternativeUserID, final String HR_RoleID,
                                                        final String SC_UserID, final String SP_UserID, final String AS_AuditSourceId,
                                                        final List<String> PT_ParticipantObjectIDs, final String EM_ParticipantObjectID,
                                                        final byte[] EM_ParticipantObjectDetail, final String eventTargetObjectId,
                                                        final String ReqM_ParticipantObjectID, final byte[] ReqM_ParticipantObjectDetail,
                                                        final String ResM_ParticipantObjectID, final byte[] ResM_ParticipantObjectDetail,
                                                        final String sourceip, final String targetip) {

        LOGGER.info("Creating EventLog for Patient Privacy: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        return EventLog.createEventLogHCPAssurance(EI_TransactionName, EI_EventActionCode, EI_EventDateTime,
                EI_EventOutcomeIndicator, null, null, HR_UserID, HR_AlternativeUserID, HR_RoleID,
                SC_UserID, SP_UserID, AS_AuditSourceId, PT_ParticipantObjectIDs, EM_ParticipantObjectID,
                EM_ParticipantObjectDetail, eventTargetObjectId, ReqM_ParticipantObjectID, ReqM_ParticipantObjectDetail,
                ResM_ParticipantObjectID, ResM_ParticipantObjectDetail, sourceip, targetip);
    }

    /**
     * This method creates an EventLog object for use in Consent PIN
     *
     * @param EI_TransactionName           value is epsosConsentServicePin
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param PC_UserID                    Point of Care: Oid of the department
     * @param PC_RoleID                    Role of the point of care
     * @param HR_UserID                    Identifier of the HCP initiated the event
     * @param HR_AlternativeUserID         Human readable name of the HCP as given in
     *                                     the Subject-ID
     * @param HR_RoleID                    Role of the HCP initiated the event
     * @param SC_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     triggered the epsos operation
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param AS_AuditSourceId             the iso3166-2 code of the country responsible for
     *                                     the audit source
     * @param PT_ParticipantObjectIDs      List of Patient Identifiers in HL7 II format
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param sourceip                     The IP Address of the source Gateway
     * @param targetip                     The IP Address of the target Gateway
     * @return the EventLog object
     */
    public static EventLog createEventLogConsentPINack(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                       final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                       final String PC_UserID, final String PC_RoleID, final String HR_UserID, final String HR_AlternativeUserID,
                                                       final String HR_RoleID, final String SC_UserID, final String SP_UserID, final String AS_AuditSourceId,
                                                       final List<String> PT_ParticipantObjectIDs, final String ReqM_ParticipantObjectID,
                                                       final byte[] ReqM_ParticipantObjectDetail, final String ResM_ParticipantObjectID,
                                                       final byte[] ResM_ParticipantObjectDetail, final String sourceip, final String targetip) {

        LOGGER.info("Creating EventLog for Consent PIN ACK: '{}'-'{}'",
                EI_TransactionName, EI_EventActionCode);
        return EventLog.createEventLogHCPAssurance(EI_TransactionName, EI_EventActionCode, EI_EventDateTime, EI_EventOutcomeIndicator,
                PC_UserID, PC_RoleID, HR_UserID, HR_AlternativeUserID, HR_RoleID, SC_UserID, SP_UserID, AS_AuditSourceId,
                PT_ParticipantObjectIDs, null, null, "PINack",
                ReqM_ParticipantObjectID, ReqM_ParticipantObjectDetail, ResM_ParticipantObjectID, ResM_ParticipantObjectDetail,
                sourceip, targetip);
    }

    /**
     * This method creates an EventLog object for use in Consent PIN
     *
     * @param EI_TransactionName           value is epsosConsentServicePin
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param PC_UserID                    Point of Care: Oid of the department
     * @param PC_RoleID                    Role of the point of care
     * @param HR_UserID                    Identifier of the HCP initiated the event
     * @param HR_AlternativeUserID         Human readable name of the HCP as given in
     *                                     the Subject-ID
     * @param HR_RoleID                    Role of the HCP initiated the event
     * @param SC_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     triggered the epsos operation
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param AS_AuditSourceId             the iso3166-2 code of the country responsible for
     *                                     the audit source
     * @param PT_ParticipantObjectIDs      List of Patient Identifiers in HL7 II format
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param sourceip                     The IP Address of the source Gateway
     * @param targetip                     The IP Address of the target Gateway
     * @return the EventLog object
     */
    public static EventLog createEventLogConsentPINdny(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                       final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                       final String PC_UserID, final String PC_RoleID, final String HR_UserID, final String HR_AlternativeUserID,
                                                       final String HR_RoleID, final String SC_UserID, final String SP_UserID, final String AS_AuditSourceId,
                                                       final List<String> PT_ParticipantObjectIDs, final String ReqM_ParticipantObjectID,
                                                       final byte[] ReqM_ParticipantObjectDetail, final String ResM_ParticipantObjectID,
                                                       final byte[] ResM_ParticipantObjectDetail, final String sourceip, final String targetip) {

        LOGGER.info("Creating EventLog for Consent PIN Deny: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        return EventLog.createEventLogHCPAssurance(EI_TransactionName, EI_EventActionCode, EI_EventDateTime, EI_EventOutcomeIndicator,
                PC_UserID, PC_RoleID, HR_UserID, HR_AlternativeUserID, HR_RoleID, SC_UserID, SP_UserID, AS_AuditSourceId,
                PT_ParticipantObjectIDs, null, null, "PINdny",
                ReqM_ParticipantObjectID, ReqM_ParticipantObjectDetail, ResM_ParticipantObjectID, ResM_ParticipantObjectDetail,
                sourceip, targetip);
    }

    /**
     * This method creates an EventLog object for use in HCP Assurance Schema
     *
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param PC_UserID                    Point of Care: Oid of the department
     * @param PC_RoleID                    Role of the point of care
     * @param HR_UserID                    Identifier of the HCP initiated the event
     * @param HR_RoleID                    Role of the HCP initiated the event
     * @param HR_AlternativeUserID         Human readable name of the HCP as given in
     *                                     the Subject-ID
     * @param SC_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     triggered the epsos operation
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param AS_AuditSourceId             the iso3166-2 code of the country responsible for
     *                                     the audit source
     * @param PT_ParticipantObjectIDs      List of Patient Identifiers in HL7 II format
     * @param EM_ParticipantObjectID       The error code included with the response
     *                                     message
     * @param EM_ParticipantObjectDetail   Contains the base64 encoded error
     *                                     message
     * @param eventTargetObjectId          The string encoded UUID of the returned document
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param sourceip                     The IP Address of the source Gateway
     * @param targetip                     The IP Address of the target Gateway
     * @return the EventLog object
     */
    public static EventLog createEventLogHCPAssurance(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                      final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                      final String PC_UserID, final String PC_RoleID, final String HR_UserID, final String HR_AlternativeUserID,
                                                      final String HR_RoleID, final String SC_UserID, final String SP_UserID, final String AS_AuditSourceId,
                                                      final List<String> PT_ParticipantObjectIDs, final String EM_ParticipantObjectID,
                                                      final byte[] EM_ParticipantObjectDetail, final String eventTargetObjectId,
                                                      final String ReqM_ParticipantObjectID, final byte[] ReqM_ParticipantObjectDetail,
                                                      final String ResM_ParticipantObjectID, final byte[] ResM_ParticipantObjectDetail,
                                                      final String sourceip, final String targetip) {

        LOGGER.info("Creating EventLog for Healthcare Provider Assurance: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        final EventLog eventLog = new EventLog();
        eventLog.setEI_TransactionName(EI_TransactionName);
        eventLog.setEI_EventActionCode(EI_EventActionCode);
        eventLog.setEI_EventDateTime(EI_EventDateTime);
        eventLog.setEI_EventOutcomeIndicator(EI_EventOutcomeIndicator);
        eventLog.setPC_UserID(nullToEmptyString(PC_UserID));
        eventLog.setPC_RoleID(nullToEmptyString(PC_RoleID));
        eventLog.setHR_UserID(nullToEmptyString(HR_UserID));
        eventLog.setHR_AlternativeUserID(nullToEmptyString(HR_AlternativeUserID));
        eventLog.setHR_RoleID(nullToEmptyString(HR_RoleID));
        eventLog.setSC_UserID(nullToEmptyString(SC_UserID));
        eventLog.setSP_UserID(nullToEmptyString(SP_UserID));
        eventLog.setAS_AuditSourceId(nullToEmptyString(AS_AuditSourceId));
        eventLog.setPT_ParticipantObjectIDs(PT_ParticipantObjectIDs);
        eventLog.setEM_ParticipantObjectID(nullToEmptyString(EM_ParticipantObjectID));
        eventLog.setEM_ParticipantObjectDetail(EM_ParticipantObjectDetail);
        eventLog.getEventTargetParticipantObjectIds().add(nullToEmptyString(eventTargetObjectId));
        //  TODO: Audit - Event Target
        eventLog.setReqM_ParticipantObjectID(nullToEmptyString(ReqM_ParticipantObjectID));
        eventLog.setReqM_ParticipantObjectDetail(ReqM_ParticipantObjectDetail);
        eventLog.setResM_ParticipantObjectID(nullToEmptyString(ResM_ParticipantObjectID));
        eventLog.setResM_ParticipantObjectDetail(ResM_ParticipantObjectDetail);
        eventLog.setSourceip(nullToEmptyString(sourceip));
        eventLog.setTargetip(nullToEmptyString(targetip));
        eventLog.setQueryByParameter("");
        eventLog.setHciIdentifier("");

        if (LOGGER_CLINICAL.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            LOGGER_CLINICAL.debug("'{}'", eventLog);
        }
        return eventLog;
    }

    /**
     * @param EI_TransactionName
     * @param EI_EventActionCode
     * @param EI_EventDateTime
     * @param EI_EventOutcomeIndicator
     * @param PC_UserID
     * @param PC_RoleID
     * @param HR_UserID
     * @param HR_AlternativeUserID
     * @param HR_RoleID
     * @param SC_UserID
     * @param SP_UserID
     * @param AS_AuditSourceId
     * @param PT_ParticipantObjectIDs
     * @param EM_ParticipantObjectID
     * @param EM_ParticipantObjectDetail
     * @param eventTargetObjectId
     * @param ReqM_ParticipantObjectID
     * @param ReqM_ParticipantObjectDetail
     * @param ResM_ParticipantObjectID
     * @param ResM_ParticipantObjectDetail
     * @param sourceip
     * @param targetip
     * @return
     */
    public static EventLog createEventLogPatientService(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                        final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                        final String PC_UserID, final String PC_RoleID, final String HR_UserID,
                                                        final String HR_AlternativeUserID, final String HR_RoleID, final String SC_UserID,
                                                        final String SP_UserID, final String AS_AuditSourceId, final List<String> PT_ParticipantObjectIDs,
                                                        final String EM_ParticipantObjectID, final byte[] EM_ParticipantObjectDetail,
                                                        final String eventTargetObjectId, final String ReqM_ParticipantObjectID,
                                                        final byte[] ReqM_ParticipantObjectDetail, final String ResM_ParticipantObjectID,
                                                        final byte[] ResM_ParticipantObjectDetail, final String sourceip, final String targetip) {

        LOGGER.info("Creating EventLog for Patient Service: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        return EventLog.createEventLogHCPAssurance(EI_TransactionName, EI_EventActionCode, EI_EventDateTime, EI_EventOutcomeIndicator,
                PC_UserID, PC_RoleID, HR_UserID, HR_AlternativeUserID, HR_RoleID, SC_UserID, SP_UserID, AS_AuditSourceId,
                PT_ParticipantObjectIDs, EM_ParticipantObjectID, EM_ParticipantObjectDetail, eventTargetObjectId,
                ReqM_ParticipantObjectID, ReqM_ParticipantObjectDetail, ResM_ParticipantObjectID, ResM_ParticipantObjectDetail,
                sourceip, targetip);
    }

    /**
     * This method creates an EventLog object for use in Issuance of a Treatment Relationship Confirmation Assertion.
     *
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param PC_UserID                    Point of Care: Oid of the department
     * @param PC_RoleID                    Role of the Point of Care: Oid of the department
     * @param HR_UserID                    Identifier of the HCP initiated the event
     * @param HR_RoleID                    Role of HCP initiated the event
     * @param HR_AlternativeUserID         Human readable name of the HCP as given in
     *                                     the Subject-ID
     * @param SC_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     triggered the epsos operation
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param AS_AuditSourceId             the iso3166-2 code of the country responsible for
     *                                     the audit source
     * @param PT_ParticipantObjectIDs      List of Patient Identifiers in HL7 II format
     * @param eventTargetObjectId          The string encoded UUID of the returned document
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param sourceIp                     The IP Address of the source Gateway
     * @param targetIp                     The IP Address of the target Gateway
     * @return the EventLog object
     */
    public static EventLog createEventLogTRCA(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                              final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                              final String PC_UserID, final String PC_RoleID, final String HR_UserID, final String HR_RoleID,
                                              final String HR_AlternativeUserID, final String SC_UserID, final String SP_UserID,
                                              final String AS_AuditSourceId, final List<String> PT_ParticipantObjectIDs, final String eventTargetObjectId,
                                              final String ReqM_ParticipantObjectID, final byte[] ReqM_ParticipantObjectDetail,
                                              final String ResM_ParticipantObjectID, final byte[] ResM_ParticipantObjectDetail,
                                              final String sourceIp, final String targetIp, final NcpSide ncpSide) {

        LOGGER.info("Creating EventLog for TRC Assertions: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        final EventLog eventLog = new EventLog();
        eventLog.setEI_TransactionName(EI_TransactionName);
        eventLog.setEI_EventActionCode(EI_EventActionCode);
        eventLog.setEI_EventDateTime(EI_EventDateTime);
        eventLog.setEI_EventOutcomeIndicator(EI_EventOutcomeIndicator);
        eventLog.setPC_UserID(nullToEmptyString(PC_UserID));
        eventLog.setPC_RoleID(nullToEmptyString(PC_RoleID));
        eventLog.setHR_UserID(nullToEmptyString(HR_UserID));
        eventLog.setHR_RoleID(nullToEmptyString(HR_RoleID));
        eventLog.setHR_AlternativeUserID(nullToEmptyString(HR_AlternativeUserID));
        eventLog.setSC_UserID(nullToEmptyString(SC_UserID));
        eventLog.setSP_UserID(nullToEmptyString(SP_UserID));
        eventLog.setAS_AuditSourceId(nullToEmptyString(AS_AuditSourceId));
        eventLog.setPT_ParticipantObjectIDs(PT_ParticipantObjectIDs);
        eventLog.getEventTargetParticipantObjectIds().add(nullToEmptyString(eventTargetObjectId));
        //  TODO: Audit - Event Target
        eventLog.setReqM_ParticipantObjectID(nullToEmptyString(ReqM_ParticipantObjectID));
        eventLog.setReqM_ParticipantObjectDetail(ReqM_ParticipantObjectDetail);
        eventLog.setResM_ParticipantObjectID(nullToEmptyString(ResM_ParticipantObjectID));
        eventLog.setResM_ParticipantObjectDetail(ResM_ParticipantObjectDetail);
        eventLog.setSourceip(nullToEmptyString(sourceIp));
        eventLog.setTargetip(nullToEmptyString(targetIp));
        eventLog.ncpSide = ncpSide;
        eventLog.setQueryByParameter("");
        eventLog.setHciIdentifier("");

        if (LOGGER_CLINICAL.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            LOGGER_CLINICAL.debug("EventLog: '{}'", eventLog);
        }
        return eventLog;
    }

    /**
     * This method creates an EventLog object for use in Issuance of a Next of Kin Assertion.
     *
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param PC_UserID                    Point of Care: Oid of the department
     * @param PC_RoleID                    Role of the Point of Care: Oid of the department
     * @param HR_UserID                    Identifier of the HCP initiated the event
     * @param HR_RoleID                    Role of HCP initiated the event
     * @param HR_AlternativeUserID         Human-readable name of the HCP as given in
     *                                     the Subject-ID
     * @param SC_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     triggered the epsos operation
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param AS_AuditSourceId             the iso3166-2 code of the country responsible for
     *                                     the audit source
     * @param NOK_ParticipantObjectIDs     List of Next of Kin Identifiers in HL7 II format
     * @param eventTargetObjectId          The string encoded UUID of the returned document
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param sourceIp                     The IP Address of the source Gateway
     * @param targetIp                     The IP Address of the target Gateway
     * @return the EventLog object
     */
    public static EventLog createEventLogNOKA(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                              final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                              final String PC_UserID, final String PC_RoleID, final String HR_UserID, final String HR_RoleID,
                                              final String HR_AlternativeUserID, final String SC_UserID, final String SP_UserID,
                                              final String AS_AuditSourceId, final List<String> NOK_ParticipantObjectIDs, final String eventTargetObjectId,
                                              final String ReqM_ParticipantObjectID, final byte[] ReqM_ParticipantObjectDetail,
                                              final String ResM_ParticipantObjectID, final byte[] ResM_ParticipantObjectDetail,
                                              final String sourceIp, final String targetIp, final NcpSide ncpSide) {

        LOGGER.info("Creating EventLog for NOK Assertions: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        final EventLog eventLog = new EventLog();
        eventLog.setEI_TransactionName(EI_TransactionName);
        eventLog.setEI_EventActionCode(EI_EventActionCode);
        eventLog.setEI_EventDateTime(EI_EventDateTime);
        eventLog.setEI_EventOutcomeIndicator(EI_EventOutcomeIndicator);
        eventLog.setPC_UserID(nullToEmptyString(PC_UserID));
        eventLog.setPC_RoleID(nullToEmptyString(PC_RoleID));
        eventLog.setHR_UserID(nullToEmptyString(HR_UserID));
        eventLog.setHR_RoleID(nullToEmptyString(HR_RoleID));
        eventLog.setHR_AlternativeUserID(nullToEmptyString(HR_AlternativeUserID));
        eventLog.setSC_UserID(nullToEmptyString(SC_UserID));
        eventLog.setSP_UserID(nullToEmptyString(SP_UserID));
        eventLog.setAS_AuditSourceId(nullToEmptyString(AS_AuditSourceId));
        eventLog.setPT_ParticipantObjectIDs(NOK_ParticipantObjectIDs);
        eventLog.getEventTargetParticipantObjectIds().add(nullToEmptyString(eventTargetObjectId));
        //  TODO: Audit - Event Target
        eventLog.setReqM_ParticipantObjectID(nullToEmptyString(ReqM_ParticipantObjectID));
        eventLog.setReqM_ParticipantObjectDetail(ReqM_ParticipantObjectDetail);
        eventLog.setResM_ParticipantObjectID(nullToEmptyString(ResM_ParticipantObjectID));
        eventLog.setResM_ParticipantObjectDetail(ResM_ParticipantObjectDetail);
        eventLog.setSourceip(nullToEmptyString(sourceIp));
        eventLog.setTargetip(nullToEmptyString(targetIp));
        eventLog.ncpSide = ncpSide;
        eventLog.setQueryByParameter("");
        eventLog.setHciIdentifier("");

        if (LOGGER_CLINICAL.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            LOGGER_CLINICAL.debug("EventLog: '{}'", eventLog);
        }
        return eventLog;
    }

    /**
     * This method creates an EventLog object for use in Patient ID Mapping
     * Audit Schema
     *
     * @param EI_TransactionName
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param HR_UserID                    Identifier of the HCP initiated the event
     * @param HR_RoleID                    Role of the HCP initiated the event
     * @param HR_AlternativeUserID         Human readable name of the HCP as given in
     *                                     the Subject-ID attrbute of the HCP identity assertion
     * @param SC_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     triggered the epsos operation
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param AS_AuditSourceId             the iso3166-2 code of the country responsible for
     *                                     the audit source
     * @param PS_ParticipantObjectIDs      List of Patient Identifiers in HL7 II format
     *                                     (Patient Source)
     * @param PT_ParticipantObjectIDs      List of Patient Identifiers in HL7 II format
     *                                     (Patient Target)
     * @param EM_ParticipantObjectID       The error code included with the response
     *                                     message
     * @param EM_ParticipantObjectDetail   Contains the base64 encoded error
     *                                     message
     * @param MS_UserID                    The string encoded OID of the service instance performed
     *                                     the mapping
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param sourceip                     The IP Address of the source Gateway
     * @param targetip                     The IP Address of the target Gateway
     * @return the EventLog object
     */
    public static EventLog createEventLogPatientMapping(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                        final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                        final String HR_UserID, final String HR_RoleID, final String HR_AlternativeUserID,
                                                        final String SC_UserID, final String SP_UserID, final String AS_AuditSourceId,
                                                        final List<String> PS_ParticipantObjectIDs, final List<String> PT_ParticipantObjectIDs,
                                                        final String EM_ParticipantObjectID, final byte[] EM_ParticipantObjectDetail,
                                                        final String MS_UserID, final String ReqM_ParticipantObjectID,
                                                        final byte[] ReqM_ParticipantObjectDetail, final String ResM_ParticipantObjectID,
                                                        final byte[] ResM_ParticipantObjectDetail, final String sourceip, final String targetip) {

        LOGGER.info("Creating EventLog for Patient ID Mapping: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        final EventLog eventLog = new EventLog();
        eventLog.setEI_TransactionName(EI_TransactionName);
        eventLog.setEI_EventActionCode(EI_EventActionCode);
        eventLog.setEI_EventDateTime(EI_EventDateTime);
        eventLog.setEI_EventOutcomeIndicator(EI_EventOutcomeIndicator);
        eventLog.setHR_UserID(nullToEmptyString(HR_UserID));
        eventLog.setHR_RoleID(nullToEmptyString(HR_RoleID));
        eventLog.setHR_AlternativeUserID(nullToEmptyString(HR_AlternativeUserID));
        eventLog.setSC_UserID(nullToEmptyString(SC_UserID));
        eventLog.setSP_UserID(nullToEmptyString(SP_UserID));
        eventLog.setAS_AuditSourceId(nullToEmptyString(AS_AuditSourceId));
        eventLog.setPS_ParticipantObjectIDs(PS_ParticipantObjectIDs);
        eventLog.setPT_ParticipantObjectIDs(PT_ParticipantObjectIDs);
        eventLog.setEM_ParticipantObjectID(nullToEmptyString(EM_ParticipantObjectID));
        eventLog.setEM_ParticipantObjectDetail(EM_ParticipantObjectDetail);
        eventLog.setMS_UserID(nullToEmptyString(MS_UserID));
        eventLog.setReqM_ParticipantObjectID(nullToEmptyString(ReqM_ParticipantObjectID));
        eventLog.setReqM_ParticipantObjectDetail(ReqM_ParticipantObjectDetail);
        eventLog.setResM_ParticipantObjectID(nullToEmptyString(ResM_ParticipantObjectID));
        eventLog.setResM_ParticipantObjectDetail(ResM_ParticipantObjectDetail);
        eventLog.setSourceip(nullToEmptyString(sourceip));
        eventLog.setTargetip(nullToEmptyString(targetip));
        eventLog.setQueryByParameter("");
        eventLog.setHciIdentifier("");

        if (LOGGER_CLINICAL.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            LOGGER_CLINICAL.debug("'{}'", eventLog);
        }
        return eventLog;
    }

    /**
     * This method creates an EventLog object for use in HCP Assurance Schema
     *
     * @param EI_EventActionCode           Possible values according to D4.5.6 are E,R,U,D
     * @param EI_EventDateTime             The datetime the event occurred
     * @param EI_EventOutcomeIndicator     <br>
     *                                     0 for full success <br>
     *                                     4 in case of partial delivery <br>
     *                                     8 for temporal failures <br>
     *                                     12 for permanent failure <br>
     * @param PC_UserID                    Point of Care: Oid of the department
     * @param PC_RoleID                    Point of Care: Role of the department
     * @param HR_UserID                    Identifier of the HCP initiated the event
     * @param HR_RoleID                    Role of the HCP initiated the event
     * @param HR_AlternativeUserID         Human readable name of the HCP as given in
     *                                     the Subject-ID
     * @param SC_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     triggered the epsos operation
     * @param SP_UserID                    The string encoded CN of the TLS certificate of the NCP
     *                                     processed the epsos operation
     * @param PT_ParticipantObjectIDs      List of Patient Identifiers in HL7 II format
     * @param EM_ParticipantObjectID       The error code included with the response
     *                                     message
     * @param EM_ParticipantObjectDetail   Contains the base64 encoded error
     *                                     message
     * @param ReqM_ParticipantObjectID     String-encoded UUID of the request
     *                                     message
     * @param ReqM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param ResM_ParticipantObjectID     String-encoded UUID of the response
     *                                     message
     * @param ResM_ParticipantObjectDetail The value MUST contain the base64
     *                                     encoded security header.
     * @param sourceip                     The IP Address of the source Gateway
     * @param targetip                     The IP Address of the target Gateway
     * @return the EventLog object
     */
    public static EventLog createEventLogCommunicationFailure(final TransactionName EI_TransactionName, final EventActionCode EI_EventActionCode,
                                                              final XMLGregorianCalendar EI_EventDateTime, final EventOutcomeIndicator EI_EventOutcomeIndicator,
                                                              final String PC_UserID, final String PC_RoleID, final String HR_UserID,
                                                              final String HR_RoleID, final String HR_AlternativeUserID, final String SC_UserID,
                                                              final String SP_UserID, final List<String> PT_ParticipantObjectIDs, final String EM_ParticipantObjectID,
                                                              final byte[] EM_ParticipantObjectDetail, final String ReqM_ParticipantObjectID,
                                                              final byte[] ReqM_ParticipantObjectDetail, final String ResM_ParticipantObjectID,
                                                              final byte[] ResM_ParticipantObjectDetail, final String sourceip, final String targetip) {

        LOGGER.info("Creating EventLog for System Communication Failure: '{}'-'{}'", EI_TransactionName, EI_EventActionCode);
        final EventLog eventLog = new EventLog();
        eventLog.setEI_TransactionName(EI_TransactionName);
        eventLog.setEI_EventActionCode(EI_EventActionCode);
        eventLog.setEI_EventDateTime(EI_EventDateTime);
        eventLog.setEI_EventOutcomeIndicator(EI_EventOutcomeIndicator);
        eventLog.setPC_UserID(nullToEmptyString(PC_UserID));
        eventLog.setPC_RoleID(nullToEmptyString(PC_RoleID));
        eventLog.setHR_UserID(nullToEmptyString(HR_UserID));
        eventLog.setHR_RoleID(nullToEmptyString(HR_RoleID));
        eventLog.setHR_AlternativeUserID(nullToEmptyString(HR_AlternativeUserID));
        eventLog.setSC_UserID(nullToEmptyString(SC_UserID));
        eventLog.setSP_UserID(nullToEmptyString(SP_UserID));
        eventLog.setPT_ParticipantObjectIDs(PT_ParticipantObjectIDs);
        eventLog.setEM_ParticipantObjectID(nullToEmptyString(EM_ParticipantObjectID));
        eventLog.setEM_ParticipantObjectDetail(EM_ParticipantObjectDetail);
        eventLog.setReqM_ParticipantObjectID(nullToEmptyString(ReqM_ParticipantObjectID));
        eventLog.setReqM_ParticipantObjectDetail(ReqM_ParticipantObjectDetail);
        eventLog.setResM_ParticipantObjectID(nullToEmptyString(ResM_ParticipantObjectID));
        eventLog.setResM_ParticipantObjectDetail(ResM_ParticipantObjectDetail);
        eventLog.setSourceip(nullToEmptyString(sourceip));
        eventLog.setTargetip(nullToEmptyString(targetip));
        eventLog.setQueryByParameter("");
        eventLog.setHciIdentifier("");

        if (LOGGER_CLINICAL.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            LOGGER_CLINICAL.debug("'{}'", eventLog);
        }
        return eventLog;
    }

    /**
     * @param str represents a string
     * @return empty string if the param is null. Otherwise returns the string as is
     */
    private static String nullToEmptyString(final String str) {

        return str == null ? "" : str;
    }

    public String getHR_RoleID() {
        return HR_RoleID;
    }

    public void setHR_RoleID(final String HR_RoleID) {
        this.HR_RoleID = HR_RoleID;
    }

    public String getEventTargetAdditionalObjectId() {
        return eventTargetAdditionalObjectId;
    }

    public void setEventTargetAdditionalObjectId(final String eventTargetAdditionalObjectId) {
        this.eventTargetAdditionalObjectId = eventTargetAdditionalObjectId;
    }

    public String getPC_RoleID() {
        return PC_RoleID;
    }

    public void setPC_RoleID(final String PC_RoleID) {
        this.PC_RoleID = PC_RoleID;
    }

    public String getSourceip() {
        return sourceip;
    }

    public void setSourceip(final String sourceip) {
        this.sourceip = sourceip;
    }

    public String getTargetip() {
        return targetip;
    }

    public void setTargetip(final String targetip) {
        this.targetip = targetip;
    }

    public String getReqM_ParticipantObjectID() {
        return ReqM_ParticipantObjectID;
    }

    public void setReqM_ParticipantObjectID(final String ReqM_ParticipantObjectID) {
        this.ReqM_ParticipantObjectID = ReqM_ParticipantObjectID;
    }

    public byte[] getReqM_ParticipantObjectDetail() {
        return ReqM_ParticipantObjectDetail;
    }

    public void setReqM_ParticipantObjectDetail(final byte[] ReqM_ParticipantObjectDetail) {
        this.ReqM_ParticipantObjectDetail = ReqM_ParticipantObjectDetail;
    }

    public String getResM_ParticipantObjectID() {
        return ResM_ParticipantObjectID;
    }

    public void setResM_ParticipantObjectID(final String ResM_ParticipantObjectID) {
        this.ResM_ParticipantObjectID = ResM_ParticipantObjectID;
    }

    public byte[] getResM_ParticipantObjectDetail() {
        return ResM_ParticipantObjectDetail;
    }

    public void setResM_ParticipantObjectDetail(final byte[] ResM_ParticipantObjectDetail) {
        this.ResM_ParticipantObjectDetail = ResM_ParticipantObjectDetail;
    }

    public List<String> getEventTargetParticipantObjectIds() {
        return eventTargetParticipantObjectIds;
    }

    public void setEventTargetParticipantObjectIds(final List<String> eventTargetParticipantObjectIds) {
        this.eventTargetParticipantObjectIds = eventTargetParticipantObjectIds;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public void setEventType(final EventType eventType) {
        this.eventType = eventType;
    }

    public byte[] getEM_ParticipantObjectDetail() {
        return EM_ParticipantObjectDetail;
    }

    public void setEM_ParticipantObjectDetail(final byte[] EM_ParticipantObjectDetail) {
        this.EM_ParticipantObjectDetail = EM_ParticipantObjectDetail;
    }

    public String getAS_AuditSourceId() {
        return AS_AuditSourceId;
    }

    public void setAS_AuditSourceId(final String AS_AuditSourceId) {
        this.AS_AuditSourceId = AS_AuditSourceId;
    }

    public String getEI_EventActionCode() {
        return EI_EventActionCode;
    }

    public void setEI_EventActionCode(final EventActionCode EI_EventActionCode) {
        this.EI_EventActionCode = EI_EventActionCode.getCode();
    }

    public XMLGregorianCalendar getEI_EventDateTime() {
        return EI_EventDateTime;
    }

    public void setEI_EventDateTime(final XMLGregorianCalendar EI_EventDateTime) {
        this.EI_EventDateTime = EI_EventDateTime;
    }

    public BigInteger getEI_EventOutcomeIndicator() {
        return EI_EventOutcomeIndicator;
    }

    public void setEI_EventOutcomeIndicator(final EventOutcomeIndicator EI_EventOutcomeIndicator) {
        this.EI_EventOutcomeIndicator = EI_EventOutcomeIndicator.getCode();
    }

    public String getEI_TransactionName() {
        return EI_TransactionName;
    }

    public void setEI_TransactionName(final TransactionName EI_TransactionName) {
        this.EI_TransactionName = EI_TransactionName.getCode();
    }

    public String getEI_TransactionNumber() {
        return EI_TransactionNumber;
    }

    public void setEI_TransactionNumber(final String EI_TransactionNumber) {
        this.EI_TransactionNumber = EI_TransactionNumber;
    }

    public String getEM_ParticipantObjectID() {
        return EM_ParticipantObjectID;
    }

    public void setEM_ParticipantObjectID(final String EM_ParticipantObjectID) {
        this.EM_ParticipantObjectID = EM_ParticipantObjectID;
    }

    public String getHR_AlternativeUserID() {
        return HR_AlternativeUserID;
    }

    public void setHR_AlternativeUserID(final String HR_AlternativeUserID) {
        this.HR_AlternativeUserID = HR_AlternativeUserID;
    }

    public String getHR_UserID() {
        return HR_UserID;
    }

    public void setHR_UserID(final String HR_UserID) {
        this.HR_UserID = HR_UserID;
    }

    public String getHR_UserName() {
        return HR_UserName;
    }

    public void setHR_UserName(final String HR_UserName) {
        this.HR_UserName = HR_UserName;
    }

    public String getMS_UserID() {
        return MS_UserID;
    }

    public void setMS_UserID(final String MS_UserID) {
        this.MS_UserID = MS_UserID;
    }

    public String getPC_UserID() {
        return PC_UserID;
    }

    public void setPC_UserID(final String PC_UserID) {
        this.PC_UserID = PC_UserID;
    }

    public List<String> getPS_ParticipantObjectIDs() {
        return PS_ParticipantObjectIDs;
    }

    public void setPS_ParticipantObjectIDs(final List<String> PS_ParticipantObjectIDs) {
        this.PS_ParticipantObjectIDs = PS_ParticipantObjectIDs;
    }

    public List<String> getPT_ParticipantObjectIDs() {
        return PT_ParticipantObjectIDs;
    }

    public void setPT_ParticipantObjectIDs(final List<String> PT_ParticipantObjectIDs) {
        this.PT_ParticipantObjectIDs = PT_ParticipantObjectIDs;
    }

    public String getSC_UserID() {
        return SC_UserID;
    }

    public void setSC_UserID(final String SC_UserID) {
        this.SC_UserID = SC_UserID;
    }

    public String getSP_UserID() {
        return SP_UserID;
    }

    public void setSP_UserID(final String SP_UserID) {
        this.SP_UserID = SP_UserID;
    }

    public NcpSide getNcpSide() {
        return ncpSide;
    }

    public void setNcpSide(final NcpSide ncpSide) {
        this.ncpSide = ncpSide;
    }

    public String getQueryByParameter() {
        return queryByParameter;
    }

    public void setQueryByParameter(final String queryByParameter) {
        this.queryByParameter = queryByParameter;
    }

    public String getHciIdentifier() {
        return hciIdentifier;
    }

    public void setHciIdentifier(final String hciIdentifier) {
        this.hciIdentifier = hciIdentifier;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("ncpSide", ncpSide)
                .append("EI_TransactionNumber", EI_TransactionNumber)
                .append("EI_TransactionName", EI_TransactionName)
                .append("EI_EventDateTime", EI_EventDateTime)
                .append("EI_EventActionCode", EI_EventActionCode)
                .append("EI_EventOutcomeIndicator", EI_EventOutcomeIndicator)
                .append("EventType", eventType)
                .append("PC_UserID", PC_UserID)
                .append("PC_RoleID", PC_RoleID)
                .append("HR_UserID", HR_UserID)
                .append("HR_AlternativeUserID", HR_AlternativeUserID)
                .append("HR_UserName", HR_UserName)
                .append("HR_RoleID", HR_RoleID)
                .append("SC_UserID", SC_UserID)
                .append("SP_UserID", SP_UserID)
                .append("AS_AuditSourceId", AS_AuditSourceId)
                .append("PS_ParticipantObjectIDs", PS_ParticipantObjectIDs)
                .append("PT_ParticipantObjectIDs", PT_ParticipantObjectIDs)
                .append("EM_ParticipantObjectID", EM_ParticipantObjectID)
                .append("EM_ParticipantObjectDetail", EM_ParticipantObjectDetail)
                .append("MS_UserID", MS_UserID)
                .append("eventTargetParticipantObjectIds", eventTargetParticipantObjectIds)
                .append("eventTargetAdditionalObjectId", eventTargetAdditionalObjectId)
                .append("ReqM_ParticipantObjectID", ReqM_ParticipantObjectID)
                .append("ResM_ParticipantObjectID", ResM_ParticipantObjectID)
                .append("sourceip", sourceip)
                .append("targetip", targetip)
                .append("queryByParameter", queryByParameter)
                .append("hciIdentifier", hciIdentifier)
                .toString();
    }
}
