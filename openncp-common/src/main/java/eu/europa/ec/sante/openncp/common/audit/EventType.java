package eu.europa.ec.sante.openncp.common.audit;

import eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders.*;
import net.RFC3881.dicom.AuditMessage;


public enum EventType {

    IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS(EventID.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS, EventTypeCode.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS, new IdentificationServiceAuditMessageBuilder()),
    XCA_SERVICE_LIST(EventID.XCA_SERVICE_LIST, EventTypeCode.XCA_SERVICE_LIST, new XCAListAuditMessageBuilder()),
    XCA_SERVICE_RETRIEVE_NCP_A(EventID.XCA_SERVICE_RETRIEVE_NCP_A, EventTypeCode.XCA_SERVICE_RETRIEVE_NCP_A, new XCARetrieveAuditMessageBuilder()),
    XCA_SERVICE_RETRIEVE_NCP_B(EventID.XCA_SERVICE_RETRIEVE_NCP_B, EventTypeCode.XCA_SERVICE_RETRIEVE_NCP_B, new XCARetrieveAuditMessageBuilder()),
    XDR_SERVICE_NCP_A(EventID.XDR_SERVICE_NCP_A, EventTypeCode.XDR_SERVICE_NCP_A, new DispensationServiceAuditMessageBuilder()),
    XDR_SERVICE_NCP_B(EventID.XDR_SERVICE_NCP_B, EventTypeCode.XDR_SERVICE_NCP_B, new DispensationServiceAuditMessageBuilder()),
    HCP_AUTHENTICATION(EventID.HCP_AUTHENTICATION, EventTypeCode.HCP_AUTHENTICATION, new HCPAuthenticationAuditMessageBuilder()),
    TRC_ASSERTION(EventID.TRC_ASSERTION, EventTypeCode.TRC_ASSERTION, new TRCAssertionAuditMessageBuilder()),
    NOK_ASSERTION(EventID.NOK_ASSERTION, EventTypeCode.NOK_ASSERTION, new NOKAssertionAuditMessageBuilder()),
    NCP_TRUSTED_SERVICE_LIST(EventID.NCP_TRUSTED_SERVICE_LIST, EventTypeCode.NCP_TRUSTED_SERVICE_LIST, new NCPTrustedServiceListAuditMessageBuilder()),
    PIVOT_TRANSLATION(EventID.PIVOT_TRANSLATION, EventTypeCode.PIVOT_TRANSLATION, new PivotTranslationAuditMessageBuilder()),
    SMP_QUERY(EventID.SMP_QUERY, EventTypeCode.SMP_QUERY, new SMPAuditMessageBuilder()),
    SMP_PUSH(EventID.SMP_PUSH, EventTypeCode.SMP_PUSH, new SMPAuditMessageBuilder()),
    COMMUNICATION_FAILURE(EventID.COMMUNICATION_FAILURE, EventTypeCode.COMMUNICATION_FAILURE, new CommunicationFailureAuditMessageBuilder());

    private final EventID eventID;

    private final EventTypeCode eventTypeCode;

    private final AuditMessageBuilder builder;

    public EventID getEventID() {
        return eventID;
    }

    public EventTypeCode getEventTypeCode() {
        return eventTypeCode;
    }

    public AuditMessageBuilder getBuilder() { return builder; }

    EventType(EventID eventID, EventTypeCode eventTypeCode, AuditMessageBuilder builder) {
        this.eventID = eventID;
        this.eventTypeCode = eventTypeCode;
        this.builder = builder;
    }

    public AuditMessage buildAuditMessage(EventLog eventLog) {
        return getBuilder().build(eventLog);
    }


    public enum EventID {

        IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS(EventActionCode.EXECUTE, "110112", "Query"),
        XCA_SERVICE_LIST(EventActionCode.EXECUTE, "110112", "Query"),
        XCA_SERVICE_RETRIEVE_NCP_A(EventActionCode.READ, "110106", "Export"),
        XCA_SERVICE_RETRIEVE_NCP_B(EventActionCode.CREATE, "110107", "Import"),
        XDR_SERVICE_NCP_A(EventActionCode.CREATE, "110107", "Import"),
        XDR_SERVICE_NCP_B(EventActionCode.CREATE, "110106", "Export"),
        HCP_AUTHENTICATION(EventActionCode.EXECUTE, "110114", "User Authentication"),
        TRC_ASSERTION(EventActionCode.EXECUTE, "110100", "Application Activity"),
        NOK_ASSERTION(EventActionCode.EXECUTE, "110100", "Application Activity"),
        NCP_TRUSTED_SERVICE_LIST(EventActionCode.EXECUTE, "110100", "Application Activity"),
        PIVOT_TRANSLATION(EventActionCode.EXECUTE, "110100", "Application Activity"),
        SMP_QUERY(EventActionCode.EXECUTE, "110100", "Application Activity"),
        SMP_PUSH(EventActionCode.EXECUTE, "110100", "Application Activity"),
        COMMUNICATION_FAILURE(EventActionCode.EXECUTE, "110100", "Application Activity");

