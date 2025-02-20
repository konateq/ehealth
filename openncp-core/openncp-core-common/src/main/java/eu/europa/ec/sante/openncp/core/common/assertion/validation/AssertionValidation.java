package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;


public interface AssertionValidation {
    AssertionValidationResult validate(AssertionDetails assertionDetails);

    default AssertionValidationResult validate(Assertion assertion) {
        Validate.notNull(assertion, "An assertion must not be null in order to be validated.");

        final AssertionDetails assertionDetails = AssertionDetails.of(assertion);
        return validate(assertionDetails);
    }
}
