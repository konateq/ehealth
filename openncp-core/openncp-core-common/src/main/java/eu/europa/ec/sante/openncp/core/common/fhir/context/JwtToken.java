package eu.europa.ec.sante.openncp.core.common.fhir.context;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import org.apache.commons.lang3.StringUtils;

@Domain
public interface JwtToken {
    String getToken();

    default String getAuthorizationHeaderValue() {
        return "Bearer " + getToken();
    }

    static JwtToken of(final String authenticationValue) {
        if (StringUtils.isBlank(authenticationValue)) {
            return null;
        } else {
            if (authenticationValue.startsWith("Bearer ")) {
                return ImmutableJwtToken.of(authenticationValue.substring(7));
            } else {
                return ImmutableJwtToken.of(authenticationValue);
            }
        }
    }
}
