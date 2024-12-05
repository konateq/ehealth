package eu.europa.ec.sante.openncp.core.common.fhir.context;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import eu.europa.ec.sante.openncp.common.Loinc;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

public class PathAndFhirTypeLoincRequestMatcher extends PathRequestMatcher {
    private final Loinc loinc;

    public PathAndFhirTypeLoincRequestMatcher(final String path, final Loinc loinc) {
        super(path);
        this.loinc = Validate.notNull(loinc, "Loinc must not be null");
    }

    @Override
    public boolean matches(final RequestDetails requestDetails) {
        if (requestDetails == null) {
            return false;
        }

        return super.matches(requestDetails) &&
                requestDetails.getParameters().entrySet().stream()
                        .filter(parameterEntry -> "type".equals(parameterEntry.getKey()))
                        .flatMap(parameterEntry -> Arrays.stream(parameterEntry.getValue()))
                        .findFirst()
                        .map(loinc::matches)
                        .orElse(false);
    }
}
