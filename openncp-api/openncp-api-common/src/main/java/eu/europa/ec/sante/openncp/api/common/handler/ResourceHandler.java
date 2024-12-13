package eu.europa.ec.sante.openncp.api.common.handler;

import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ResourceHandler {

    boolean accepts(DispatchContext dispatchContext, IBaseResource resource);

    void handle(DispatchContext dispatchContext, IBaseResource resource);
}
