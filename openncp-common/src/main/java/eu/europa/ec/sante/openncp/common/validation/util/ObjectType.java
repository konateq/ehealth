package eu.europa.ec.sante.openncp.common.validation.util;

public enum ObjectType {

    ASSERTION("ASSERTION"),
    AUDIT("AUDIT"),
    CDA("CDA"),
    FHIR("FHIR"),
    PDQ("PDQ"),
    XCA_QUERY_REQUEST("XCA-QUERY-REQUEST"),
    XCA_QUERY_RESPONSE("XCA-QUERY-RESPONSE"),
    XCA_RETRIEVE_REQUEST("XCA-RETRIEVE-REQUEST"),
    XCA_RETRIEVE_RESPONSE("XCA-RETRIEVE-RESPONSE"),
    XCF_REQUEST("XCF-REQUEST"),
    XCF_RESPONSE("XCF-RESPONSE"),
    XCPD_QUERY_REQUEST("XCPD-QUERY-REQUEST"),
    XCPD_QUERY_RESPONSE("XCPD-QUERY-RESPONSE"),
    XDR_SUBMIT_REQUEST("XDR-SUBMIT-REQUEST"),
    XDR_SUBMIT_RESPONSE("XDR-SUBMIT-RESPONSE");

    private final String name;

    ObjectType(final String s) {
        name = s;
    }

    @Override
    public String toString() {
        return name;
    }
}
