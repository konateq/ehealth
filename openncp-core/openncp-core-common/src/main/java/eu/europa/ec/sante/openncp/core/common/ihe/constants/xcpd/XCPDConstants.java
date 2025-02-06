package eu.europa.ec.sante.openncp.core.common.ihe.constants.xcpd;


import eu.europa.ec.sante.openncp.core.common.ihe.constants.IheConstants;

/**
 * Holds all the fixed properties used in the XCPD Profile transactions.
 *
 */
public class XCPDConstants {

    public static final String ITS_VERSION = "XML_1.0";
    public static final String HL7_VERSION = "V3PR1";
    public static final String INTERACTION_IDS_NAMESPACE = "2.16.840.1.113883.1.6";
    public static final String PATIENT_DISCOVERY_REQUEST = "PRPA_IN201305UV02";
    public static final String PATIENT_DISCOVERY_RESPONSE = "PRPA_IN201306UV02";

    /* Processing Code */
    public static final String PROCESSING_CODE = "P";
    public static final String PROCESSING_MODE_CODE = "T";

    public static final String ACCEPT_ACK_CODE = "AL";

    public static final String DETERMINER_CODE_INSTANCE = "INSTANCE";

    /* Class Code */
    public static final String CLASS_CODE_AGNT = "AGNT";
    public static final String CLASS_CODE_ORG = "ORG";
    public static final String CLASS_CODE_ASSIGNED = "ASSIGNED";

    public static final String HL7_V3_NAMESPACE_URI = "urn:hl7-org:v3";

    public class LOG {

        /* ProvideAndRegister */
        public static final String OUTGOING_XCPD_MESSAGE = "Outgoing XCPD request message to NCP-A: {}";
        public static final String INCOMING_XCPD_MESSAGE = "Incoming XCPD response message from NCP-A: {}";
    }

    public class CONTROL_ACT_PROCESS {

        public static final String CODE = "PRPA_TE201305UV02";
        public static final String QUERY_BY_PARAMETER_ID_ROOT = "1.263507841149";
        public static final String QUERY_BY_PARAMETER_STATUS_CODE = "new";
        public static final String QUERY_BY_PARAMETER_RESPONSE_MODALITY_CODE = "R";
        public static final String QUERY_BY_PARAMETER_RESPONSE_PRIORITY_CODE = "I";
        public static final String QUERY_BY_PARAMETER_LIVING_SUBJECT_NAME_SEMANTICS = "LivingSubject.name";
        public static final String QUERY_BY_PARAMETER_LIVING_SUBJECT_ID_SEMANTICS = "LivingSubject.id";
        public static final String QUERY_BY_PARAMETER_ADMINISTRATIVE_GENDER_SEMANTICS = "LivingSubject.administrativeGender";
        public static final String QUERY_BY_PARAMETER_BIRTHDATE_SEMANTICS = "LivingSubject.birthTime";
        public static final String QUERY_BY_PARAMETER_PATIENT_ADDRESS_SEMANTICS = "Patient.addr";
    }

    public class SOAP_HEADERS {

        /* Mixed */
        public static final String REQUEST_ACTION = "urn:hl7-org:v3:PRPA_IN201305UV02:CrossGatewayPatientDiscovery";
        public static final String NAMESPACE_URI = "urn:ihe:iti:xcpd:2009";
        public static final String MUST_UNDERSTAND = "mustUnderstand";
        public static final String OM_NAMESPACE = "http://www.w3.org/2005/08/addressing";
        public static final String SECURITY_XSD = IheConstants.SOAP_HEADERS.SECURITY_XSD;

        /* Request */
        public static final String NAMESPACE_REQUEST_LOCAL_PART = "respondingGateway_PRPA_IN201305UV02";

        /* Response */
        // Insert response hard-coded properties here.
    }
}
