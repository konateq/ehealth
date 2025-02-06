package eu.europa.ec.sante.openncp.common.fhir.context.r4.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.DiagnosticReport;

@ResourceDef(name = "DiagnosticReport", profile = DiagnosticReportLabMyHealthEu.PROFILE)
public class DiagnosticReportLabMyHealthEu extends DiagnosticReport implements CustomResource {

    protected static final String PROFILE = "http://fhir.ehdsi.eu/laboratory/StructureDefinition/DiagnosticReport-lab-myhealtheu";
}
