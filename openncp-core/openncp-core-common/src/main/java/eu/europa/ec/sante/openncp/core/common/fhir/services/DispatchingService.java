package eu.europa.ec.sante.openncp.core.common.fhir.services;

import ca.uhn.fhir.rest.api.MethodOutcome;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

public interface DispatchingService {

    <T extends IBaseResource> T dispatchSearch(DispatchContext requestDetails);

    <T extends IBaseResource> T dispatchRead(DispatchContext requestDetails);

    MethodOutcome dispatchWrite(DispatchContext requestDetails, IBaseResource bundleToCreate);
}
