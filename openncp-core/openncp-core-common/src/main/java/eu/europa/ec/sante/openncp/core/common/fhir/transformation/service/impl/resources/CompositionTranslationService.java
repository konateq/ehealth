package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CompositionLabReportMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompositionTranslationService extends AbstractResourceTranslationService<CompositionLabReportMyHealthEu> {

    public CompositionTranslationService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public CompositionLabReportMyHealthEu translateTypedResource(final CompositionLabReportMyHealthEu compositionLabReportMyHealthEu,
                                                                 final List<ITMTSAMError> errors,
                                                                 final List<ITMTSAMError> warnings,
                                                                 final String targetLanguage) {

        translateCodeableConcept(compositionLabReportMyHealthEu.getType(), errors, warnings, targetLanguage);
        translateCodeableConceptsList(compositionLabReportMyHealthEu.getCategory(), errors, warnings, targetLanguage);
        for (final Composition.SectionComponent sectionComponent : compositionLabReportMyHealthEu.getSection()) {
            translateSection(sectionComponent, errors, warnings, targetLanguage);
        }
        return compositionLabReportMyHealthEu;
    }

    private void translateSection(final Composition.SectionComponent sectionComponent,
                                  final List<ITMTSAMError> errors,
                                  final List<ITMTSAMError> warnings,
                                  final String targetLanguage) {
        translateCodeableConcept(sectionComponent.getCode(), errors, warnings, targetLanguage);
        for (final Composition.SectionComponent nestedSectionComponent : sectionComponent.getSection()) {
            translateSection(nestedSectionComponent, errors, warnings, targetLanguage);
        }
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Composition;
    }
}
