package eu.europa.ec.sante.ehdsi.constant.error;

import java.util.Arrays;

public enum OpenNCPErrorCode implements ErrorCode {

    ERROR_GENERIC("ERROR_GENERIC", "Internal Server Error - Please retry or contact the server administrator and inform them of the time the error occurred."),
    WARNING_GENERIC("WARNING_GENERIC", "Internal Server Warning."),
    ERROR_GENERIC_CONNECTION_NOT_POSSIBLE("ERROR_GENERIC_CONNECTION_NOT_POSSIBLE", "The Country of Treatment (Country B) is unable to contact the Patient Country of Affiliation."),
    ERROR_GENERIC_DOCUMENT_MISSING("ERROR_GENERIC_DOCUMENT_MISSING", "The document requested was not found."),
    ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN("ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN", "Class code not supported."),

    // 01. Ensure Health Professional (HP) Identification, Authentication and Authorization
    ERROR_HPI_GENERIC("ERROR_HPI_GENERIC", "A generic error has been detected regarding the Health Professional Identification."),
    WARNING_HPI_GENERIC("WARNING_HPI_GENERIC", "A generic warning has been detected regarding the Health Professional Identification."),

    ERROR_HPI_NO_INFORMATION("ERROR_HPI_NO_INFORMATION", "No information about the Health Professional has been provided."),

    ERROR_HPI_INSUFFICIENT_INFORMATION("ERROR_HPI_INSUFFICIENT_INFORMATION", "The information provided about the HP is insufficient."),
    ERROR_HPI_HAI_NO_INFORMATION("ERROR_HPI_HAI_NO_INFORMATION", "No information about the Health Authorities Institutions and/or Healthcare Provider Organization has been provided."),
    ERROR_HPI_POC_NO_INFORMATION("ERROR_HPI_POC_NO_INFORMATION", "No information about the organisation or healthcare facility has been provided."),
    ERROR_HPI_AUTHENTICATION_NOT_RECEIVED("ERROR_HPI_AUTHENTICATION_NOT_RECEIVED", "The Health Professional authentication method has not been received by the Country of Affiliation."),

    // 02. Ensure Patient Identification
    ERROR_PI_GENERIC("ERROR_PI_GENERIC", "A generic error has been detected regarding the Patient Identification."),
    WARNING_PI_GENERIC("WARNING_PI_GENERIC", "A generic warning has been detected regarding the Patient Identification."),
    ERROR_PI_NO_MATCH("ERROR_PI_NO_MATCH", "The offered identification and authentication data of the patient could not be found in Country A."),
    ERROR_PI_MULTIPLE_MATCHES("ERROR_PI_MULTIPLE_MATCHES", "The offered identification and authentication data return multiple matches. Please provide additional identification data to narrow down the results to one."),

    // 05. Make Patient Summary available to HP
    ERROR_PS_GENERIC("ERROR_PS_GENERIC", "A generic error has been detected regarding the Patient Summary."),
    WARNING_PS_GENERIC("WARNING_PS_GENERIC", "A generic warning has been detected regarding the Patient Summary."),
    ERROR_PS_MISSING_BASIC_SECTIONS("ERROR_PS_MISSING_BASIC_SECTIONS", "Any or all of the 5 basic sections of the Patient Summary are missing, and no reason is communicated for this missing."),
    WARNING_PS_MISSING_BASIC_DATA("WARNING_PS_MISSING_BASIC_DATA", "Any or all of the Patient Summary basic data set is missing."),
    ERROR_PS_MISSING_EXPECTED_MAPPING("ERROR_PS_MISSING_EXPECTED_MAPPING", "An expected code mapping of the Patient Summary is missing."),
    WARNING_PS_MISSING_EXPECTED_MAPPING("WARNING_PS_MISSING_EXPECTED_MAPPING", "An expected code mapping of the Patient Summary is missing."),
    WARNING_PS_MISSING_EXPECTED_TRANSLATION("EHDSI_WARNING_PS_MISSING_EXPECTED_TRANSLATION", "An expected translation of the Patient Summary is missing."),
    ERROR_PS_PDF_FORMAT_NOT_PROVIDED("ERROR_PS_PDF_FORMAT_NOT_PROVIDED", "No PDF version of the Patient Summary has been provided by the Country of Affiliation."),

