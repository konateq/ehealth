package eu.europa.ec.sante.openncp.core.common.ihe.exception;

import eu.europa.ec.sante.openncp.common.error.ErrorCode;

import java.util.Arrays;

public enum XCPDErrorCode implements ErrorCode {

    //EHI Error Code
    /*
     * https://profiles.ihe.net/ITI/TF/Volume2/ITI-55.html
     * Table 3.55.4.2.2.7-1: Coded values for codeSystem=1.3.6.1.4.1.19376.1.2.27.3
     */

    AnswerNotAvailable("AnswerNotAvailable", "The answer is not available. Human intervention may be needed.", "1.3.6.1.4.1.19376.1.2.27.3"),
    ResponderBusy("ResponderBusy", "The responder was not able to process the request because it is currently overloaded.", "1.3.6.1.4.1.19376.1.2.27.3"),
    InternalError("InternalError", "The responder was not able to respond due to an internal error or inconsistency.", "1.3.6.1.4.1.19376.1.2.27.3"),

    //EHDSI Error code
    /*
     * XCPD Profile Technical Specification
     * https://webgate.ec.europa.eu/fpfis/wikis/x/3eTzN
     */

    AdditionalDemographicsRequested("AdditionalDemographicsRequested", "The service requestor tried an identification based on an ID only or did not provide enough data to univocally identify the patient.", "1.3.6.1.4.1.12559.11.10.1.3.2.2.1"),
    DemographicsQueryNotAllowed("DemographicsQueryNotAllowed", "The service  provider  only allows  for patient identification by national/shared ID.", "1.3.6.1.4.1.12559.11.10.1.3.2.2.1"),
    EHICDataRequested("EHICDataRequested", "The service provider only allows for patient identification by national health card or EHIC. Queries based on demographics only are not supported.", "1.3.6.1.4.1.12559.11.10.1.3.2.2.1"),
    PrivacyViolation("PrivacyViolation", "The service provider does not accept he query because responding MAY lead to a disclosure of private patient data.", "1.3.6.1.4.1.12559.11.10.1.3.2.2.1"),
    InsufficientRights("InsufficientRights", "The requestor has insufficient rights to query for patient’s identity data.", "1.3.6.1.4.1.12559.11.10.1.3.2.2.1"),
    NationalInfrastructure("NationalInfrastructure", "National infrastructure error occured while querying for patient’s identity data.", "1.3.6.1.4.1.12559.11.10.1.3.2.2.1"),
    PatientAuthenticationRequired("PatientAuthenticationRequired", "A respective identifier (e.g. GSS TAN)was not provided.", "1.3.6.1.4.1.12559.11.10.1.3.2.2.1"),
    PolicyViolation("PolicyViolation", "The service consumer defined a confidence level that conflicts with the security policy of the service provider.", "1.3.6.1.4.1.12559.11.10.1.3.2.2.1");

    private final String code;
    private final String description;

    private final String codeSystem;

    XCPDErrorCode(String code, String description, String codeSystem) {
        this.code = code;
        this.description = description;
        this.codeSystem = codeSystem;
    }

    public static XCPDErrorCode getErrorCode(String code) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.name().equals(code))
                .findAny()
                .orElse(null);
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return description;
    }

    public String getCodeSystem() {
        return codeSystem;
    }
}
