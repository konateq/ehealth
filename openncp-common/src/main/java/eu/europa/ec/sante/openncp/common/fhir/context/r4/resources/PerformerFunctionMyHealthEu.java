package eu.europa.ec.sante.openncp.common.fhir.context.r4.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.CodeableConcept;

@ResourceDef(name = "performerFunction")
public class PerformerFunctionMyHealthEu extends org.hl7.fhir.r4.model.Extension {

    protected CodeableConcept value;

    @Override
    public CodeableConcept getValue() {
        return value;
    }
}