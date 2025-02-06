package eu.europa.ec.sante.openncp.common.fhir.context.r4.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.ServiceRequest;

@ResourceDef(name = "ServiceRequestLabMyHealthEu", profile = DiagnosticReportLabMyHealthEu.PROFILE)
public class ServiceRequestLabMyHealthEu extends ServiceRequest implements CustomResource {

    protected static final String PROFILE = "http://fhir.ehdsi.eu/laboratory/StructureDefinition/ServiceRequest-lab-myhealtheu";
}
