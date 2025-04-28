package eu.europa.ec.sante.openncp.core.common.ihe.eadc;

public enum ServiceType {

    //SPECIFICATION
    PATIENT_IDENTIFICATION_QUERY("PATIENT_IDENTIFICATION_QUERY"),
    PATIENT_IDENTIFICATION_RESPONSE("PATIENT_IDENTIFICATION_RESPONSE"),
    DOCUMENT_LIST_QUERY("DOCUMENT_LIST_QUERY"),
    DOCUMENT_LIST_RESPONSE("DOCUMENT_LIST_RESPONSE"),
    DOCUMENT_EXCHANGED_QUERY("DOCUMENT_EXCHANGED_QUERY"),
    DOCUMENT_EXCHANGED_RESPONSE("DOCUMENT_EXCHANGED_RESPONSE"),
    DISPENSATION_QUERY("DISPENSATION_QUERY"),
    DISPENSATION_RESPONSE("DISPENSATION_RESPONSE"),
    FHIR_SEARCH("FHIR_SEARCH"),
    FHIR_READ("FHIR_READ");

    private final String description;

    ServiceType(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
