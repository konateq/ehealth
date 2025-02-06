package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ObservationResultsLaboratoryMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ObservationTranslationService extends AbstractResourceTranslationService<ObservationResultsLaboratoryMyHealthEu> {

    public ObservationTranslationService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public ObservationResultsLaboratoryMyHealthEu translateTypedResource(final ObservationResultsLaboratoryMyHealthEu observationResultsLaboratoryMyHealthEu,
                                                                         final List<ITMTSAMError> errors,
                                                                         final List<ITMTSAMError> warnings,
                                                                         final String targetLanguage) {

        translateCodeableConceptsList(observationResultsLaboratoryMyHealthEu.getCategory(), errors, warnings, targetLanguage);
        translateCodeableConcept(observationResultsLaboratoryMyHealthEu.getCode(), errors, warnings, targetLanguage);
        translateCodeableConcept(observationResultsLaboratoryMyHealthEu.getPerformerFunctionMyHealthEu().getValue(), errors, warnings, targetLanguage);
        translateCodeableConcept(observationResultsLaboratoryMyHealthEu.getValueCodeableConcept(), errors, warnings, targetLanguage);
        translateCodeableConceptsList(observationResultsLaboratoryMyHealthEu.getInterpretation(), errors, warnings, targetLanguage);
        translateCodeableConcept(observationResultsLaboratoryMyHealthEu.getMethod(), errors, warnings, targetLanguage);
        for (final Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent : observationResultsLaboratoryMyHealthEu.getReferenceRange()) {
            translateCodeableConcept(observationReferenceRangeComponent.getType(), errors, warnings, targetLanguage);
            translateCodeableConceptsList(observationReferenceRangeComponent.getAppliesTo(), errors, warnings, targetLanguage);
        }
        for (final Observation.ObservationComponentComponent observationComponentComponent : observationResultsLaboratoryMyHealthEu.getComponent()) {
            translateCodeableConcept(observationComponentComponent.getCode(), errors, warnings, targetLanguage);
            translateCodeableConcept(observationComponentComponent.getValueCodeableConcept(), errors, warnings, targetLanguage);
            translateCodeableConceptsList(observationComponentComponent.getInterpretation(), errors, warnings, targetLanguage);
            for (final Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent : observationComponentComponent.getReferenceRange()) {
                translateCodeableConcept(observationReferenceRangeComponent.getType(), errors, warnings, targetLanguage);
                translateCodeableConceptsList(observationReferenceRangeComponent.getAppliesTo(), errors, warnings, targetLanguage);
            }
        }

        return observationResultsLaboratoryMyHealthEu;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Observation;
    }
}
