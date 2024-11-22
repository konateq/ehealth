package eu.europa.ec.sante.openncp.common.fhir.context.r4.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.Composition;

@ResourceDef(name = "Composition", profile = CompositionLabReportMyHealthEu.PROFILE)
public class CompositionLabReportMyHealthEu extends Composition implements CustomResource {

    protected static final String PROFILE = "http://fhir.ehdsi.eu/laboratory/StructureDefinition/Composition-lab-myhealtheu";

//    @Child(name = "text", type = { Narrative.class }, order = 5, min = 0, max = 1, modifier = false, summary = false)
//    @Description(shortDefinition = "Narrative text", value = "Narrative text")
//    protected Narrative text;

    //TODO rest of the lab result
}