        private EventActionCode eventActionCode;

        private String csdCode;

        private String originalText;

        public EventID.EventActionCode getEventActionCode() {
            return eventActionCode;
        }

        public String getCsdCode() {
            return csdCode;
        }

        public String getOriginalText() {
            return originalText;
        }

        EventID(EventActionCode eventActionCode,
                String csdCode,
                String originalText) {
            this.eventActionCode = eventActionCode;
            this.csdCode = csdCode;
            this.originalText = originalText;
        }

        public enum EventActionCode {

            EXECUTE("E"),
            CREATE("C"),
            READ("R");

            private String code;

            public String getCode() {
                return code;
            }

            EventActionCode(String code) {
                this.code = code;
            }
        }
    }

    public enum EventTypeCode {

        IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS(CodeSystemName.IHE_TRANSACTIONS, "ITI-55", "Cross Gateway Patient Discovery"),
        XCA_SERVICE_LIST(CodeSystemName.IHE_TRANSACTIONS, "ITI-38", "Cross Gateway Query"),
        XCA_SERVICE_RETRIEVE_NCP_A(CodeSystemName.IHE_TRANSACTIONS, "ITI-39", "Cross Gateway Retrieve"),
        XCA_SERVICE_RETRIEVE_NCP_B(CodeSystemName.IHE_TRANSACTIONS, "ITI-39", "Cross Gateway Retrieve"),
        XDR_SERVICE_NCP_A(CodeSystemName.IHE_TRANSACTIONS, "ITI-41", "Provide and Register Document Set-b"),
        XDR_SERVICE_NCP_B(CodeSystemName.IHE_TRANSACTIONS, "ITI-41", "Provide and Register Document Set-b"),
        HCP_AUTHENTICATION(CodeSystemName.IHE_TRANSACTIONS, "ITI-40", "Provide X-User Assertion"),
        TRC_ASSERTION(CodeSystemName.EHDSI_TRANSACTIONS, "EHDSI-92", "Issuance of a TRC Assertion"),
        NOK_ASSERTION(CodeSystemName.EHDSI_TRANSACTIONS, "EHDSI-96", "Issuance of a NOK Assertion"),
        NCP_TRUSTED_SERVICE_LIST(CodeSystemName.EHDSI_TRANSACTIONS, "EHDSI-93", "NSL Import"),
        PIVOT_TRANSLATION(CodeSystemName.EHDSI_TRANSACTIONS, "EHDSI-94", "Transformation"),
        SMP_QUERY(CodeSystemName.EHDSI_TRANSACTIONS, "EHDSI-193", "SMP Query"),
        SMP_PUSH(CodeSystemName.EHDSI_TRANSACTIONS, "EHDSI-194", "SMP Push"),
        COMMUNICATION_FAILURE(CodeSystemName.EHDSI_TRANSACTIONS, "EHDSI-CF", "Communication Failure");


        private final CodeSystemName codeSystemName;
        private final String csdCode;
        private final String originalText;

        public CodeSystemName getCodeSystemName() {
            return codeSystemName;
        }

        public String getCsdCode() {
            return csdCode;
        }

        public String getOriginalText() {
            return originalText;
        }

        EventTypeCode(CodeSystemName codeSystemName,
                          String csdCode,
                          String originalText) {
            this.codeSystemName = codeSystemName;
            this.csdCode = csdCode;
            this.originalText = originalText;
        }

        public enum CodeSystemName {
            IHE_TRANSACTIONS("IHE Transactions"),
            EHDSI_TRANSACTIONS("eHDSI Transactions");

            private final String name;

            public String getName() {
                return name;
            }

            CodeSystemName(String name) {
                this.name = name;
            }
        }
    }
}
