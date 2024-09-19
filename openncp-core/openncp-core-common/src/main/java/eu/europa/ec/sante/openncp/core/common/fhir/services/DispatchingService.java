package eu.europa.ec.sante.openncp.core.common.fhir.services;

import ca.uhn.fhir.rest.api.MethodOutcome;
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface DispatchingService {

    <T extends IBaseResource> T dispatchSearch(EuRequestDetails requestDetails, String JWTToken);

    <T extends IBaseResource> T dispatchRead(EuRequestDetails requestDetails, String JWTToken);

    MethodOutcome dispatchWrite(EuRequestDetails requestDetails, String jwtToken);
}
