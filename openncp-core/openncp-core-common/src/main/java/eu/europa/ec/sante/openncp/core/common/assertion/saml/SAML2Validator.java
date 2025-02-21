package eu.europa.ec.sante.openncp.core.common.assertion.saml;

import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.core.common.assertion.PolicyAssertionManager;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.XSDValidationException;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidator;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SAML2Validator {

    private static final String OASIS_WSSE_SCHEMA_LOC = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final Logger LOGGER = LoggerFactory.getLogger(SAML2Validator.class);

    private final AssertionValidator assertionValidator;
    private final SignatureManager signatureManager;
    private final PolicyAssertionManager policyAssertionManager;

    private SAML2Validator(final AssertionValidator assertionValidator, final SignatureManager signatureManager, final PolicyAssertionManager policyAssertionManager) {
        this.assertionValidator = Validate.notNull(assertionValidator, "assertionValidator must not be null");
        this.signatureManager = Validate.notNull(signatureManager, "signatureManager must not be null");
        this.policyAssertionManager = Validate.notNull(policyAssertionManager, "policyAssertionManager must not be null");
    }

    public static void validateAndExtractCountryCodeForXcpd(final Assertion assertion) throws MissingFieldException, InsufficientRightsException,
            InvalidFieldException, XSDValidationException, SMgrException {
        LOGGER.debug("[SAML] Validating HCP assertion.");

        try {
            RequiredFieldValidators.validateVersion(assertion);
            RequiredFieldValidators.validateID(assertion);
            RequiredFieldValidators.validateIssueInstant(assertion);
            RequiredFieldValidators.validateIssuer(assertion);
            RequiredFieldValidators.validateSubject(assertion);
            RequiredFieldValidators.validateNameID(assertion);
            RequiredFieldValidators.validateFormat(assertion);
            RequiredFieldValidators.validateSubjectConfirmation(assertion);
            RequiredFieldValidators.validateMethod(assertion);
            RequiredFieldValidators.validateConditions(assertion);
            RequiredFieldValidators.validateNotBefore(assertion);
            RequiredFieldValidators.validateNotOnOrAfter(assertion);
            RequiredFieldValidators.validateAuthnStatement(assertion);
            RequiredFieldValidators.validateAuthnInstant(assertion);
            RequiredFieldValidators.validateAuthnContext(assertion);
            RequiredFieldValidators.validateAuthnContextClassRef(assertion);
            RequiredFieldValidators.validateAttributeStatement(assertion);

            FieldValueValidators.validateVersionValue(assertion);
            FieldValueValidators.validateIssuerValue(assertion);
            FieldValueValidators.validateNameIDValue(assertion);
            FieldValueValidators.validateNotBeforeValue(assertion);
            FieldValueValidators.validateNotOnOrAfterValue(assertion);
            FieldValueValidators.validateTimeSpanForHCP(assertion);
            FieldValueValidators.validateAuthnContextClassRefValueForHCP(assertion);

        } catch (final MissingFieldException e) {
            throw new MissingFieldException(OpenNCPErrorCode.ERROR_HPI_GENERIC, e.getMessage());
        } catch (final InvalidFieldException e) {
            throw new InvalidFieldException(OpenNCPErrorCode.ERROR_HPI_GENERIC, e.getMessage());
        }

    }
}
