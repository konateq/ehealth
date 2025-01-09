package eu.europa.ec.sante.openncp.core.common.fhir.context;

public interface RequestMatcher {
    boolean matches(DispatchContext dispatchContext);

    String getPath();
}
