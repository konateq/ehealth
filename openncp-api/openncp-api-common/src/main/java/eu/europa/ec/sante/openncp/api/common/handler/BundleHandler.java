package eu.europa.ec.sante.openncp.api.common.handler;

import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import org.hl7.fhir.r4.model.Bundle;

public interface BundleHandler {

    Bundle handle(Bundle bundle, DispatchContext dispatchContext);
}