    // 06. Make ePrescription available to HP
    ERROR_EP_GENERIC("ERROR_EP_GENERIC", "A generic error has been detected regarding the ePrescription."),
    WARNING_EP_GENERIC("WARNING_EP_GENERIC", "A generic warning has been detected regarding the ePrescription."),
    WARNING_EP_MISSING_BASIC_DATA("WARNING_EP_MISSING_BASIC_DATA", "Any or all of the ePrescription basic data set is missing, and no reason is communicated for this missing."),
    ERROR_EP_MISSING_EXPECTED_MAPPING("ERROR_EP_MISSING_EXPECTED_MAPPING", "An expected code mapping of the ePrescription is missing."),
    WARNING_EP_MISSING_EXPECTED_MAPPING("WARNING_EP_MISSING_EXPECTED_MAPPING", "An expected code mapping of the ePrescription is missing."),
    WARNING_EP_MISSING_EXPECTED_TRANSLATION("WARNING_EP_MISSING_EXPECTED_TRANSLATION", "An expected translation of the ePrescription is missing."),
    ERROR_EP_PDF_FORMAT_NOT_PROVIDED("ERROR_EP_PDF_FORMAT_NOT_PROVIDED", "No PDF version of the ePrescription has been provided by the Country of Affiliation."),

    // 07. Handle Dispensation of medicine and Substitution
    ERROR_ED_GENERIC("ERROR_ED_GENERIC", "A generic error has been detected regarding the eDispensation."),
    WARNING_ED_GENERIC("WARNING_ED_GENERIC", "A generic warning has been detected regarding the eDispensation."),
    ERROR_ED_NOT_DISPENSABLE_EPRESCRIPTION("ERROR_ED_NOT_DISPENSABLE_EPRESCRIPTION", "The selected ePrescription is not dispensable."),
    ERROR_ED_SUBSTITUTION_INFORMATION_NOT_ALLOWED("ERROR_ED_SUBSTITUTION_INFORMATION_NOT_ALLOWED", "The Patient Country of Affiliation has received an information of substitution when it was not allowed based on its national legislation."),
    ERROR_ED_EPRESCRIPTION_NOT_IDENTIFIABLE("ERROR_ED_EPRESCRIPTION_NOT_IDENTIFIABLE", "The necessary information to  identify the original  ePrescription and the related medicinal product is missing."),
    WARNING_ED_MISSING_BASIC_DATA("WARNING_ED_MISSING_BASIC_DATA", "Any or all of the eDispensation basic data set is missing, and no reason has been communicated for this missing."),
    ERROR_ED_INFORMATION_NOT_STORED("ERROR_ED_INFORMATION_NOT_STORED", "Information of the eDispensation could not be stored by the Country of Treatment. The eDispensation discard optionality will not be applicable to the eDispensation."),
    ERROR_ED_MISSING_EXPECTED_MAPPING("ERROR_ED_MISSING_EXPECTED_MAPPING", "An expected code mapping of the eDispensation is missing."),
    WARNING_ED_MISSING_EXPECTED_MAPPING("WARNING_ED_MISSING_EXPECTED_MAPPING", "An expected code mapping of the eDispensation is missing."),
    ERROR_ED_EDISPENSATION_ACK_NOT_RECEIVED("ERROR_ED_EDISPENSATION_ACK_NOT_RECEIVED", "A new request for available ePrescriptions cannot be done before the dispensation acknowledgement from Country A has been received."),
    ERROR_ED_EPRESCRIPTION_NOT_UPDATED("ERROR_ED_EPRESCRIPTION_NOT_UPDATED", "Some of the ePrescription(s) could not be updated with the dispensed medicine information."),
    ERROR_ED_EDISPENSATION_NOT_STORED("ERROR_ED_EDISPENSATION_NOT_STORED", "Information of the eDispensation could not be stored by the Country of Affiliation. The eDispensation discard optionality will not be applicable to the eDispensation."),
    ERROR_ED_DISCARD_SERVICE_NOT_AVAILABLE("ERROR_ED_DISCARD_SERVICE_NOT_AVAILABLE", "eDispensation Discard service of the Country of Affiliation could not be contacted."),
    ERROR_ED_DISCARD_FAILED("ERROR_ED_DISCARD_FAILED", "The eDispensation Discard could not be performed."),
    WARNING_ED_DISCARD_MISSING_MANDATORY_DATA("WARNING_ED_DISCARD_MISSING_MANDATORY_DATA", "Mandatory elements of the eDispensation discard are missing."),

