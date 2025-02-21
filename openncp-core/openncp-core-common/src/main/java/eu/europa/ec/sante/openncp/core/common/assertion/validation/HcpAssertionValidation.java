package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.core.common.assertion.PolicyAssertionManager;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.FieldValueValidators;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.RequiredFieldValidators;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class HcpAssertionValidation extends BaseAssertionValidation {
    private final PolicyAssertionManager policyAssertionManager;

    public HcpAssertionValidation(final SignatureManager signatureManager, final PolicyAssertionManager policyAssertionManager) {
        super(signatureManager);
        this.policyAssertionManager = Validate.notNull(policyAssertionManager, "policyAssertionManager must not be null");
    }

    @Override
    protected AssertionType getSupportedAssertionType() {
        return AssertionType.HCP;
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
                FieldValueValidators::validateVersionValue,
                FieldValueValidators::validateIssuerValue,
                FieldValueValidators::validateNameIDValue,
                FieldValueValidators::validateNotBeforeValue,
                FieldValueValidators::validateNotOnOrAfterValue,
                FieldValueValidators::validateTimeSpanForHCP,
                FieldValueValidators::validateAuthnContextClassRefValueForHCP
        );
    }

    @Override
    protected List<PolicyValidator> getPolicyValidators() {
        return List.of(
                policyAssertionManager::xspaSubjectValidator,
                policyAssertionManager::xspaRoleValidator,
                policyAssertionManager::healthcareFacilityValidator,
                policyAssertionManager::purposeOfUseValidator,
                policyAssertionManager::xspaLocalityValidator
        );
    }
}
