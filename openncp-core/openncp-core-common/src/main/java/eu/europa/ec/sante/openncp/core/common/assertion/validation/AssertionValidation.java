package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public interface AssertionValidation {
    default Optional<AssertionValidationResult> validate(final Assertion assertionToCheck, final List<Assertion> allAssertions) {
        Validate.notNull(assertionToCheck, "An assertion must not be null in order to be validated.");
        Validate.notNull(allAssertions, "The list of assertions must not be null in order to be validated.");

        final AssertionDetails assertionDetails = AssertionDetails.of(assertionToCheck);
        final List<AssertionDetails> allAssertionDetails = allAssertions.stream().map(AssertionDetails::of).collect(Collectors.toList());
        return validate(assertionDetails, allAssertionDetails);
    }

    Optional<AssertionValidationResult> validate(AssertionDetails assertionToCheck, List<AssertionDetails> allAssertions);
}
