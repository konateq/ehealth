package eu.europa.ec.sante.openncp.common.fhir.context.r4.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;

@ResourceDef(name = "SpecimenMyHealthEu", profile = SpecimenMyHealthEu.PROFILE)
public class SpecimenMyHealthEu extends Specimen {

    public static final String PROFILE = "http://fhir.ehdsi.eu/laboratory/StructureDefinition/Specimen-lab-myhealtheu";
}
