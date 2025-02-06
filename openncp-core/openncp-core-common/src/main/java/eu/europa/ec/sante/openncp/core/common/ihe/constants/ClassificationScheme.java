package eu.europa.ec.sante.openncp.core.common.ihe.constants;

public enum ClassificationScheme {

    CLASS_CODE("urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a"),
    CONFIDENTIALITY("urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f"),
    FORMAT_CODE("urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d"),
    HEALTHCARE_FACILITY_CODE("urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1"),
    PATIENT_ID("urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427"),
    PRACTICE_SETTING_CODE("urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead"),
    TYPE_CODE("urn:uuid:f0306f51-975f-434e-a61c-c59651d33983"),
    UNIQUE_ID("urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab");

    private final String uuid;

    ClassificationScheme(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}