    // 08. Make Original Clinical Documents available to HP
    ERROR_ORCD_GENERIC("ERROR_ORCD_GENERIC", "A generic error has been detected regarding the Original Clinical Document."),
    WARNING_ORCD_GENERIC("WARNING_ORCD_GENERIC", "A generic warning has been detected regarding the Original Clinical Document."),
    ERROR_ORCD_SERVICE_NOT_AVAILABLE("ERROR_ORCD_SERVICE_NOT_AVAILABLE", "\tThe Original Clinical Document service of the Country of Affiliation could not be contacted."),
    WARNING_ORCD_MISSING_MANDATORY_DATA("WARNING_ORCD_MISSING_MANDATORY_DATA", "Mandatory data of the Original Clinical Document are missing."),
    ERROR_ORCD_INCORRECT_FORMAT("ERROR_ORCD_INCORRECT_FORMAT", "The file format is not acceptable. The file has not been transmitted."),
    WARNING_ORCD_MISSING_MANDATORY_METADATA("WARNING_ORCD_MISSING_MANDATORY_METADATA", "Some or all of the mandatory metadata of the Original Clinical Document are missing."),

    // 09. Ensure high quality information (structured, equivalent, understandable) is exchanged between countries
    ERROR_HQI_GENERIC("ERROR_HQI_GENERIC", "A generic error has been detected regarding the High Quality Information requirement."),
    WARNING_HQI_GENERIC("WARNING_HQI_GENERIC", "A generic warning has been detected regarding the High Quality Information requirement."),
    WARNING_HQI_B_INFORMATION_NOT_PROPERLY_STRUCTURED("WARNING_HQI_B_INFORMATION_NOT_PROPERLY_STRUCTURED", "Information provided by the Country of Treatment is not properly structured."),
    WARNING_HQI_A_INFORMATION_NOT_PROPERLY_STRUCTURED("WARNING_HQI_A_INFORMATION_NOT_PROPERLY_STRUCTURED", "The information provided by the Country of Affiliation is not properly structured."),

    // 10. Ensure the security, performance, traceability and auditability of the services, data and systems
    ERROR_SEC_GENERIC("ERROR_SEC_GENERIC", "A generic error has been detected regarding the Information Security."),
    WARNING_SEC_GENERIC("WARNING_SEC_GENERIC", "A generic warning has been detected regarding the Information Security."),

    ERROR_SEC_DATA_INTEGRITY_NOT_ENSURED("ERROR_SEC_DATA_INTEGRITY_NOT_ENSURED", "Integrity of the exchanged data cannot be ensured by the Country of Affiliation, such as with an insufficient/invalidated SAML assertions."),
    WARNING_SEC_UNEXPECTED_NUMBER_OF_REQUESTS_FOR_UNIQUE_PATIENT("WARNING_SEC_UNEXPECTED_NUMBER_OF_REQUESTS_FOR_UNIQUE_PATIENT", "An unexpected number of requests has been detected for one specific patient."),
    WARNING_SEC_UNEXPECTED_NUMBER_OF_REQUESTS_FOR_UNIQUE_POINT_OF_CARE("WARNING_SEC_UNEXPECTED_NUMBER_OF_REQUESTS_FOR_UNIQUE_POINT_OF_CARE", "An unexpected number of requests has been detected from one specific Point of Care."),
    WARNING_SEC_UNEXPECTED_NUMBER_OF_REQUESTS("WARNING_SEC_UNEXPECTED_NUMBER_OF_REQUESTS", "An unexpected number of requests has been detected."),

