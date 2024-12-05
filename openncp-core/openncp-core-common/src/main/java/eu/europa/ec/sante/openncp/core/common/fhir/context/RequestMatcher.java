package eu.europa.ec.sante.openncp.core.common.fhir.context;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface RequestMatcher {
    boolean matches(RequestDetails requestDetails);

    String getPath();
}
