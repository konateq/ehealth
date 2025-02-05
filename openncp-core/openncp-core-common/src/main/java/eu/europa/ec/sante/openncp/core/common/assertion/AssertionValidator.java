package eu.europa.ec.sante.openncp.core.common.assertion;

import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AssertionValidator {
    private final List<AssertionValidation> assertionValidations;

    public AssertionValidator(final List<AssertionValidation> assertionValidations) {
        this.assertionValidations = Validate.notNull(assertionValidations, "assertionValidations must not be null");
    }

    public Optional<AssertionValidationResult> validate(final Assertion assertion) {
        Validate.notNull(assertion, "An assertion must not be null in order to be validated.");
        return validate(AssertionDetails.of(assertion));
    }

    public Optional<AssertionValidationResult> validate(final AssertionDetails assertionDetails) {
        return assertionValidations.stream()
                .map(assertionValidation -> assertionValidation.validate(assertionDetails))
                .filter(AssertionValidationResult::isCorrectType)
                .findFirst();
    }
}
