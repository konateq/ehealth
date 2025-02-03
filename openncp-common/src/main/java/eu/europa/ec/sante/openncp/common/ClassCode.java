package eu.europa.ec.sante.openncp.common;

public enum ClassCode {

    EP_CLASSCODE("57833-6"),
    PS_CLASSCODE("60591-5"),
    EDD_CLASSCODE("DISCARD-60593-1"),
    ED_CLASSCODE("60593-1"),
    MRO_CLASSCODE("56445-0"),
    ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE("34105-7"),
    ORCD_LABORATORY_RESULTS_CLASSCODE("11502-2"),
    ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE("18748-4"),
    ORCD_MEDICAL_IMAGES_CLASSCODE("x-clinical-image"),
    CONSENT_CLASSCODE("57016-8"),
    HCER_CLASSCODE("34133-9"),
    LABORATORY_RESULT_REPORTS("11502-2");

    private final String code;

    ClassCode(String code) {
        this.code = code;
    }

    public static ClassCode getByCode(String code) {
        for (ClassCode classCode : values()) {
            if (classCode.code.equals(code)) {
                return classCode;
            }
        }
        throw new IllegalArgumentException("Unknown class code : " + code);
    }

    public String getCode() {
        return code;
    }
}
