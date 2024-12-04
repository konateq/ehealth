package eu.europa.ec.sante.openncp.core.server.fhir.transcoding.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ObservationResultsLaboratoryMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ObservationTranscodingService extends AbstractResourceTranscodingService<ObservationResultsLaboratoryMyHealthEu> {

    public ObservationTranscodingService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Observation;
    }

    @Override
    public ObservationResultsLaboratoryMyHealthEu transcodeTypedResource(final ObservationResultsLaboratoryMyHealthEu observationResultsLaboratoryMyHealthEu,
                                                                         final List<ITMTSAMError> errors,
                                                                         final List<ITMTSAMError> warnings) {

        transcodeCodeableConceptsList(observationResultsLaboratoryMyHealthEu.getCategory(), errors, warnings);
        transcodeCodeableConcept(observationResultsLaboratoryMyHealthEu.getCode(), errors, warnings);
        transcodeCodeableConcept(observationResultsLaboratoryMyHealthEu.getPerformerFunctionMyHealthEu().getValue(), errors, warnings);
        transcodeCodeableConcept(observationResultsLaboratoryMyHealthEu.getValueCodeableConcept(), errors, warnings);
        transcodeCodeableConceptsList(observationResultsLaboratoryMyHealthEu.getInterpretation(), errors, warnings);
        transcodeCodeableConcept(observationResultsLaboratoryMyHealthEu.getMethod(), errors, warnings);
        for (final Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent : observationResultsLaboratoryMyHealthEu.getReferenceRange()) {
            transcodeCodeableConcept(observationReferenceRangeComponent.getType(), errors, warnings);
            transcodeCodeableConceptsList(observationReferenceRangeComponent.getAppliesTo(), errors, warnings);
        }
        for (final Observation.ObservationComponentComponent observationComponentComponent : observationResultsLaboratoryMyHealthEu.getComponent()) {
            transcodeCodeableConcept(observationComponentComponent.getCode(), errors, warnings);
            transcodeCodeableConcept(observationComponentComponent.getValueCodeableConcept(), errors, warnings);
            transcodeCodeableConceptsList(observationComponentComponent.getInterpretation(), errors, warnings);
            for (final Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent : observationComponentComponent.getReferenceRange()) {
                transcodeCodeableConcept(observationReferenceRangeComponent.getType(), errors, warnings);
                transcodeCodeableConceptsList(observationReferenceRangeComponent.getAppliesTo(), errors, warnings);
            }
        }
        return observationResultsLaboratoryMyHealthEu;
    }
}
