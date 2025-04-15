package eu.europa.ec.sante.openncp.core.common.assertion.saml;

import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.security.TwoFactorAuthentication;
import eu.europa.ec.sante.openncp.common.security.AssertionConstants;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.MissingFieldException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.europa.ec.sante.openncp.core.common.assertion.AssertionHelper.getAttributeFromAssertion;

public class FieldValueValidators {

    public static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");

    //  A tolerance expressed in second.
    private static final int CONDITIONS_SECOND_RANGE = 60;
    //  Maximum time span for HCP identity assertion (hours).
    private static final int HCP_MAXIMUM_TIME_SPAN = 4;
    //  Maximum time span for trc assertion (hours).
    private static final int TRC_MAXIMUM_TIME_SPAN = 2;

    private FieldValueValidators() {
    }

    public static void validateVersionValue(final Assertion assertion) throws InvalidFieldException {
        if (assertion.getVersion().getMajorVersion() != 2 || assertion.getVersion().getMinorVersion() != 0) {
            throw (new InvalidFieldException("Version must be 2.0"));
        }
    }

    public static void validateIssuerValue(final Assertion assertion) throws InvalidFieldException {
        if (assertion.getIssuer().getValue() == null) {
            throw new InvalidFieldException("Issuer should be filled.");
        } else if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("Issuer: '{}'", assertion.getIssuer().getValue());
        }
    }

    public static void validateNameIDValue(final Assertion assertion) throws InvalidFieldException {
        if (assertion.getSubject().getNameID().getValue() == null) {
            throw (new InvalidFieldException("NameID should be filled."));
        } else if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("Subject Name ID: '{}'", assertion.getSubject().getNameID().getValue());
        }
    }

    public static void validateMethodValue(final Assertion assertion) throws InvalidFieldException {
        if (!StringUtils.equals(assertion.getSubject().getSubjectConfirmations().get(0).getMethod(),
                                "urn:oasis:names:tc:SAML:2.0:cm:sender-vouches")) {
            throw (new InvalidFieldException("Method must be 'urn:oasis:names:tc:SAML:2.0:cm:sender-vouches'"));
        }
    }

    public static void validateNotBeforeValue(final Assertion assertion) throws InvalidFieldException {
        final var instant = DateTime.now();
        if (assertion.getConditions().getNotBefore().isAfter(instant.plus(Duration.standardSeconds(CONDITIONS_SECOND_RANGE)))) {
            throw (new InvalidFieldException("The assertion has been issued in the future. Current time in server is: " + instant +
                                             " However, the starting time of your assertion is: " + assertion.getConditions().getNotBefore()));
        }
    }

    public static void validateNotOnOrAfterValue(final Assertion assertion) throws InvalidFieldException {
        final var instant = DateTime.now();
        if (assertion.getConditions().getNotOnOrAfter().isBefore(instant.minus(org.joda.time.Duration.standardSeconds(CONDITIONS_SECOND_RANGE)))) {
            throw (new InvalidFieldException(
                    "The assertion is not valid now. Current time in server is: " + instant + " However, the ending time of your assertion is: " +
                    assertion.getConditions().getNotOnOrAfter()));
        }
    }

    public static void validateTimeSpanForHCP(final Assertion assertion) throws InvalidFieldException {
        if (assertion.getConditions()
                     .getNotBefore()
                     .isBefore(assertion.getConditions().getNotOnOrAfter().minus(org.joda.time.Duration.standardHours(HCP_MAXIMUM_TIME_SPAN)))) {
            throw (new InvalidFieldException("Maximum time span for HCP Identity Assertion can be at most 4 hours."));
        }
    }

    public static void validateTimeSpanForTRC(final Assertion assertion) throws InvalidFieldException {
        if (assertion.getConditions()
                     .getNotBefore()
                     .isBefore(assertion.getConditions().getNotOnOrAfter().minus(org.joda.time.Duration.standardHours(TRC_MAXIMUM_TIME_SPAN)))) {
            throw (new InvalidFieldException("Maximum time span for TRC Assertion can be at most 2 hours."));
        }
    }

    /**
     * Reference to the HP authentication method. See [OASIS SAML Authn] for a list of valid authentication methods.
     * MUST be one of the following values as defined in [OASIS SAML Authn], which correspond to at least two-factor
     * authentication mechanisms (i.e. high level of authentication):
     * <p/>
     * “urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorUnregistered”
     * “urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract”
     * “urn:oasis:names:tc:SAML:2.0:ac:classes:X509”
     * “urn:oasis:names:tc:SAML:2.0:ac:classes:SPKI”
     * "urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard"
     * “urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI”
     * “urn:oasis:names:tc:SAML:2.0:ac:classes:SoftwarePKI”
     * “urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient"
     */
    public static void validateAuthnContextClassRefValueForHCP(final Assertion assertion) throws InvalidFieldException {
        if (assertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef() == null) {
            if (assertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef() == null) {
                throw (new InvalidFieldException("AuthnContextClassRef should be filled."));
            } else if (!TwoFactorAuthentication.getAuthTypeValues()
                                               .contains(assertion.getAuthnStatements()
                                                                  .get(0)
                                                                  .getAuthnContext()
                                                                  .getAuthnContextClassRef()
                                                                  .getAuthnContextClassRef())) {
                throw (new InvalidFieldException("AuthnContextClassRef element must be a Two Factor Authentication token."));
            }
        }
    }

    public static void validateNextOfKinIdentifiers(final Assertion assertion) throws InvalidFieldException {
        try {
            final String attributeFromAssertion = getAttributeFromAssertion(assertion, AssertionConstants.URN_EHDSI_NAMES_ISM_DOCUMENT_ID);
        } catch (final MissingFieldException e) {
            throw new InvalidFieldException("The next of kin identifier(s) are missing.");
        }

    }
}
