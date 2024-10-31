package eu.europa.ec.sante.openncp.common.fhir.context.r4.resources;

import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.BodyStructure;

@ResourceDef(name = "BodyStructureMyHealthEu", profile = BodyStructureMyHealthEu.PROFILE)
public class BodyStructureMyHealthEu extends BodyStructure {

    public static final String PROFILE = "http://fhir.ehdsi.eu/laboratory/StructureDefinition/BodyStructure-lab-myhealtheu";

    @Extension(url = "", definedLocally = false, isModifier = false)
    protected LateralityMyHealthEu laterality;

    public LateralityMyHealthEu getLaterality() {
        return laterality;
    }
}
