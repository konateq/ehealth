package eu.europa.ec.sante.openncp.core.common.ihe.constants;

/**
 * Holds multiple IHE Constants.
 *
 * @author Marcelo Fonseca<code> - marcelo.fonseca@iuz.pt</code>
 * @author Luís Pinto<code> - luis.pinto@iuz.pt</code>
 */
public final class IheConstants {

    public static final String FORMAT_CODE_SCHEME = "urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d";
    public static final String TYPE_CODE_SCHEME = "urn:uuid:f0306f51-975f-434e-a61c-c59651d33983";
    /**
     * @deprecated (use { @ link # eu.europa.ec.sante.ehdsi.openncp.pt.common.AdhocQueryResponseStatus.SUCCESS } instead.)
     */
    @Deprecated
    public static final String REGREP_RESPONSE_SUCCESS = "urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success";
    /**
     * @deprecated (use { @ link # eu.europa.ec.sante.ehdsi.openncp.pt.common.AdhocQueryResponseStatus.PARTIAL_SUCCESS } instead.))
     */
    @Deprecated
    public static final String REGREP_RESPONSE_PARTIALSUCCESS = "urn:ihe:iti:2007:ResponseStatusType:PartialSuccess";
    /**
     * @deprecated (use { @ link # eu.europa.ec.sante.ehdsi.openncp.pt.common.AdhocQueryResponseStatus.FAILURE } instead.))
     */
    @Deprecated
    public static final String REGREP_RESPONSE_FAILURE = "urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Failure";
    public static final String REGREP_STATUSTYPE_APPROVED = "urn:oasis:names:tc:ebxml-regrep:StatusType:Approved";
    public static final String CLASSCODE_SCHEME = "2.16.840.1.113883.6.1";
    public static final String DISPENSATION_FORMATCODE_DISPLAYNAME = "eHDSI coded eDispensation";
    public static final String DISPENSATION_FORMATCODE_NODEREPRESENTATION = "urn:epsos:ep:dis:2010";
    public static final String DISPENSATION_FORMATCODE_CODINGSCHEMA = "eHDSI formatCodes";
    public static final String CONSENT_FORMATCODE_DISPLAYNAME = "Consent";
    public static final String CONSENT_FORMATCODE_NODEREPRESENTATION = "urn:ihe:iti:bppc:2007";
    public static final String CONSENT_FORMATCODE_CODINGSCHEMA = "2.16.840.1.113883.6.1";

    /* Classification Scheme */
    public static final String CLASSIFICATION_SCHEME_AUTHOR_UUID = "urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d";
    public static final String CLASSIFICATION_EVENT_CODE_LIST = "urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4";


    /* Slot name */
    public static final String AUTHOR_INSTITUTION_STR = "authorInstitution";
    public static final String AUTHOR_PERSON_STR = "authorPerson";
    public static final String AUTHOR_SPECIALITY_STR = "authorSpecialty";

    private IheConstants() {
    }

    public static class SOAP_HEADERS {

        public static final String SECURITY_XSD = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

        private SOAP_HEADERS() {
        }
    }
}
