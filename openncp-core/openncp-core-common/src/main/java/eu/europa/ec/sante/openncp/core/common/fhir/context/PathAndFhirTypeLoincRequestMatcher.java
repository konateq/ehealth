package eu.europa.ec.sante.openncp.core.common.fhir.context;

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
    public boolean matches(final DispatchContext dispatchContext) {
        if (dispatchContext == null || dispatchContext.getHapiRequestDetails().isEmpty()) {
            return false;
        }

        return dispatchContext.getHapiRequestDetails().map(requestDetails -> super.matches(dispatchContext) &&
                        requestDetails.getParameters().entrySet().stream()
                                .filter(parameterEntry -> "type".equals(parameterEntry.getKey()))
                                .flatMap(parameterEntry -> Arrays.stream(parameterEntry.getValue()))
                                .findFirst()
                                .map(loinc::matches)
                                .orElse(false))
                .orElse(false);
    }
}
