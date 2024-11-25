package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.SpecimenMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecimenTranslationService extends AbstractResourceTranslationService<SpecimenMyHealthEu> {

    public SpecimenTranslationService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public SpecimenMyHealthEu translateTypedResource(final SpecimenMyHealthEu specimenMyHealthEu,
                                                     final List<ITMTSAMError> errors,
                                                     final List<ITMTSAMError> warnings,
                                                     final String targetLanguage) {

        translateCodeableConcept(specimenMyHealthEu.getType(), errors, warnings, targetLanguage);
        translateCodeableConcept(specimenMyHealthEu.getCollection().getBodySite(), errors, warnings, targetLanguage);

        return specimenMyHealthEu;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Specimen;
    }
}
