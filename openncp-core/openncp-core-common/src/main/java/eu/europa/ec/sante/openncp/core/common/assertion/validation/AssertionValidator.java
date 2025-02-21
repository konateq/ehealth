package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AssertionValidator {
    private final List<AssertionValidation> assertionValidations;

    public AssertionValidator(final List<AssertionValidation> assertionValidations) {
        this.assertionValidations = Validate.notNull(assertionValidations, "assertionValidations must not be null");
    }

    public Optional<AssertionValidationResult> validate(final Assertion assertion, final List<Assertion> allAssertions) {
        Validate.notNull(assertion, "An assertion must not be null in order to be validated.");
        Validate.notNull(allAssertions, "All assertions must not be null in order to be validated.");

        final List<AssertionDetails> allAssertionDetails = allAssertions.stream().map(AssertionDetails::of).collect(Collectors.toList());

        return validate(AssertionDetails.of(assertion), allAssertionDetails);
    }

    public Optional<AssertionValidationResult> validate(final AssertionDetails assertionToValidate, final List<AssertionDetails> allAssertions) {
        return assertionValidations.stream()
                .map(validation -> validation.validate(assertionToValidate, allAssertions))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public List<AssertionValidationResult> validate(final List<Assertion> assertions) {
        return assertions.stream()
                .map(assertionToCheck -> validate(assertionToCheck, assertions))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