    //  Old MyHealth@EU error codes
    ERROR_NOT_VALID_ASSERTION("1001", "Assertion is not valid."),
    ERROR_NOT_VALID_TRC_ASSERTION("1002", "The given TRC Assertion does not validate against the Identity Assertion."),
    ERROR_EP_NOT_FOUND("1101", "No ePrescription is registered for the given patient."),
    ERROR_PS_NOT_FOUND("1102", "No PS is registered for the given patient."),
    ERROR_MRO_NO_DATA("1103", "No Data for MRO."),
    ERROR_ORCD_NOT_FOUND("1104", "No ORCD document is registered for the given patient."),
    ERROR_DOCUMENT_NOT_FOUND("1100", "No documents are registered for the given patient."),
    ERROR_DOCUMENT_NOT_PROCESSED("2201", "Documents were received but not processed."),
    ERROR_RENDERING_INCOMPLETE("4101", "Rendering Incomplete."),
    ERROR_COLLECTION_INCOMPLETE("4102", "Collection incomplete."),
    ERROR_EP_REGISTRY_NOT_ACCESSIBLE("4103", "ePrescription registry could not be accessed."),
    ERROR_EP_NOT_AVAILABLE("4104", "Data Access Failure."),
    ERROR_EP_NOT_MATCHING("4105", "No matching ePrescription was found."),
    ERROR_EP_ALREADY_DISPENSED("4106", "ePrescription has already been dispensed."),
    ERROR_ORIGINAL_DATA_MISSING("4107", "For data of the given kind the Provide Data service provider requires the service consumer to transmit the source coded PDF document."),
    ERROR_PIVOT_MISSING("4108", "The service consumer did not provide the eHealth DSI pivot coded document which is requested by the Provide Data service provider for the given kind of data."),
    ERROR_UNSUPPORTED_FEATURE("4201", "If PDF-coded document is requested: Country A does not provide the (optional) source coded version of the document."),
    ERROR_TRANSCODING_ERROR("4203", "The requested encoding cannot be provided due to a transcoding error."),
    ERROR_UNKNOWN_FILTER("4204", "The service provider is unable to evaluate the given argument values."),
    ERROR_UNKNOWN_OPTION("4205", "The service provider does not implement the On Demand Documents option as requested by the service consumer."),
    ERROR_UNKNOWN_PATIENT_IDENTIFIER("4206", "The patient identifier in the query differs from the patient identifier in the TRC assertion."),
    ERROR_4207("4207", "The extra parameters linked to the document identifier in the query differs from the ones in the TRC assertion."),
    ERROR_NO_CONSENT("4701", "The patient has not given consent to the requested service."),
    ERROR_WEAK_AUTHENTICATION("4702", "Country A requests a higher authentication trust level than assigned to the HP."),
    ERROR_INSUFFICIENT_RIGHTS("4703", "Either the security policy of country A or a privacy policy of the patient (that was given in country A) does not allow the requested operation to be performed by the HP."),
    ERROR_NO_SIGNATURE("4704", "The Provide Data service provider only accepts data of the given kind if it is digitally signed by an HP."),
    ERROR_POLICY_VIOLATION("4705", "Policy violation."),
    ERROR_UNKNOWN_POLICY("4706", "Unknown Policy."),
    ERROR_UNKNOWN_SIGNIFIER("4202", "Class code not supported.");

    private final String code;
    private final String description;

    OpenNCPErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OpenNCPErrorCode getErrorCode(String code) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.getCode().equals(code))
                .findAny()
                .orElse(null);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getCodeSystem() {
        return null;
    }
}
