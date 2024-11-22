package eu.europa.ec.sante.openncp.common.fhir.context.r4.resources;

import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.Observation;

@ResourceDef(name = "Observation", profile = ObservationResultsLaboratoryMyHealthEu.PROFILE)
public class ObservationResultsLaboratoryMyHealthEu extends Observation {

    public static final String PROFILE = "http://fhir.ehdsi.eu/laboratory/StructureDefinition/Observation-resultslab-lab-myhealtheu";

    @Extension(url = "", definedLocally = false)
    protected PerformerFunctionMyHealthEu performerFunctionMyHealthEu;

    public PerformerFunctionMyHealthEu getPerformerFunctionMyHealthEu() {
        return performerFunctionMyHealthEu;
    }
}
