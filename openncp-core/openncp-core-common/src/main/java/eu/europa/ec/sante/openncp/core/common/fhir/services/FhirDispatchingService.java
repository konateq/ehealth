package eu.europa.ec.sante.openncp.core.common.fhir.services;

import ca.uhn.fhir.rest.api.MethodOutcome;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface FhirDispatchingService {

    <T extends IBaseResource> T dispatchSearch(DispatchContext requestDetails);

    <T extends IBaseResource> T dispatchRead(DispatchContext requestDetails);

    MethodOutcome dispatchWrite(DispatchContext requestDetails, IBaseResource bundleToCreate);
}
