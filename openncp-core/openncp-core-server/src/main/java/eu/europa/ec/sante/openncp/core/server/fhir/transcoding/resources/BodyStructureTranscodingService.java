package eu.europa.ec.sante.openncp.core.server.fhir.transcoding.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.BodyStructureMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BodyStructureTranscodingService extends AbstractResourceTranscodingService<BodyStructureMyHealthEu> {

    public BodyStructureTranscodingService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.BodyStructure;
    }

    @Override
    public BodyStructureMyHealthEu transcodeTypedResource(final BodyStructureMyHealthEu bodyStructureMyHealthEu,
                                                          final List<ITMTSAMError> errors,
                                                          final List<ITMTSAMError> warnings) {

        transcodeCodeableConcept(bodyStructureMyHealthEu.getLaterality().getValue(), errors, warnings);
        transcodeCodeableConcept(bodyStructureMyHealthEu.getMorphology(), errors, warnings);
        transcodeCodeableConcept(bodyStructureMyHealthEu.getLocation(), errors, warnings);
        transcodeCodeableConceptsList(bodyStructureMyHealthEu.getLocationQualifier(), errors, warnings);

        return bodyStructureMyHealthEu;
    }
}
