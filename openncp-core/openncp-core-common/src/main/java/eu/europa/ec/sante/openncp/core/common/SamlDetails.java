package eu.europa.ec.sante.openncp.core.common;

import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the SAML used in both the IHE and HL7 FHIR flows
 * and encapsulates information about the SAML assertions that
 * may be used for authorization and identity verification.
 */
@Domain
public interface SamlDetails {
    default Optional<AssertionDetails> getHcpAssertionDetails() {
        return getAssertionDetails(AssertionType.HCP);
    }

    List<AssertionDetails> getAssertions();

    default Optional<AssertionDetails> getAssertionDetails(final AssertionType assertionType) {
        Validate.notNull(assertionType, "assertionType must not be null");

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
        final List<AssertionDetails> allAssertionDetails = assertions.stream()
                .filter(Objects::nonNull)
                .map(AssertionDetails::of)
                .collect(Collectors.toList());

        return ImmutableSamlDetails.builder()
                .addAllAssertions(allAssertionDetails)
                .build();
    }

    static SamlDetails of(final DecodedJWT jwt) {
        Validate.notNull(jwt, "decoded JWT token must not be null");

        final List<AssertionDetails> assertions = new ArrayList<>(3);
        final Optional<AssertionDetails> hcpAssertionDetails = AssertionDetails.of(AssertionType.HCP, jwt);
        hcpAssertionDetails.ifPresent(assertions::add);
        final Optional<AssertionDetails> trcAssertionDetails = AssertionDetails.of(AssertionType.TRC, jwt);
        trcAssertionDetails.ifPresent(assertions::add);
        final Optional<AssertionDetails> nokAssertionDetails = AssertionDetails.of(AssertionType.NOK, jwt);
        nokAssertionDetails.ifPresent(assertions::add);

        return ImmutableSamlDetails.builder()
                .addAllAssertions(assertions)
                .build();
    }
}
