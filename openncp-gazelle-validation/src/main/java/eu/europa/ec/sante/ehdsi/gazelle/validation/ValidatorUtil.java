package eu.europa.ec.sante.ehdsi.gazelle.validation;

import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.gazelle.validation.util.Constant;
import eu.europa.ec.sante.ehdsi.gazelle.validation.util.ObjectType;
import eu.europa.ec.sante.ehdsi.gazelle.validation.util.XdsModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorUtil {

    public static final String EHDSI_ID_SERVICE_REQUEST;
    public static final String EHDSI_ID_SERVICE_RESPONSE;
    public static final String EHDSI_ART_DECOR_CDA_FRIENDLY;
    public static final String EHDSI_ART_DECOR_CDA_PIVOT;
    public static final String EHDSI_ART_DECOR_SCANNED_DOCUMENT;
    public static final String EHDSI_AUDIT_PROVIDE_DATA_SERVICE_SC;
    public static final String EHDSI_AUDIT_IDENTIFICATION_SERVICE_AUDIT_SP;
    public static final String EHDSI_AUDIT_IMPORT_NCP_TRUSTED_LIST;
    public static final String EHDSI_AUDIT_HCP_ASSURANCE_AUDIT;
    public static final String EHDSI_AUDIT_FETCH_DOC_SERVICE_SC;
    public static final String EHDSI_AUDIT_ISSUANCE_TRC_ASSERTION;
    public static final String EHDSI_AUDIT_PIVOT_TRANSLATION;
    public static final String EHDSI_AUDIT_PROVIDE_DATA_SERVICE_SP;
    public static final String EHDSI_AUDIT_ISSUANCE_HCP_ASSERTION;
    public static final String EHDSI_AUDIT_FETCH_DOC_SERVICE_SP;
    public static final String EHDSI_AUDIT_SMP_SERVICE_CONSUMER_QUERY;
    public static final String EHDSI_AUDIT_SMP_SERVICE_CONSUMER_PUSH;
    public static final String EHDSI_ASSERTION_HCP_IDENTITY;
    public static final String EHDSI_ASSERTION_TRC;
    public static final String EHDSI_XDS_OS_LIST_REQUEST_XCA;
    public static final String EHDSI_XDS_CS_PUT_REQUEST;
    public static final String EHDSI_XDS_PS_LIST_REQUEST_XCA;
    public static final String EHDSI_XDS_ED_INIT_REQUEST;
    public static final String EHDSI_XDS_PROVIDE_DATA_REQUEST;
    public static final String EHDSI_XDS_CS_PUT_RESPONSE;
    public static final String EHDSI_XDS_ED_INIT_RESPONSE;
    public static final String EHDSI_XDS_PROVIDE_DATA_RESPONSE;
    public static final String EHDSI_XDS_FETCH_DOC_QUERY_REQUEST;
    public static final String EHDSI_XDS_OS_LIST_RESPONSE_XCA;
    public static final String EHDSI_XDS_PS_LIST_RESPONSE_XCA;
    public static final String EHDSI_XDS_FETCH_DOC_QUERY_RESPONSE;
    public static final String EHDSI_XDS_OS_RETRIEVE_REQUEST_XCA;
    public static final String EHDSI_XDS_PS_RETRIEVE_REQUEST_XCA;
    public static final String EHDSI_XDS_FETCH_DOC_RETRIEVE_REQUEST;
    public static final String EHDSI_XDS_OS_RETRIEVE_RESPONSE_XCA;
    public static final String EHDSI_XDS_PS_RETRIEVE_RESPONSE_XCA;
    public static final String EHDSI_XDS_FETCH_DOC_RETRIEVE_RESPONSE;

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatorUtil.class);

    static {
        try {
            EHDSI_ASSERTION_HCP_IDENTITY = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_HCP_IDENTITY_ASSERTION");
            EHDSI_ASSERTION_TRC = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_TRC_ASSERTION");

            EHDSI_ID_SERVICE_REQUEST = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_ID_SERVICE_REQUEST");
            EHDSI_ID_SERVICE_RESPONSE = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_ID_SERVICE_RESPONSE");

            EHDSI_ART_DECOR_CDA_FRIENDLY = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_ART_DECOR_CDA_FRIENDLY");
            EHDSI_ART_DECOR_CDA_PIVOT = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_ART_DECOR_CDA_PIVOT");
            EHDSI_ART_DECOR_SCANNED_DOCUMENT = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_ART_DECOR_SCANNED_DOCUMENT");

            EHDSI_AUDIT_PROVIDE_DATA_SERVICE_SC = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_PROVIDE_DATA_SERVICE_SC");
            EHDSI_AUDIT_IDENTIFICATION_SERVICE_AUDIT_SP = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_IDENTIFICATION_SERVICE_AUDIT_SP");
            EHDSI_AUDIT_IMPORT_NCP_TRUSTED_LIST = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_IMPORT_NCP_TRUSTED_LIST");
            EHDSI_AUDIT_HCP_ASSURANCE_AUDIT = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_HCP_ASSURANCE_AUDIT");
            EHDSI_AUDIT_FETCH_DOC_SERVICE_SC = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_FETCH_DOC_SERVICE_SC");
            EHDSI_AUDIT_ISSUANCE_TRC_ASSERTION = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_ISSUANCE_TRC_ASSERTION");
            EHDSI_AUDIT_PROVIDE_DATA_SERVICE_SP = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_PROVIDE_DATA_SERVICE_SP");
            EHDSI_AUDIT_ISSUANCE_HCP_ASSERTION = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_ISSUANCE_HCP_ASSERTION");
            EHDSI_AUDIT_FETCH_DOC_SERVICE_SP = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_FETCH_DOC_SERVICE_SP");
            EHDSI_AUDIT_PIVOT_TRANSLATION = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_PIVOT_TRANSLATION");
            EHDSI_AUDIT_SMP_SERVICE_CONSUMER_QUERY = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_SMP_SERVICE_CONSUMER_QUERY");
            EHDSI_AUDIT_SMP_SERVICE_CONSUMER_PUSH = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_AUDIT_SMP_SERVICE_CONSUMER_PUSH");

            EHDSI_XDS_OS_LIST_REQUEST_XCA = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_OS_LIST_REQUEST_XCA");
            EHDSI_XDS_CS_PUT_REQUEST = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_CS_PUT_REQUEST");
            EHDSI_XDS_PS_LIST_REQUEST_XCA = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_PS_LIST_REQUEST_XCA");
            EHDSI_XDS_ED_INIT_REQUEST = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_ED_INIT_REQUEST");
            EHDSI_XDS_PROVIDE_DATA_REQUEST = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_PROVIDE_DATA_REQUEST");
            EHDSI_XDS_CS_PUT_RESPONSE = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_CS_PUT_RESPONSE");
            EHDSI_XDS_ED_INIT_RESPONSE = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_ED_INIT_RESPONSE");
            EHDSI_XDS_PROVIDE_DATA_RESPONSE = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_PROVIDE_DATA_RESPONSE");
            EHDSI_XDS_FETCH_DOC_QUERY_REQUEST = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_FETCH_DOC_QUERY_REQUEST");
            EHDSI_XDS_OS_LIST_RESPONSE_XCA = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_OS_LIST_RESPONSE_XCA");
            EHDSI_XDS_PS_LIST_RESPONSE_XCA = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_PS_LIST_RESPONSE_XCA");
            EHDSI_XDS_FETCH_DOC_QUERY_RESPONSE = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_FETCH_DOC_QUERY_RESPONSE");
            EHDSI_XDS_OS_RETRIEVE_REQUEST_XCA = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_OS_RETRIEVE_REQUEST_XCA");
            EHDSI_XDS_PS_RETRIEVE_REQUEST_XCA = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_PS_RETRIEVE_REQUEST_XCA");
            EHDSI_XDS_FETCH_DOC_RETRIEVE_REQUEST = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_FETCH_DOC_RETRIEVE_REQUEST");
            EHDSI_XDS_OS_RETRIEVE_RESPONSE_XCA = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_OS_RETRIEVE_RESPONSE_XCA");
            EHDSI_XDS_PS_RETRIEVE_RESPONSE_XCA = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_PS_RETRIEVE_RESPONSE_XCA");
            EHDSI_XDS_FETCH_DOC_RETRIEVE_RESPONSE = (String) GazelleConfiguration.getInstance().getConfiguration().getProperty("EHDSI_XDS_FETCH_DOC_RETRIEVE_RESPONSE");
        } catch (Exception e) {
            LOGGER.error("Failure during static initialization: '{}'", e.getMessage(), e);
            throw e;
        }
    }

    private ValidatorUtil() {
    }

    public static String obtainAuditModel(String eventType, NcpSide ncpSide) {

        String model = "";
        // Infer model according to NCP Side and EventCode
        if (ncpSide == NcpSide.NCP_A) {
            if (StringUtils.equals(eventType, "epsos-11")) {

                model = ValidatorUtil.EHDSI_AUDIT_HCP_ASSURANCE_AUDIT;
            }
            if (StringUtils.equals(eventType, "epsos-94")) {

                model = ValidatorUtil.EHDSI_AUDIT_PIVOT_TRANSLATION;
            }
            if (StringUtils.equals(eventType, "epsos-21") || StringUtils.equals(eventType, "epsos-22")
                    || StringUtils.equals(eventType, "epsos-31") || StringUtils.equals(eventType, "epsos-32")
                    || StringUtils.equals(eventType, "epsos-96") || StringUtils.equals(eventType, "ITI-38")
                    || StringUtils.equals(eventType, "ITI-39") || StringUtils.equals(eventType, "epsos-95")) {

                model = ValidatorUtil.EHDSI_AUDIT_FETCH_DOC_SERVICE_SP;
            }
            if (StringUtils.equals(eventType, "epsos-41") || StringUtils.equals(eventType, "epsos-51")) {

                model = ValidatorUtil.EHDSI_AUDIT_PROVIDE_DATA_SERVICE_SP;
            }
            if (StringUtils.equals(eventType, "ehealth-193")) {

                model = ValidatorUtil.EHDSI_AUDIT_SMP_SERVICE_CONSUMER_QUERY;
            }
            if (StringUtils.equals(eventType, "ehealth-194")) {

                model = ValidatorUtil.EHDSI_AUDIT_SMP_SERVICE_CONSUMER_PUSH;
            }
        } else {

            if (StringUtils.equals(eventType, "epsos-11")) {

                model = ValidatorUtil.EHDSI_AUDIT_HCP_ASSURANCE_AUDIT;
            }
            if (StringUtils.equals(eventType, "epsos-94")) {

                model = ValidatorUtil.EHDSI_AUDIT_PIVOT_TRANSLATION;
            }
            if (StringUtils.equals(eventType, "epsos-96") || StringUtils.equals(eventType, "ITI-38")
                    || StringUtils.equals(eventType, "ITI-39") || StringUtils.equals(eventType, "epsos-95")) {

                model = ValidatorUtil.EHDSI_AUDIT_HCP_ASSURANCE_AUDIT;
            }
            if (StringUtils.equals(eventType, "epsos-21") || StringUtils.equals(eventType, "epsos-22")
                    || StringUtils.equals(eventType, "epsos-31") || StringUtils.equals(eventType, "epsos-32")) {

                model = ValidatorUtil.EHDSI_AUDIT_FETCH_DOC_SERVICE_SC;
            }
            if (StringUtils.equals(eventType, "epsos-41") || StringUtils.equals(eventType, "epsos-51")) {

                model = ValidatorUtil.EHDSI_AUDIT_PROVIDE_DATA_SERVICE_SC;
            }
            if (StringUtils.equals(eventType, "epsos-91")) {

                model = ValidatorUtil.EHDSI_AUDIT_ISSUANCE_HCP_ASSERTION;
            }
            if (StringUtils.equals(eventType, "epsos-92")) {

                model = ValidatorUtil.EHDSI_AUDIT_ISSUANCE_TRC_ASSERTION;
            }
            if (StringUtils.equals(eventType, "epsos-93")) {

                model = ValidatorUtil.EHDSI_AUDIT_IMPORT_NCP_TRUSTED_LIST;
            }
            if (StringUtils.equals(eventType, "ehealth-193")) {

                model = ValidatorUtil.EHDSI_AUDIT_SMP_SERVICE_CONSUMER_QUERY;
            }
            if (StringUtils.equals(eventType, "ehealth-194")) {

                model = ValidatorUtil.EHDSI_AUDIT_SMP_SERVICE_CONSUMER_PUSH;
            }
        }
        return model;
    }

    /**
     * This helper method will return a specific CDA model based on a document class code
     * (also choosing between friendly or pivot documents).
     *
     * @param classCode         The document class code.
     * @param isPivot           The boolean flag stating if the document is pivot or
     *                          not.
     * @param isScannedDocument The boolean flag stating if the document is a scanned document or not.
     * @return the correspondent CDA model.
     */
    public static String obtainCdaModel(String classCode, boolean isPivot, boolean isScannedDocument) {

        if (StringUtils.isBlank(classCode)) {
            return null;
        } else {
            if (isScannedDocument) {
                return ValidatorUtil.EHDSI_ART_DECOR_SCANNED_DOCUMENT;
            } else {
                return isPivot ? ValidatorUtil.EHDSI_ART_DECOR_CDA_PIVOT : ValidatorUtil.EHDSI_ART_DECOR_CDA_FRIENDLY;
            }
        }
    }

    /**
     * This method will look into an XDR message and obtain the proper model to validate it at Gazelle
     *
     * @param message the XDR message to be validated
     * @return the proper model to be used in the validation
     */
    public static XdsModel obtainModelXdr(String message) {

        final String PROVIDE_AND_REGISTER_REQUEST = "ProvideAndRegisterDocumentSetRequest";
        final String PROVIDE_AND_REGISTER_RESPONSE = "RegistryResponse";

        XdsModel result = new XdsModel();

        if (message.contains(PROVIDE_AND_REGISTER_REQUEST)) {

            if (message.contains(Constant.CONSENT_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_CS_PUT_REQUEST);

            } else if (message.contains(Constant.ED_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_ED_INIT_REQUEST);

            } else {
                result.setValidatorName(EHDSI_XDS_PROVIDE_DATA_REQUEST);
            }
            result.setObjectType(ObjectType.XDR_SUBMIT_REQUEST.toString());

        } else if (message.contains(PROVIDE_AND_REGISTER_RESPONSE)) {

            //TODO: To be validated that only one eHDSI Gazelle Validator might be use for response validation
            // if (message.contains(Constant.CONSENT_CLASSCODE)) {
            //     result.setValidatorName(EHDSI_XDS_CS_PUT_RESPONSE);
            // } else if (message.contains(Constant.ED_CLASSCODE)) {
            //     result.setValidatorName(EHDSI_XDS_ED_INIT_RESPONSE);
            // } else {
            //     result.setValidatorName(EHDSI_XDS_PROVIDE_DATA_RESPONSE);
            // }
            result.setValidatorName(EHDSI_XDS_ED_INIT_RESPONSE);
            result.setObjectType(ObjectType.XDR_SUBMIT_RESPONSE.toString());
        }
        return result;
    }

    /**
     * This method will look into an XCA message and obtain the proper model to validate it at Gazelle.
     *
     * @param message the XCA message to be validated
     * @return the proper model to be used in the validation
     */
    public static XdsModel obtainModelXca(String message) {

        final String QUERY_REQUEST = "AdhocQueryRequest";
        final String QUERY_RESPONSE = "AdhocQueryResponse";
        final String RETRIEVE_REQUEST = "RetrieveDocumentSetRequest";
        final String RETRIEVE_RESPONSE = "RetrieveDocumentSetResponse";

        XdsModel result = new XdsModel();

        // Query / List operations
        // Request
        if (message.contains(QUERY_REQUEST)) {
            if (message.contains(Constant.EP_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_OS_LIST_REQUEST_XCA);
            } else if (message.contains(Constant.PS_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_PS_LIST_REQUEST_XCA);
            } else {
                result.setValidatorName(EHDSI_XDS_FETCH_DOC_QUERY_REQUEST);
            }
            result.setObjectType(ObjectType.XCA_QUERY_REQUEST.toString());
            // Response
        } else if (message.contains(QUERY_RESPONSE)) {
            if (message.contains(Constant.EP_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_OS_LIST_RESPONSE_XCA);
            } else if (message.contains(Constant.PS_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_PS_LIST_RESPONSE_XCA);
            } else {
                result.setValidatorName(EHDSI_XDS_FETCH_DOC_QUERY_RESPONSE);
            }
            result.setObjectType(ObjectType.XCA_QUERY_RESPONSE.toString());
        }
        // Retrieve operations
        if (message.contains(RETRIEVE_REQUEST)) {  // Request
            if (message.contains(Constant.EP_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_OS_RETRIEVE_REQUEST_XCA);
            } else if (message.contains(Constant.PS_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_PS_RETRIEVE_REQUEST_XCA);
            } else {
                result.setValidatorName(EHDSI_XDS_FETCH_DOC_RETRIEVE_REQUEST);
            }
            result.setObjectType(ObjectType.XCA_RETRIEVE_REQUEST.toString());
        } else if (message.contains(RETRIEVE_RESPONSE)) { // Response
            if (message.contains(Constant.EP_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_OS_RETRIEVE_RESPONSE_XCA);
            } else if (message.contains(Constant.PS_CLASSCODE)) {
                result.setValidatorName(EHDSI_XDS_PS_RETRIEVE_RESPONSE_XCA);
            } else {
                result.setValidatorName(EHDSI_XDS_FETCH_DOC_RETRIEVE_RESPONSE);
            }
            result.setObjectType(ObjectType.XCA_RETRIEVE_RESPONSE.toString());
        }
        return result;
    }
}
