package eu.europa.ec.sante.openncp.core.server.fhir.transcoding.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CompositionLabReportMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompositionTranscodingService extends AbstractResourceTranscodingService<CompositionLabReportMyHealthEu> {

    public CompositionTranscodingService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Composition;
    }

    @Override
    public CompositionLabReportMyHealthEu transcodeTypedResource(final CompositionLabReportMyHealthEu compositionLabReportMyHealthEu,
                                                                 final List<ITMTSAMError> errors,
                                                                 final List<ITMTSAMError> warnings) {

        transcodeCodeableConcept(compositionLabReportMyHealthEu.getType(), errors, warnings);
        transcodeCodeableConceptsList(compositionLabReportMyHealthEu.getCategory(), errors, warnings);
        for (final Composition.SectionComponent section : compositionLabReportMyHealthEu.getSection()) {
            transcodeCodeableConcept(section.getCode(), errors, warnings);
        }
        return compositionLabReportMyHealthEu;
    }
}
