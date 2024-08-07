package eu.europa.ec.sante.ehdsi.gazelle.validation;

import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.constant.ClassCode;
import eu.europa.ec.sante.ehdsi.gazelle.validation.reporting.ReportBuilder;
import eu.europa.ec.sante.ehdsi.gazelle.validation.util.DetailedResultUnMarshaller;
import eu.europa.ec.sante.ehdsi.gazelle.validation.util.ObjectType;
import eu.europa.ec.sante.ehdsi.gazelle.validation.util.XdsModel;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import net.ihe.gazelle.jaxb.result.sante.DetailedResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.TransformerException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OpenNCPValidation {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenNCPValidation.class);
    private static final String MSG_VALIDATION_EXECUTION = "Remote validation executed in: '{}ms'";

    private OpenNCPValidation() {
    }

    /**
     * @param document
     * @param eventType
     * @param ncpSide
     */
    public static void validateAuditMessage(final String document, final String eventType, final NcpSide ncpSide) {

        LOGGER.info("Audit Message Validation: '{}'-'{}'", eventType, ncpSide.getName());
        final String validator = ValidatorUtil.obtainAuditModel(eventType, ncpSide);

        if (isRemoteValidationEnable()) {

            new Thread(() -> {
                final StopWatch watch = new StopWatch();
                watch.start();
                final AuditMessageValidator auditMessageValidator = GazelleValidatorFactory.getAuditMessageValidator();
                final String xmlResult = auditMessageValidator.validateDocument(document, validator);
                ReportBuilder.build(ReportBuilder.formatDate(), validator, ObjectType.AUDIT.toString(), document,
                        DetailedResultUnMarshaller.unmarshal(xmlResult), xmlResult, ncpSide);
                watch.stop();
                LOGGER.info(MSG_VALIDATION_EXECUTION, watch.getTime());
            }).start();
        } else {
            ReportBuilder.build(ReportBuilder.formatDate(), validator, ObjectType.AUDIT.toString(), document, ncpSide);
        }
    }

    /**
     * @param assertion
     * @param ncpSide
     */
    public static void validateHCPAssertion(final Assertion assertion, final NcpSide ncpSide) {

        LOGGER.info("validate HCP Assertion...");
        validateAssertion(assertion, ValidatorUtil.EHDSI_ASSERTION_HCP_IDENTITY, ncpSide);
    }

    /**
     * @param assertion
     * @param ncpSide
     */
    public static void validateNoKAssertion(final Assertion assertion, final NcpSide ncpSide) {

        LOGGER.info("validate Next Of Kin Assertion...");
        validateAssertion(assertion, ValidatorUtil.EHDSI_ASSERTION_NOK, ncpSide);
    }

    /**
     * @param assertion
     * @param ncpSide
     */
    public static void validateTRCAssertion(final Assertion assertion, final NcpSide ncpSide) {

        LOGGER.info("validate TRC Assertion...");
        validateAssertion(assertion, ValidatorUtil.EHDSI_ASSERTION_TRC, ncpSide);
    }

    /**
     * @param assertion
     * @param validatorName
     * @param ncpSide
     */
    private static void validateAssertion(final Assertion assertion, final String validatorName, final NcpSide ncpSide) {

        LOGGER.info("[Validation Service: Assertion Validator]");
        try {
            final String base64 = DatatypeConverter.printBase64Binary(XMLUtil.prettyPrint(assertion.getDOM()).getBytes(StandardCharsets.UTF_8));

            if (isRemoteValidationEnable()) {

                new Thread(() -> {
                    final StopWatch watch = new StopWatch();
                    watch.start();
                    final String xmlResult;
                    final AssertionValidator assertionValidator = GazelleValidatorFactory.getAssertionValidator();
                    xmlResult = assertionValidator.validateBase64Document(base64, validatorName);
                    final DetailedResult detailedResult = DetailedResultUnMarshaller.unmarshal(xmlResult);
                    ReportBuilder.build(ReportBuilder.formatDate(), validatorName, ObjectType.ASSERTION.toString(), base64, detailedResult, xmlResult, ncpSide);
                    watch.stop();
                    LOGGER.info(MSG_VALIDATION_EXECUTION, watch.getTime());
                }).start();

            } else {
                ReportBuilder.build(ReportBuilder.formatDate(), validatorName, ObjectType.ASSERTION.toString(), base64, ncpSide);
            }
        } catch (final TransformerException e) {
            LOGGER.error("TransformerException: '{}'", e.getMessage(), e);
        }
    }

    /**
     * @param request
     * @param ncpSide
     */
    public static void validatePatientDemographicRequest(final String request, final NcpSide ncpSide) {

        validatePatientDemographic(request, ValidatorUtil.EHDSI_ID_SERVICE_REQUEST, ObjectType.XCPD_QUERY_REQUEST, ncpSide);
    }

    /**
     * @param request
     * @param ncpSide
     */
    public static void validatePatientDemographicResponse(final String request, final NcpSide ncpSide) {

        validatePatientDemographic(request, ValidatorUtil.EHDSI_ID_SERVICE_RESPONSE, ObjectType.XCPD_QUERY_RESPONSE, ncpSide);
    }

    /**
     * @param request
     * @param validator
     * @param objectType
     * @param ncpSide
     */
    private static void validatePatientDemographic(final String request, final String validator, final ObjectType objectType, final NcpSide ncpSide) {

        LOGGER.info("[Validation Service: XCPD Validator]");
        if (isRemoteValidationEnable()) {

            new Thread(() -> {
                final StopWatch watch = new StopWatch();
                watch.start();
                final HL7v3Validator hl7v3Validator = GazelleValidatorFactory.getHL7v3Validator();
                final String xmlResult = hl7v3Validator.validateDocument(request, validator, ncpSide);
                final DetailedResult detailedResult = DetailedResultUnMarshaller.unmarshal(xmlResult);
                ReportBuilder.build(ReportBuilder.formatDate(), validator, objectType.toString(), request, detailedResult, xmlResult, ncpSide);
                watch.stop();
                LOGGER.info(MSG_VALIDATION_EXECUTION, watch.getTime());
            }).start();
        } else {

            ReportBuilder.build(ReportBuilder.formatDate(), validator, objectType.toString(), request, ncpSide);
        }
    }

    /**
     * @param message
     * @param ncpSide
     */
    public static void validateCrossCommunityAccess(final String message, final NcpSide ncpSide, final List<ClassCode> classCodes) {

        LOGGER.info("[Validation Service: XCA Validator]");
        final XdsModel xdsModel = ValidatorUtil.obtainModelXca(message, classCodes);
        validateXDSMessage(message, xdsModel, ncpSide);
    }

    /**
     * @param request
     * @param ncpSide
     */
    public static void validateXDRMessage(final String request, final NcpSide ncpSide, final List<String> classCodes) {

        LOGGER.info("[Validation Service: XDR Validator]");
        final XdsModel xdsModel = ValidatorUtil.obtainModelXdr(request, classCodes);
        validateXDSMessage(request, xdsModel, ncpSide);
    }

    private static void validateXDSMessage(final String message, final XdsModel xdsModel, final NcpSide ncpSide) {

        if (isRemoteValidationEnable()) {

            new Thread(() -> {
                final StopWatch watch = new StopWatch();
                watch.start();
                final XdsValidator xdsValidator = GazelleValidatorFactory.getXdsValidator();
                final String xmlResult = xdsValidator.validateDocument(message, xdsModel.getValidatorName());
                final DetailedResult detailedResult = DetailedResultUnMarshaller.unmarshal(xmlResult);
                ReportBuilder.build(ReportBuilder.formatDate(), xdsModel.getValidatorName(), xdsModel.getObjectType(), message, detailedResult, xmlResult, ncpSide);
                watch.stop();
                LOGGER.info(MSG_VALIDATION_EXECUTION, watch.getTime());
            }).start();
        } else {
            ReportBuilder.build(ReportBuilder.formatDate(), xdsModel.getValidatorName(), xdsModel.getObjectType(), message, ncpSide);
        }
    }

    /**
     * @param cda
     * @param ncpSide
     * @param classCode
     * @param isPivot
     */
    public static void validateCdaDocument(final String cda, final NcpSide ncpSide, final ClassCode classCode, final boolean isPivot) {

        LOGGER.info("[Validation Service: CDA Validator]");
        final boolean isScannedDocument = cda.contains("nonXMLBody");
        final String cdaModel = ValidatorUtil.obtainCdaModel(classCode, isPivot, isScannedDocument);

        if (isRemoteValidationEnable()) {

            new Thread(() -> {
                final StopWatch watch = new StopWatch();
                watch.start();
                final String xmlResult = GazelleValidatorFactory.getCdaValidator().validateDocument(cda, cdaModel, ncpSide);
                final DetailedResult detailedResult = DetailedResultUnMarshaller.unmarshal(xmlResult);
                ReportBuilder.build(ReportBuilder.formatDate(), cdaModel, ObjectType.CDA.toString(), cda, detailedResult, xmlResult, ncpSide);
                watch.stop();
                LOGGER.info(MSG_VALIDATION_EXECUTION, watch.getTime());
            }).start();
        } else {
            ReportBuilder.build(ReportBuilder.formatDate(), cdaModel, ObjectType.CDA.toString(), cda, ncpSide);
        }
    }

    /**
     * @return
     */
    public static boolean isValidationEnable() {

        return ConfigurationManagerFactory.getConfigurationManager().getBooleanProperty("automated.validation") &&
                !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name());
    }

    /**
     * @return
     */
    public static boolean isRemoteValidationEnable() {

        return ConfigurationManagerFactory.getConfigurationManager().getBooleanProperty("automated.validation.remote");
    }
}
