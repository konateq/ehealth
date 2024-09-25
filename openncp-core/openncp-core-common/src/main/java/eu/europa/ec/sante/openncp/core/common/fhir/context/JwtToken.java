package eu.europa.ec.sante.openncp.core.common.fhir.context;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.util.Optional;

@Domain
public interface JwtToken {
    String getToken();

    default String getAuthorizationHeaderValue() {
        return "Bearer " + getToken();
    }

    static Optional<JwtToken> extractFrom(final HttpServletRequest theRequest) {
        if (theRequest == null) {
            return Optional.empty();
        } else {
            return JwtToken.of(theRequest.getHeader(HttpHeaders.AUTHORIZATION));
        }
    }

    static Optional<JwtToken> of(final String authenticationValue) {
        if (StringUtils.isNotBlank(authenticationValue) && authenticationValue.startsWith("Bearer ")) {
            return Optional.of(ImmutableJwtToken.of(authenticationValue.substring(7)));
        } else {
            return Optional.empty();
        }

    }
}
