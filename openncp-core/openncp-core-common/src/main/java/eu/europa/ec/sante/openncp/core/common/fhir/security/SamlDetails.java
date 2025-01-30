package eu.europa.ec.sante.openncp.core.common.fhir.security;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Domain
public interface SamlDetails {
    DecodedJWT getFullJwtToken();

    ClaimDetails getHcpClaim();

    List<ClaimDetails> getClaims();

    default Optional<ClaimDetails> getClaim(final AssertionType assertionType) {
        Validate.notNull(assertionType, "assertionType must not be null");

        if (assertionType == AssertionType.HCP) {
            return Optional.of(getHcpClaim());
        }

        return getClaims().stream()
                .filter(claim -> claim.getAssertionType() == assertionType)
                .findFirst();
    }

    static SamlDetails of(final DecodedJWT jwt) {
        Validate.notNull(jwt, "decoded JWT token must not be null");

        final List<ClaimDetails> claimDetails = new ArrayList<>(3);

        final ClaimDetails hcpClaimDetails = ClaimDetails.of(AssertionType.HCP, jwt).orElseThrow(() -> new AuthenticationException("A HCP claim is mandatory."));
        claimDetails.add(hcpClaimDetails);

        final Optional<ClaimDetails> trcClaimDetails = ClaimDetails.of(AssertionType.TRC, jwt);
        trcClaimDetails.ifPresent(claimDetails::add);
        final Optional<ClaimDetails> nokClaimDetails = ClaimDetails.of(AssertionType.NOK, jwt);
        nokClaimDetails.ifPresent(claimDetails::add);

        return ImmutableSamlDetails.builder()
                .fullJwtToken(jwt)
                .hcpClaim(hcpClaimDetails)
                .addAllClaims(claimDetails)
                .build();
    }


}
