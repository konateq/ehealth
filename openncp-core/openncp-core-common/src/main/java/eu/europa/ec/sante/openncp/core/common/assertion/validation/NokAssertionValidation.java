package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.FieldValueValidators;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.RequiredFieldValidators;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Component
public class NokAssertionValidation extends BaseAssertionValidation {
    private final Logger LOGGER = LoggerFactory.getLogger(NokAssertionValidation.class);

    public NokAssertionValidation(final SignatureManager signatureManager) {
        super(signatureManager);
    }

    @Override
    protected AssertionType getSupportedAssertionType() {
        return AssertionType.NOK;
    }

    @Override
    public Optional<AssertionValidationResult> validate(final AssertionDetails assertionToValidate, final List<AssertionDetails> allAssertionDetails) {

        final Optional<AssertionValidationResult> baseValidationResult = super.validate(assertionToValidate, allAssertionDetails);
        if (baseValidationResult.isPresent()) {
            final AssertionValidationResult validationResult = baseValidationResult.get();
            final Optional<AssertionDetails> hcpAssertion = allAssertionDetails.stream().filter(assertionDetails -> assertionDetails.getAssertionType() == AssertionType.HCP).findFirst();
            final AssertionValidationDetail checkSubjectgainstHCPValidationDetail = hcpAssertion.map(hcpAssertionDetails -> {
                final boolean checkSubjectgainstHCP = checkSubjectAgainstHcpSubject(assertionToValidate.getAssertion(), hcpAssertionDetails.getAssertion());
                if (checkSubjectgainstHCP) {
                    return AssertionValidationDetail.passed(AssertionValidationKey.NOK_SUBJECT_REFERENCE);
                } else {
                    return ImmutableAssertionValidationDetail.builder()
                            .key(AssertionValidationKey.NOK_SUBJECT_REFERENCE)
                            .status(AssertionValidationDetailStatus.FAILED)
                            .message("The NOK subject was different than the HP assertion subject.")
                            .build();
                }
            }).orElseGet(() -> ImmutableAssertionValidationDetail.builder()
                    .key(AssertionValidationKey.NOK_SUBJECT_REFERENCE)
                    .status(AssertionValidationDetailStatus.FAILED)
                    .message("Could not check NOK subject reference because the HCP assertion was not present")
                    .build());

            final List<AssertionValidationDetail> updatedValidationDetails = new ArrayList<>(validationResult.getValidationDetails());
            updatedValidationDetails.add(checkSubjectgainstHCPValidationDetail);

            return Optional.of(ImmutableAssertionValidationResult.copyOf(validationResult).withValidationDetails(updatedValidationDetails));
        } else {
            return Optional.empty();
        }
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

    private boolean checkSubjectAgainstHcpSubject(final Assertion nokAssertion, final Assertion hcpAssertion) {
        final NameID nokNameId = nokAssertion.getSubject().getNameID();
        final NameID hcpNameId = hcpAssertion.getSubject().getNameID();

        if (nokNameId == null && hcpNameId == null) {
            LOGGER.info("Both NameID objects are null");
            return true;
        }

        if (nokNameId == null || hcpNameId == null) {
            LOGGER.error("One NameID objects is null: NOK NameID [{}], HCP NameID [{}]", nokNameId, hcpNameId);
            return false;
        }

        if (!Objects.equals(nokNameId.getFormat(), hcpNameId.getFormat())) {
            LOGGER.error("NOK Assertion subject id format [{}] does not equal to the HCP one [{}]",
                    nokNameId.getFormat(), hcpNameId.getFormat());
            return false;
        }

        if (!Objects.equals(nokNameId.getValue(), hcpNameId.getValue())) {
            LOGGER.error("NOK Assertion subject id value [{}] does not equal to the HCP one [{}]",
                    nokNameId.getValue(), hcpNameId.getValue());
            return false;
        }

        return true;
    }
}
