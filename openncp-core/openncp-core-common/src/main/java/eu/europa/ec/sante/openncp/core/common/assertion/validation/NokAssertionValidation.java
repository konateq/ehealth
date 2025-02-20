package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.AssertionValidationException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.FieldValueValidators;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.RequiredFieldValidators;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class NokAssertionValidation implements AssertionValidation {

    private final SignatureManager signatureManager;

    public NokAssertionValidation(SignatureManager signatureManager) {
        this.signatureManager = Validate.notNull(signatureManager, "signatureManager must not be null");
    }

    @Override
    public AssertionValidationResult validate(final AssertionDetails assertionDetails) {
        if (assertionDetails.getAssertionType() != AssertionType.NOK) {
            return AssertionValidationResult.differentAssertion(assertionDetails, AssertionType.NOK);
        }

        final Assertion assertion = assertionDetails.getAssertion();
        final List<AssertionValidationDetail> validationDetails = new ArrayList<>();

        try {
            checkRequiredFields(assertion);
            validationDetails.add(AssertionValidationDetail.passed(AssertionValidationKey.REQUIRED_FIELDS));
        } catch (AssertionValidationException e) {
            return ImmutableAssertionValidationResult
                    .builder()
                    .status(AssertionValidationStatus.FAILED)
                    .assertionDetails(assertionDetails)
                    .addValidationDetail(ImmutableAssertionValidationDetail.builder()
                            .key(AssertionValidationKey.REQUIRED_FIELDS)
                            .status(AssertionValidationDetailStatus.FAILED)
                            .error(e)
                            .message(e.getMessage())
                            .build())
                    .build();
        }

        try {
            signatureManager.verifySAMLAssertion(assertion);
            validationDetails.add(AssertionValidationDetail.passed(AssertionValidationKey.SIGNATURE));
        } catch (SMgrException e) {
            return ImmutableAssertionValidationResult
                    .builder()
                    .status(AssertionValidationStatus.FAILED)
                    .assertionDetails(assertionDetails)
                    .addAllValidationDetails(validationDetails)
                    .addValidationDetail(ImmutableAssertionValidationDetail.builder()
                            .key(AssertionValidationKey.SIGNATURE)
                            .status(AssertionValidationDetailStatus.FAILED)
                            .error(e)
                            .message(e.getMessage())
                            .build())
                    .build();
        }

        return ImmutableAssertionValidationResult
                .builder()
                .status(AssertionValidationStatus.PASSED)
                .assertionDetails(assertionDetails)
                .addAllValidationDetails(validationDetails)
                .build();
    }

    private void checkRequiredFields(final Assertion assertion) throws MissingFieldException, InvalidFieldException {
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
        RequiredFieldValidators.validateSignature(assertion);

        RequiredFieldValidators.validateNokIdentifiers(assertion);

        FieldValueValidators.validateVersionValue(assertion);
        FieldValueValidators.validateIssuerValue(assertion);
        FieldValueValidators.validateNameIDValue(assertion);
        FieldValueValidators.validateNotBeforeValue(assertion);
        FieldValueValidators.validateNotOnOrAfterValue(assertion);


    }
}
