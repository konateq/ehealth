package eu.europa.ec.sante.openncp.core.common;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the SAML used in both the IHE and HL7 FHIR flows
 * and encapsulates information about the SAML assertions that
 * may be used for authorization and identity verification.
 */
@Domain
public interface SamlDetails {
    AssertionDetails getHcpAssertion();

    List<AssertionDetails> getAssertions();

    default Optional<AssertionDetails> getAssertion(final AssertionType assertionType) {
        Validate.notNull(assertionType, "assertionType must not be null");

        if (assertionType == AssertionType.HCP) {
            return Optional.of(getHcpAssertion());
        }

        return getAssertions().stream()
                .filter(assertionDetails -> assertionDetails.getAssertionType() == assertionType)
                .findFirst();
    }

    default Map<AssertionType, Assertion> getAssertionMap() {
        return getAssertions().stream().collect(Collectors.toMap(
                AssertionDetails::getAssertionType,
                AssertionDetails::getAssertion,
                (existing, replacement) -> existing  // Handle duplicate keys (keep existing)
        ));
    }

    static SamlDetails of(final List<Assertion> assertions) {
        final List<AssertionDetails> allAssertionDetails = new ArrayList<>();
        AssertionDetails hcpAssertionDetails = null;
        for (final Assertion assertion : assertions) {
            if (assertion != null) {
                final AssertionDetails assertionDetails = AssertionDetails.of(assertion);
                allAssertionDetails.add(assertionDetails);
                if (assertionDetails.getAssertionType() == AssertionType.HCP) {
                    hcpAssertionDetails = assertionDetails;
                }
            }
        }

        if (hcpAssertionDetails == null) {
            throw new RuntimeException("A HCP assertion is mandatory.");
        }

        return ImmutableSamlDetails.builder()
                .hcpAssertion(hcpAssertionDetails)
                .addAllAssertions(allAssertionDetails)
                .build();
    }

    static SamlDetails of(final DecodedJWT jwt) {
        Validate.notNull(jwt, "decoded JWT token must not be null");

        final List<AssertionDetails> assertions = new ArrayList<>(3);

        final AssertionDetails hcpAssertionDetails = AssertionDetails.of(AssertionType.HCP, jwt).orElseThrow(() -> new AuthenticationException("A HCP assertion is mandatory."));
        assertions.add(hcpAssertionDetails);

        final Optional<AssertionDetails> trcAssertionDetails = AssertionDetails.of(AssertionType.TRC, jwt);
        trcAssertionDetails.ifPresent(assertions::add);
        final Optional<AssertionDetails> nokAssertionDetails = AssertionDetails.of(AssertionType.NOK, jwt);
        nokAssertionDetails.ifPresent(assertions::add);

        return ImmutableSamlDetails.builder()
                .hcpAssertion(hcpAssertionDetails)
                .addAllAssertions(assertions)
                .build();
    }


}
