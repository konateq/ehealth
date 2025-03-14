package eu.europa.ec.sante.openncp.core.common.ihe.evidence;

import org.herasaf.xacml.core.policy.impl.AttributeAssignmentType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

public class ATNAObligationHandler implements ObligationHandler {

    private static final String ATNA_PREFIX = "urn:eSENS:obligations:nrr:ATNA";
    private static final String EVENT_ACTION_CODE = "EventActionCode";
    private static final String EVENT_DATE_TIME = "EventDateTime";
    private static final String EVENT_OUTCOME_INDICATOR = "EventOutcomeIndicator";
    private static final String EVENT_ID = "EventIDType";
    private static final String CODE = "code";
    private static final String CODE_SYSTEM = "codeSystem";
    private static final String CODE_SYSTEM_NAME = "codeSystemName";
    private static final String DISPLAY_NAME = "displayName";
    private static final String EVENT_TYPE_CODE = "EventTypeCodeType";
    private static final String ACTIVE_PARTICIPANT = "ActiveParticipant";
    private static final String ALTERNATIVE_USER_ID = "AlternativeUserID";
    private static final String NETWORK_ACCESS_POINT_ID = "NetworkAccessPointID";
    private static final String NETWORK_ACCESS_POINT_TYPE_CODE = "NetworkAccessPointTypeCode";
    private static final String USER_ID = "UserID";
    private static final String USER_IS_REQUESTOR = "UserIsRequestor";
    private static final String USER_NAME = "UserName";
    private static final String ROLE_ID_CODE = "RoleIDCode";
    private static final String SOURCE = "Source";
    private static final String AUDIT_ENTERPRISE_SITE_ID = "AuditEnterpriseSiteID";
    private static final String AUDIT_SOURCE_IDENTIFICATION = "AuditSourceIdentification";
    private static final String AUDIT_SOURCE_ID = "AuditSourceID";
    private static final String AUDIT_SOURCE_TYPE_CODE = "AuditSourceTypeCode";
    private static final String PARTICIPANT_OBJECT_IDENTIFICATION = "ParticipantObjectIdentification";
    private static final String PARTICIPANT_OBJECT_DATA_LIFE_CYCLE = "ParticipantObjectDataLifeCycle";
    private static final String PARTICIPANT_OBJECT_ID = "ParticipantObjectID";
    private static final String PARTICIPANT_OBJECT_TYPE_CODE = "ParticipantObjectTypeCode";
    private static final String PARTICIPANT_OBJECT_TYPE_CODE_ROLE = "ParticipantObjectTypeCodeRole";
    private static final String PARTICIPANT_OBJECT_ID_TYPE_CODE = "ParticipantObjectIDTypeCode";
    private static final String N_A = "N/A";

    private final HashMap<String, String> auditValueMap = new HashMap<>();
    private final IHEMessageType messageType;
    private final List<ESensObligation> obligations;
    private final Context context;
    private Document audit = null;

    public ATNAObligationHandler(final MessageType messageType, final List<ESensObligation> obligations, final Context context) {

        this.messageType = (IHEMessageType) messageType;
        this.obligations = obligations;
        this.context = context;
    }

    /**
     * Discharge returns the object discharged, or exception(non-Javadoc)
     *
     * @throws ObligationDischargeException
     */

    @Override
    public void discharge() throws ObligationDischargeException {

        // Here I need to check the IHE message type. It can be XCA, XCF, whatever
        if (messageType instanceof IHEXCARetrieve) {
            try {
                makeIHEXCARetrieveAudit(obligations);
            } catch (final ParserConfigurationException e) {
                throw new ObligationDischargeException(e);
            }
        } else {
            throw new ObligationDischargeException("Unkwnon message type");
        }
    }

    private void makeIHEXCARetrieveAudit(final List<ESensObligation> obligations2)
                        throws ParserConfigurationException {

        for (final ESensObligation eSensObl : obligations2) {
            // Here I am in the ATNA handler, thus I have to check if it is prefixed by ATNA
            if (eSensObl.getObligationID().startsWith(ATNA_PREFIX)) {
                final String outcome = getOutcome(eSensObl);

                // Here the real mapping happens: are we NRR or NRO? I think both, and it depends on the policy
                final List<AttributeAssignmentType> attributeAssignments = eSensObl.getAttributeAssignments();

                if (attributeAssignments != null) {
                    for (final AttributeAssignmentType aat : attributeAssignments) {
                        if (aat.getAttributeId().startsWith(ATNA_PREFIX)) {
                            fillHash(aat.getAttributeId(), aat.getContent());
                        }
                    }
                }
                makeAuditXml();
            }
            // else skip, it's not an ATNA obligation
        }
    }

