package eu.europa.ec.sante.openncp.common.audit;

import eu.europa.ec.sante.openncp.common.ClassCode;

import java.util.List;

/**
 * Enumeration for populating the EventType of the AuditMessage.
 */
public enum TransactionName {

    IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS("IdentityService::FindIdentityByTraits"),
    PATIENT_SERVICE_LIST("PatientService::List"),
    PATIENT_SERVICE_RETRIEVE("PatientService::Retrieve"),
    ORDER_SERVICE_LIST("OrderService::List"),
    ORDER_SERVICE_RETRIEVE("OrderService::Retrieve"),
    ORCD_SERVICE_LIST("OrCDService::List"),
    ORCD_SERVICE_RETRIEVE("OrCDService::Retrieve"),
    DISPENSATION_SERVICE_INITIALIZE("DispensationService::Initialize"),
    DISPENSATION_SERVICE_DISCARD("DispensationService::Discard"),
    CONSENT_SERVICE_PUT("ConsentService::Put"),
    CONSENT_SERVICE_DISCARD("ConsentService::Discard"),
    CONSENT_SERVICE_PIN("ConsentService::PIN"),
    HCP_AUTHENTICATION("identityProvider::HPAuthentication"),
    TRC_ASSERTION("ncp::TrcAssertion"),
    NOK_ASSERTION("ncp::NokAssertion"),
    NCP_TRUSTED_SERVICE_LIST("ncpConfigurationManager::ImportNSL"),
    PIVOT_TRANSLATION("ncpTransformationMgr::Translate"),
    COMMUNICATION_FAILURE("CommunicationFailure"),
    SMP_QUERY("SMP::Query"),
    SMP_PUSH("SMP::Push");

    private final String code;

    TransactionName(String c) {
        code = c;
    }

    public String getCode() {
        return code;
    }

    public static TransactionName determineTransactionNameForXCARetrieve(ClassCode classCode) {
        switch (classCode) {
            case PS_CLASSCODE:
                return TransactionName.PATIENT_SERVICE_RETRIEVE;
            case EP_CLASSCODE:
                return TransactionName.ORDER_SERVICE_RETRIEVE;
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
                return TransactionName.ORCD_SERVICE_RETRIEVE;
        }
        throw new RuntimeException(String.format("TransactionName for XCA Retrieve cannot be determined based on classCode [{}]", classCode));
    }

    public static TransactionName determineTransactionNameForXCAQuery(List<ClassCode> classCodes) {
        for (ClassCode classCode : classCodes) {
            switch (classCode) {
                case PS_CLASSCODE:
                    return TransactionName.PATIENT_SERVICE_LIST;
                case EP_CLASSCODE:
                    return TransactionName.ORDER_SERVICE_LIST;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    return TransactionName.ORCD_SERVICE_LIST;
            }
        }
        throw new RuntimeException(String.format("TransactionName for XCA List cannot be determined based on classCodes [%s]", classCodes));
    }
}
