package eu.europa.ec.sante.openncp.common.audit;

import eu.europa.ec.sante.openncp.common.ClassCode;
import net.RFC3881.dicom.AuditMessage;

import eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders.*;

import java.util.List;

public enum EventType {

    IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS("EHDSI-11", "110112","ITI-55", "XCPD::CrossGatewayPatientDiscovery", new IdentificationServiceAuditMessageBuilder()),
    PATIENT_SERVICE_LIST("EHDSI-21", "110112","ITI-38", "XCA::CrossGatewayQuery", new XCAListAuditMessageBuilder()),
    PATIENT_SERVICE_RETRIEVE("EHDSI-22", "110106","ITI-39", "XCA::CrossGatewayRetrieve", new XCARetrieveAuditMessageBuilder()),
    ORDER_SERVICE_LIST("EHDSI-31", "110112","ITI-38", "XCA::CrossGatewayQuery", new XCAListAuditMessageBuilder()),
    ORDER_SERVICE_RETRIEVE("EHDSI-32","110112","ITI-39", "XCA::CrossGatewayRetrieve", new XCARetrieveAuditMessageBuilder()),
    ORCD_SERVICE_LIST("EHDSI-61", "110112","ITI-38", "XCA::CrossGatewayQuery", new XCAListAuditMessageBuilder()),
    ORCD_SERVICE_RETRIEVE("EHDSI-62","110112","ITI-39", "XCA::CrossGatewayRetrieve", new XCARetrieveAuditMessageBuilder()),
    DISPENSATION_SERVICE_INITIALIZE("EHDSI-41", "110112","ITI-41", "XDR::ProvideandRegisterDocumentSet-b", new DispensationServiceAuditMessageBuilder()),
    DISPENSATION_SERVICE_DISCARD("EHDSI-42", "110112","ITI-41", "XDR::ProvideandRegisterDocumentSet-b", new DispensationDiscardServiceAuditMessageBuilder()),
    HCP_AUTHENTICATION("EHDSI-91", "110112","ITI-40", "XUA::ProvideX-UserAssertion", new HCPAuthenticationAuditMessageBuilder()),
    TRC_ASSERTION("EHDSI-92", "110112","EHDSI-92", "ncp::TrcAssertion", new TRCAssertionAuditMessageBuilder()),
    NOK_ASSERTION("EHDSI-96", "110112","EHDSI-96", "ncp::NokAssertion", new NOKAssertionAuditMessageBuilder()),
    NCP_TRUSTED_SERVICE_LIST("EHDSI-93", "110112","EHDSI-93", "ncpConfigurationManager::ImportNSL", new NCPTrustedServiceListAuditMessageBuilder()),
    PIVOT_TRANSLATION("EHDSI-94", "110112","EHDSI-94", "ncpTransformationMgr::Translate", new PivotTranslationAuditMessageBuilder()),
    SMP_QUERY("EHDSI-193", "110112","EHDSI-193", "SMP::Query", new SMPAuditMessageBuilder()),
    SMP_PUSH("EHDSI-194", "110112","EHDSI-194", "SMP::Push", new SMPAuditMessageBuilder()),
    COMMUNICATION_FAILURE("EHDSI-CF", "110112","EHDSI-CF", "epsosCommunicationFailure", new CommunicationFailureAuditMessageBuilder());

    private final String code;
    private final String csdCode;
    private final String iheCode;
    private final String iheTransactionName;

    private final AuditMessageBuilder builder;

    public String getCode() {
        return code;
    }

    public String getCsdCode() {
        return csdCode;
    }

    public String getIheCode() {
        return iheCode;
    }

    public String getIheTransactionName() {
        return iheTransactionName;
    }

    public AuditMessageBuilder getBuilder() { return builder; }

    EventType(String code,
              String csdCode,
              String iheCode,
              String iheTransactionName,
              AuditMessageBuilder builder) {
        this.code = code;
        this.csdCode = csdCode;
        this.iheCode = iheCode;
        this.iheTransactionName = iheTransactionName;
        this.builder = builder;
    }

    public AuditMessage buildAuditMessage(EventLog eventLog) {
        return getBuilder().build(eventLog);
    }

    public static EventType determineEventTypeForXCAQuery(List<ClassCode> classCodes) {
        for (ClassCode classCode : classCodes) {
            switch (classCode) {
                case PS_CLASSCODE:
                    return EventType.PATIENT_SERVICE_LIST;
                case EP_CLASSCODE:
                    return EventType.ORDER_SERVICE_LIST;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    return EventType.ORCD_SERVICE_LIST;
            }
        }
        throw new RuntimeException(String.format("EventType for XCA List cannot be determined based on classCodes [%s]", classCodes));
    }

    public static EventType determineEventTypeForXCARetrieve(ClassCode classCode) {
        switch (classCode) {
            case PS_CLASSCODE:
                return EventType.PATIENT_SERVICE_RETRIEVE;
            case EP_CLASSCODE:
                return EventType.ORDER_SERVICE_RETRIEVE;
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
                return EventType.ORCD_SERVICE_RETRIEVE;
        }
        throw new RuntimeException(String.format("EventType for XCA Retrieve cannot be determined based on classCode [%s]", classCode));
    }
}
