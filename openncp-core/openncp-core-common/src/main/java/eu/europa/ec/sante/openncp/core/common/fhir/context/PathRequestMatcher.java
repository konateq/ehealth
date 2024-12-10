package eu.europa.ec.sante.openncp.core.common.fhir.context;

import org.apache.commons.lang3.Validate;

public class PathRequestMatcher implements RequestMatcher {
    private final String path;

    public PathRequestMatcher(final String path) {
        this.path = Validate.notNull(path, "path must not be null");
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean matches(final DispatchContext dispatchContext) {
        if (dispatchContext == null) {
            return false;
        }

        return path.equals(dispatchContext.getResourceName());
    }
}
