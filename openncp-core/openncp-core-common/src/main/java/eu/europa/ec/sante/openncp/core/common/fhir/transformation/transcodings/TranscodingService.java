package eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings;

import eu.europa.ec.sante.openncp.core.common.fhir.transformation.domain.TMResponseStructure;
import org.hl7.fhir.r4.model.Bundle;

public interface TranscodingService {

    TMResponseStructure transcode(Bundle FHIRDocument);
}
