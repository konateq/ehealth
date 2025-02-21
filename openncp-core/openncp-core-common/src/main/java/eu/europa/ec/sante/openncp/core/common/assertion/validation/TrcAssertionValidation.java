package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.assertion.PolicyAssertionManager;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.FieldValueValidators;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.RequiredFieldValidators;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Component
public class TrcAssertionValidation extends BaseAssertionValidation {
    private final Logger LOGGER = LoggerFactory.getLogger(TrcAssertionValidation.class);
    private final PolicyAssertionManager policyAssertionManager;

    public TrcAssertionValidation(final SignatureManager signatureManager, final PolicyAssertionManager policyAssertionManager) {
        super(signatureManager);
        this.policyAssertionManager = Validate.notNull(policyAssertionManager, "policyAssertionManager must not be null");
    }

    @Override
    public Optional<AssertionValidationResult> validate(final AssertionDetails assertionToCheck, final List<AssertionDetails> allAssertionDetails) {
        final Optional<AssertionValidationResult> baseValidationResult = super.validate(assertionToCheck, allAssertionDetails);
        if (baseValidationResult.isPresent()) {
            final AssertionValidationResult validationResult = baseValidationResult.get();
            final Optional<AssertionDetails> hcpAssertion = allAssertionDetails.stream().filter(assertionDetails -> assertionDetails.getAssertionType() == AssertionType.HCP).findFirst();
            final AssertionValidationDetail trcAdviceIdReferenceValidationDetail = hcpAssertion.map(assertionDetails -> {
                final boolean trcAdviceIdReferenceCheck = checkTRCAdviceIdReferenceAgainstHCPId(assertionToCheck.getAssertion(), assertionDetails.getAssertion());
                if (trcAdviceIdReferenceCheck) {
                    return AssertionValidationDetail.passed(AssertionValidationKey.TRC_ADVICE);
                } else {
                    return ImmutableAssertionValidationDetail.builder()
                            .key(AssertionValidationKey.TRC_ADVICE)
                            .status(AssertionValidationDetailStatus.FAILED)
                            .message("Could not check TRC advice ID reference because the HCP assertion was not present")
                            .build();
                }
            }).orElseGet(() -> ImmutableAssertionValidationDetail.builder()
                    .key(AssertionValidationKey.TRC_ADVICE)
                    .status(AssertionValidationDetailStatus.FAILED)
                    .message("Could not check TRC advice ID reference because the HCP assertion was not present")
                    .build());

            final List<AssertionValidationDetail> updatedValidationDetails = new ArrayList<>(validationResult.getValidationDetails());
            updatedValidationDetails.add(trcAdviceIdReferenceValidationDetail);

            return Optional.of(ImmutableAssertionValidationResult.copyOf(validationResult).withValidationDetails(updatedValidationDetails));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected AssertionType getSupportedAssertionType() {
        return AssertionType.TRC;
    }

    @Override
    protected List<FieldValidator> getFieldValidators() {
        return List.of(
                RequiredFieldValidators::validateVersion,
                RequiredFieldValidators::validateID,
                RequiredFieldValidators::validateIssuer,
                RequiredFieldValidators::validateIssueInstant,
                RequiredFieldValidators::validateSubject,
                RequiredFieldValidators::validateNameID,
                RequiredFieldValidators::validateFormat,
                RequiredFieldValidators::validateSubjectConfirmation,
                RequiredFieldValidators::validateMethod,
                RequiredFieldValidators::validateConditions,
                RequiredFieldValidators::validateNotBefore,
                RequiredFieldValidators::validateNotOnOrAfter,
                RequiredFieldValidators::validateAdvice,
                RequiredFieldValidators::validateAssertionIdRef,
                RequiredFieldValidators::validateAuthnStatement,
                RequiredFieldValidators::validateAuthnInstant,
                RequiredFieldValidators::validateAuthnContext,
                RequiredFieldValidators::validateAuthnContextClassRef,
                RequiredFieldValidators::validateAttributeStatement,
                RequiredFieldValidators::validateSignature,

                FieldValueValidators::validateVersionValue,
                FieldValueValidators::validateIssuerValue,
                FieldValueValidators::validateNameIDValue,
                FieldValueValidators::validateMethodValue,
                FieldValueValidators::validateNotBeforeValue,
                FieldValueValidators::validateNotOnOrAfterValue,
                FieldValueValidators::validateTimeSpanForTRC,
                FieldValueValidators::validateAuthnContextClassRefValueForHCP
        );
    }

    @Override
    protected List<PolicyValidator> getPolicyValidators() {
        return List.of(
                policyAssertionManager::purposeOfUseValidatorForTRC,
                policyAssertionManager::xspaSubjectValidator

        );
    }

    private boolean checkTRCAdviceIdReferenceAgainstHCPId(final Assertion trcAssertion, final Assertion hcpAssertion) {

        try {
            final String trcFirstReferenceId = trcAssertion.getAdvice().getAssertionIDReferences().get(0).getAssertionID();

            if (trcFirstReferenceId != null && trcFirstReferenceId.equals(hcpAssertion.getID())) {
                LOGGER.info("Assertion id reference equals to id.");
                return true;
            }
        } catch (final Exception ex) {
            LOGGER.error("Unable to resolve first id reference: '{}'", ex.getMessage(), ex);
        }
        
        return false;
    }
}
