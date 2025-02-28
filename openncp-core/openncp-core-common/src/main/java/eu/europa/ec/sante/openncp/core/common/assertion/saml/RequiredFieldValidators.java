package eu.europa.ec.sante.openncp.core.common.assertion.saml;

import eu.europa.ec.sante.openncp.core.common.assertion.AssertionConstants;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.MissingFieldException;
import org.opensaml.saml.saml2.core.Assertion;

import static eu.europa.ec.sante.openncp.core.common.assertion.AssertionHelper.getAttributeFromAssertion;

public class RequiredFieldValidators {

    private RequiredFieldValidators() {
    }

    public static void validateVersion(final Assertion assertion) throws MissingFieldException {
        if (assertion.getVersion() == null) {
            throw (new MissingFieldException("Version attribute is required."));
        }
    }

    public static void validateID(final Assertion assertion) throws MissingFieldException {
        if (assertion.getID() == null) {
            throw (new MissingFieldException("ID attribute is required."));
        }
    }

    public static void validateIssueInstant(final Assertion assertion) throws MissingFieldException {
        if (assertion.getIssueInstant() == null) {
            throw (new MissingFieldException("IssueInstant attribute is required."));
        }
    }

    public static void validateIssuer(final Assertion assertion) throws MissingFieldException {
        if (assertion.getIssuer() == null) {
            throw (new MissingFieldException("Issuer attribute is required."));
        }
    }

    public static void validateSubject(final Assertion assertion) throws MissingFieldException {
        if (assertion.getSubject() == null) {
            throw (new MissingFieldException("Subject element is required."));
        }
    }

    public static void validateNameID(final Assertion assertion) throws MissingFieldException {
        if (assertion.getSubject().getNameID() == null) {
            throw (new MissingFieldException("NameID element is required."));
        }
    }

    public static void validateFormat(final Assertion assertion) throws MissingFieldException {
        if (assertion.getSubject().getNameID().getFormat() == null) {
            throw (new MissingFieldException("Format attribute is required."));
        }
    }

    public static void validateSubjectConfirmation(final Assertion assertion) throws MissingFieldException {
        if (assertion.getSubject().getSubjectConfirmations().isEmpty()) {
            throw (new MissingFieldException("SubjectConfirmation element is required."));
        }
    }

    public static void validateMethod(final Assertion assertion) throws MissingFieldException {
        if (assertion.getSubject().getSubjectConfirmations().get(0).getMethod() == null) {
            throw (new MissingFieldException("Method attribute is required."));
        }
    }

    public static void validateConditions(final Assertion assertion) throws MissingFieldException {
        if (assertion.getConditions() == null) {
            throw (new MissingFieldException("Conditions element is required."));
        }
    }

    public static void validateNotBefore(final Assertion assertion) throws MissingFieldException {
        if (assertion.getConditions().getNotBefore() == null) {
            throw (new MissingFieldException("NotBefore attribute is required."));
        }
    }

    public static void validateNotOnOrAfter(final Assertion assertion) throws MissingFieldException {
        if (assertion.getConditions().getNotOnOrAfter() == null) {
            throw (new MissingFieldException("NotOnOrAfter attribute is required."));
        }
    }

    public static void validateAuthnStatement(final Assertion assertion) throws MissingFieldException {
        if (assertion.getAuthnStatements().isEmpty()) {
            throw (new MissingFieldException("AuthnStatement element is required."));
        }
    }

    public static void validateAuthnInstant(final Assertion assertion) throws MissingFieldException {
        if (assertion.getAuthnStatements().get(0).getAuthnInstant() == null) {
            throw (new MissingFieldException("AuthnInstant attribute is required."));
        }
    }

    public static void validateAuthnContext(final Assertion assertion) throws MissingFieldException {
        if (assertion.getAuthnStatements().get(0).getAuthnContext() == null) {
            throw (new MissingFieldException("AuthnContext element is required."));
        }
    }

    public static void validateAuthnContextClassRef(final Assertion assertion) throws MissingFieldException {
        if (assertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef() == null) {
            throw (new MissingFieldException("AuthnContextClassRef element is required."));
        }
    }

    public static void validateAttributeStatement(final Assertion assertion) throws MissingFieldException {
        if (assertion.getAttributeStatements().isEmpty()) {
            throw (new MissingFieldException("AttributeStatement element is required."));
        }
    }

    public static void validateSignature(final Assertion assertion) throws MissingFieldException {
        if (assertion.getSignature() == null) {
            throw (new MissingFieldException("Signature element is required."));
        }
    }

    public static void validateAdvice(final Assertion assertion) throws MissingFieldException {
        if (assertion.getAdvice() == null) {
            throw (new MissingFieldException("Advice element is required."));
        }
    }

    public static void validateAssertionIdRef(final Assertion assertion) throws MissingFieldException {
        if (assertion.getAdvice().getAssertionIDReferences().isEmpty()) {
            throw (new MissingFieldException("AssertionIdRef element is required."));
        }
    }

    public static void validateNokIdentifiers(final Assertion assertion) throws MissingFieldException {
        getAttributeFromAssertion(assertion, AssertionConstants.URN_EHDSI_NOK_ID);
    }
}