    private String getOutcome(final ESensObligation eSensObl) {
        final String outcome;
        if (eSensObl instanceof PERMITEsensObligation) {
            outcome = "SUCCESS";
        } else {
            outcome = "MINOR FAILURE";
        }
        return outcome;
    }

    private void makeAuditXml() throws ParserConfigurationException {

        /*
         * Read the values that you need from the auditValueMap and fill the XML.
         *
         * The idea is to let configurable some parts. The configurable options are defined in the policy
         * and if they are available in the hashmap, use them, if not, use the presets.
         *
         * The idea is to call here the OpenNCP implementation. This is just a placeholder
         */
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setXIncludeAware(false);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setNamespaceAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document auditDocument = db.newDocument();

        final Element auditMessage = auditDocument.createElement("AuditMessage");
        auditDocument.appendChild(auditMessage);
        final Element eventIdentification = auditDocument.createElement("EventIdentification");
        eventIdentification.setAttribute(EVENT_ACTION_CODE, auditValueMap.get(EVENT_ACTION_CODE).trim().isEmpty() ? N_A : auditValueMap.get(EVENT_ACTION_CODE).trim());
        eventIdentification.setAttribute(EVENT_DATE_TIME, Instant.now().toString());
        eventIdentification.setAttribute(EVENT_OUTCOME_INDICATOR, auditValueMap.get(EVENT_OUTCOME_INDICATOR).trim().isEmpty() ? N_A : auditValueMap.get(EVENT_OUTCOME_INDICATOR).trim()); // by default, we fail

        final Element eventId = auditDocument.createElement(EVENT_ID);
        eventIdentification.appendChild(eventId);
        eventId.setAttribute(CODE, auditValueMap.get(EVENT_ID).isEmpty() ? N_A : auditValueMap.get(EVENT_ID).trim());
        eventId.setAttribute(CODE_SYSTEM, "");
        eventId.setAttribute(CODE_SYSTEM_NAME, "DCM");
        eventId.setAttribute(DISPLAY_NAME, "Export");

        //What have been done?
        final Element eventTypeCode = auditDocument.createElement(EVENT_TYPE_CODE);
        eventIdentification.appendChild(eventTypeCode);
        eventTypeCode.setAttribute(CODE, auditValueMap.get(EVENT_TYPE_CODE).trim().isEmpty() ? N_A : auditValueMap.get(EVENT_TYPE_CODE).trim());
        eventTypeCode.setAttribute(CODE_SYSTEM, "");
        eventTypeCode.setAttribute(CODE_SYSTEM_NAME, "IHE Transactions");
        eventTypeCode.setAttribute(DISPLAY_NAME, "Retrieve Document Set");

        auditMessage.appendChild(eventIdentification);

        // Initiated from where
        final Element activeParticipant1 = auditDocument.createElement(ACTIVE_PARTICIPANT);
        activeParticipant1.setAttribute(ALTERNATIVE_USER_ID, context.getUsername().isEmpty() ? N_A : context.getUsername().trim());
        activeParticipant1.setAttribute(NETWORK_ACCESS_POINT_ID, context.getCurrentHost().isEmpty() ? N_A : context.getCurrentHost().trim());
        activeParticipant1.setAttribute(NETWORK_ACCESS_POINT_TYPE_CODE, "2");
        activeParticipant1.setAttribute(USER_ID, "1.2.3.4.5.1.1000.990.1.1.1.22");
        activeParticipant1.setAttribute(USER_IS_REQUESTOR, "false");
        activeParticipant1.setAttribute(USER_NAME, "");
        final Element roleIdCode1 = auditDocument.createElement(ROLE_ID_CODE);
        activeParticipant1.appendChild(roleIdCode1);
        roleIdCode1.setAttribute(CODE, "110153");
        roleIdCode1.setAttribute(CODE_SYSTEM, "");
        roleIdCode1.setAttribute(CODE_SYSTEM_NAME, "DCM");
        roleIdCode1.setAttribute(DISPLAY_NAME, SOURCE);

        // What is the recipient?
        final Element activeParticipant2 = auditDocument.createElement(ACTIVE_PARTICIPANT);
        activeParticipant2.setAttribute(NETWORK_ACCESS_POINT_ID, context.getRemoteHost().trim().isEmpty() ? N_A : context.getRemoteHost().trim());
        activeParticipant2.setAttribute(NETWORK_ACCESS_POINT_TYPE_CODE, "2");
        activeParticipant2.setAttribute(USER_ID, "http://www.w3.org/2005/08/addressing/anonymous");
        activeParticipant2.setAttribute(USER_IS_REQUESTOR, "true");
        activeParticipant2.setAttribute(USER_NAME, "");
        final Element roleIdCode2 = auditDocument.createElement(ROLE_ID_CODE);
        activeParticipant2.appendChild(roleIdCode2);
        roleIdCode2.setAttribute(CODE, "110152");
        roleIdCode2.setAttribute(CODE_SYSTEM, "");
        roleIdCode2.setAttribute(CODE_SYSTEM_NAME, "DCM");
        roleIdCode2.setAttribute(DISPLAY_NAME, "Destination");

        // Who is the physician?
        final Element activeParticipant3 = auditDocument.createElement(ACTIVE_PARTICIPANT);
        activeParticipant3.setAttribute(USER_ID, context.getUsername().trim().isEmpty() ? N_A : context.getUsername().trim());
        activeParticipant3.setAttribute(USER_IS_REQUESTOR, "true");
        activeParticipant3.setAttribute(USER_NAME, "");
        final Element roleIdCode3 = auditDocument.createElement(ROLE_ID_CODE);
        activeParticipant3.appendChild(roleIdCode3);
        roleIdCode3.setAttribute(CODE, "USR");
        roleIdCode3.setAttribute(CODE_SYSTEM, "1.3.6.1.4.1.21998.2.1.5");
        roleIdCode3.setAttribute(CODE_SYSTEM_NAME, "Tiani-Spirit Audit Participant Role ID Codes");
        roleIdCode3.setAttribute(DISPLAY_NAME, "User");

        auditMessage.appendChild(activeParticipant1);
        auditMessage.appendChild(activeParticipant2);
        auditMessage.appendChild(activeParticipant3);

        // Who is the audit client?
        final Element auditSourceIdentification = auditDocument.createElement(AUDIT_SOURCE_IDENTIFICATION);
        auditSourceIdentification.setAttribute(AUDIT_ENTERPRISE_SITE_ID, auditValueMap.get(AUDIT_ENTERPRISE_SITE_ID).trim().isEmpty() ? N_A : auditValueMap.get(AUDIT_ENTERPRISE_SITE_ID).trim());
        auditSourceIdentification.setAttribute(AUDIT_SOURCE_ID, "urn:epsos:ncpa");
        final Element auditSourceTypeCode = auditDocument.createElement(AUDIT_SOURCE_TYPE_CODE);
        auditSourceIdentification.appendChild(auditSourceTypeCode);
        auditSourceTypeCode.setAttribute(CODE, "DOC_REPOSITORY");
        auditSourceTypeCode.setAttribute(CODE_SYSTEM, "");
        auditSourceTypeCode.setAttribute(CODE_SYSTEM_NAME, "IHE Actors");
        auditSourceTypeCode.setAttribute(DISPLAY_NAME, "IHE Document Repository");

        auditMessage.appendChild(auditSourceIdentification);

        //To which patient?
        final Element participantObjectIdentification1 = auditDocument.createElement(PARTICIPANT_OBJECT_IDENTIFICATION);
        participantObjectIdentification1.setAttribute(PARTICIPANT_OBJECT_DATA_LIFE_CYCLE, "1");
        participantObjectIdentification1.setAttribute(PARTICIPANT_OBJECT_ID, "");
        participantObjectIdentification1.setAttribute(PARTICIPANT_OBJECT_TYPE_CODE, "1");
        participantObjectIdentification1.setAttribute(PARTICIPANT_OBJECT_TYPE_CODE_ROLE, "1");
        final Element participantObjectIdTypeCode1 = auditDocument.createElement(PARTICIPANT_OBJECT_ID_TYPE_CODE);
        participantObjectIdentification1.appendChild(participantObjectIdTypeCode1);
        participantObjectIdTypeCode1.setAttribute(CODE, "2");
        participantObjectIdTypeCode1.setAttribute(CODE_SYSTEM, "");
        participantObjectIdTypeCode1.setAttribute(CODE_SYSTEM_NAME, "RFC-3881");
        participantObjectIdTypeCode1.setAttribute(DISPLAY_NAME, "Patient Number");

        // To which resource?
        final Element participantObjectIdentification2 = auditDocument.createElement(PARTICIPANT_OBJECT_IDENTIFICATION);
        participantObjectIdentification2.setAttribute(PARTICIPANT_OBJECT_ID, "1.2.40.0.13.1.1.2117081378.20130402104335703.34529");
        participantObjectIdentification2.setAttribute(PARTICIPANT_OBJECT_TYPE_CODE, "2");
        participantObjectIdentification2.setAttribute(PARTICIPANT_OBJECT_TYPE_CODE_ROLE, "3");
        final Element participantObjectIdTypeCode2 = auditDocument.createElement(PARTICIPANT_OBJECT_ID_TYPE_CODE);
        participantObjectIdentification2.appendChild(participantObjectIdTypeCode2);
        participantObjectIdTypeCode2.setAttribute(CODE, "9");
        participantObjectIdTypeCode2.setAttribute(CODE_SYSTEM, "");
        participantObjectIdTypeCode2.setAttribute(CODE_SYSTEM_NAME, "RFC-3881");
        participantObjectIdTypeCode2.setAttribute(DISPLAY_NAME, "Report Number");

        final Element participantObjectIdentification3 = auditDocument.createElement(PARTICIPANT_OBJECT_IDENTIFICATION);
        participantObjectIdentification3.setAttribute(PARTICIPANT_OBJECT_DATA_LIFE_CYCLE, "9");
        participantObjectIdentification3.setAttribute(PARTICIPANT_OBJECT_ID, "17d77343-88b5-4aad-8d37-ce68ce720060");
        participantObjectIdentification3.setAttribute(PARTICIPANT_OBJECT_TYPE_CODE, "2");
        participantObjectIdentification3.setAttribute(PARTICIPANT_OBJECT_TYPE_CODE_ROLE, "3");
        final Element participantObjectIdTypeCode3 = auditDocument.createElement(PARTICIPANT_OBJECT_ID_TYPE_CODE);
        participantObjectIdentification3.appendChild(participantObjectIdTypeCode3);
        participantObjectIdTypeCode3.setAttribute(CODE, auditValueMap.get(EVENT_TYPE_CODE).trim().isEmpty() ? N_A : auditValueMap.get(EVENT_TYPE_CODE).trim());
        participantObjectIdTypeCode3.setAttribute(CODE_SYSTEM, "");
        participantObjectIdTypeCode3.setAttribute(CODE_SYSTEM_NAME, "IHE Transactions");
        participantObjectIdTypeCode3.setAttribute(DISPLAY_NAME, "Retrieve Document Set");
        final Element participantObjectName = auditDocument.createElement("ParticipantObjectName");
        participantObjectName.setTextContent("Transaction ID");
        participantObjectIdentification3.appendChild(participantObjectName);
        final Element participantObjectDetail = auditDocument.createElement("ParticipantObjectDetail");
        participantObjectDetail.setAttribute("type", "DURATION");
        participantObjectDetail.setAttribute("value", "Mg==");

        participantObjectIdentification3.appendChild(participantObjectDetail);

        auditMessage.appendChild(participantObjectIdentification1);
        auditMessage.appendChild(participantObjectIdentification2);
        auditMessage.appendChild(participantObjectIdentification3);

        setMessage(auditMessage.getOwnerDocument());
    }

    private void fillHash(final String attributeId, final List<Object> content) {
        // +1 is the colon
        auditValueMap.put(attributeId.substring(ATNA_PREFIX.length() + 1), (String) content.get(0));
    }

    @Override
    public Document getMessage() {
        return audit;
    }

    private void setMessage(final Document audit) {
        this.audit = audit;
    }
}
