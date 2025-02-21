package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.common.xml.SAMLSchemaBuilder;
import org.opensaml.saml.saml2.core.Assertion;
import org.xml.sax.SAXException;

import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BaseAssertionValidation implements AssertionValidation {
    private final SignatureManager signatureManager;

    protected abstract AssertionType getSupportedAssertionType();

    protected abstract List<FieldValidator> getFieldValidators();

    protected abstract List<PolicyValidator> getPolicyValidators();

    public BaseAssertionValidation(final SignatureManager signatureManager) {
        this.signatureManager = Validate.notNull(signatureManager, "signatureManager must not be null");
    }

    @Override
    public Optional<AssertionValidationResult> validate(final AssertionDetails assertionDetails, final List<AssertionDetails> allAssertionDetails) {
        if (assertionDetails.getAssertionType() != getSupportedAssertionType()) {
            return Optional.empty();
        }

        final Assertion assertion = assertionDetails.getAssertion();
        final List<AssertionValidationDetail> validationDetails = new ArrayList<>();

        try {
            signatureManager.verifySAMLAssertion(assertion);
            validationDetails.add(AssertionValidationDetail.passed(AssertionValidationKey.SIGNATURE));
        } catch (final SMgrException e) {
            return Optional.of(ImmutableAssertionValidationResult
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
                    .build());
        }

        final var schemaBuilder = new SAMLSchemaBuilder(SAMLSchemaBuilder.SAML1Version.SAML_11);
        validationDetails.add(AssertionValidationDetail.passed(AssertionValidationKey.XSD));
        try {
            schemaBuilder.getSAMLSchema().newValidator().validate(new DOMSource(assertion.getDOM()));
        } catch (final IOException | SAXException e) {
            return Optional.of(ImmutableAssertionValidationResult
                    .builder()
                    .status(AssertionValidationStatus.FAILED)
                    .assertionDetails(assertionDetails)
                    .addAllValidationDetails(validationDetails)
                    .addValidationDetail(ImmutableAssertionValidationDetail.builder()
                            .key(AssertionValidationKey.XSD)
                            .status(AssertionValidationDetailStatus.FAILED)
                            .error(e)
                            .message(e.getMessage())
                            .build())
                    .build());
        }

        final List<AssertionValidationDetail> fieldValidationDetails = checkFields(assertion);
        if (fieldValidationDetails.isEmpty()) {
            validationDetails.add(AssertionValidationDetail.passed(AssertionValidationKey.REQUIRED_FIELDS));
        } else {
            return Optional.of(ImmutableAssertionValidationResult
                    .builder()
                    .status(AssertionValidationStatus.FAILED)
                    .assertionDetails(assertionDetails)
                    .addAllValidationDetails(validationDetails)
                    .addAllValidationDetails(fieldValidationDetails)
                    .build());
        }

        final List<AssertionValidationDetail> policyValidationDetails = checkPolicies(assertion);
        if (fieldValidationDetails.isEmpty()) {
            validationDetails.add(AssertionValidationDetail.passed(AssertionValidationKey.POLICIES));
        } else {
            return Optional.of(ImmutableAssertionValidationResult
                    .builder()
                    .status(AssertionValidationStatus.FAILED)
                    .assertionDetails(assertionDetails)
                    .addAllValidationDetails(validationDetails)
                    .addAllValidationDetails(policyValidationDetails)
                    .build());
        }

        return Optional.of(ImmutableAssertionValidationResult
                .builder()
                .status(AssertionValidationStatus.PASSED)
                .assertionDetails(assertionDetails)
                .addAllValidationDetails(validationDetails)
                .build());
    }

    private List<AssertionValidationDetail> checkFields(final Assertion assertion) {
        final List<AssertionValidationDetail> failedValidations = new ArrayList<>();
        for (final FieldValidator fieldValidator : getFieldValidators()) {
            try {
                fieldValidator.validate(assertion);
            } catch (final Exception e) {
                failedValidations.add(ImmutableAssertionValidationDetail.builder()
                        .key(AssertionValidationKey.REQUIRED_FIELDS)
                        .status(AssertionValidationDetailStatus.FAILED)
                        .error(e)
                        .message(e.getMessage())
                        .build());
            }
        }
        return failedValidations;
    }

    private List<AssertionValidationDetail> checkPolicies(final Assertion assertion) {
        final List<AssertionValidationDetail> failedValidations = new ArrayList<>();
        for (final PolicyValidator policyValidator : getPolicyValidators()) {
            try {
                policyValidator.validate(assertion);
            } catch (final Exception e) {
                failedValidations.add(ImmutableAssertionValidationDetail.builder()
                        .key(AssertionValidationKey.POLICIES)
                        .status(AssertionValidationDetailStatus.FAILED)
                        .error(e)
                        .message(e.getMessage())
                        .build());
            }
        }
        return failedValidations;
    }


    @FunctionalInterface
    protected interface FieldValidator {
        void validate(Assertion assertion) throws Exception;
    }

    @FunctionalInterface
    protected interface PolicyValidator {
        void validate(Assertion assertion) throws Exception;
    }
}
