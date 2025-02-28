package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.FieldValueValidators;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.RequiredFieldValidators;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class NokAssertionValidation extends BaseAssertionValidation {
    public NokAssertionValidation(final SignatureManager signatureManager) {
        super(signatureManager);
    }

    @Override
    protected AssertionType getSupportedAssertionType() {
        return AssertionType.NOK;
    }

    @Override
    protected List<FieldValidator> getFieldValidators() {
        return List.of(
                RequiredFieldValidators::validateVersion,
                RequiredFieldValidators::validateID,
                RequiredFieldValidators::validateIssueInstant,
                RequiredFieldValidators::validateIssuer,
                RequiredFieldValidators::validateSubject,
                RequiredFieldValidators::validateNameID,
                RequiredFieldValidators::validateFormat,
                RequiredFieldValidators::validateSubjectConfirmation,
                RequiredFieldValidators::validateMethod,
                RequiredFieldValidators::validateConditions,
                RequiredFieldValidators::validateNotBefore,
                RequiredFieldValidators::validateNotOnOrAfter,
                RequiredFieldValidators::validateAuthnStatement,
                RequiredFieldValidators::validateAuthnInstant,
                RequiredFieldValidators::validateAuthnContext,
                RequiredFieldValidators::validateAuthnContextClassRef,
                RequiredFieldValidators::validateAttributeStatement,
                RequiredFieldValidators::validateSignature,

                RequiredFieldValidators::validateNokIdentifiers,

                FieldValueValidators::validateVersionValue,
                FieldValueValidators::validateIssuerValue,
                FieldValueValidators::validateNameIDValue,
                FieldValueValidators::validateNotBeforeValue,
                FieldValueValidators::validateNotOnOrAfterValue
        );
    }

    @Override
    protected List<PolicyValidator> getPolicyValidators() {
        return List.of();
    }
}
