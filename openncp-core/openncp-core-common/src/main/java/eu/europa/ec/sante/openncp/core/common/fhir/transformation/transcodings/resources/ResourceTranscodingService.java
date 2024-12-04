package eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings.resources;

import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import java.util.List;

public interface ResourceTranscodingService<R extends Resource> {

    ResourceType getResourceType();

    boolean accepts(final Resource resource);

    R transcode(final Resource resource, List<ITMTSAMError> errors, List<ITMTSAMError> warnings);

}
