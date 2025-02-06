package eu.europa.ec.sante.openncp.common.fhir.context.r4.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DiagnosticReport;

@ResourceDef(name = "DeviceSpecimenMyHealthEu", profile = DiagnosticReportLabMyHealthEu.PROFILE)
public class DeviceSpecimenMyHealthEu extends Device {

    public static final String PROFILE = "http://fhir.ehdsi.eu/laboratory/StructureDefinition/Device-specimen-lab-myhealtheu";
}
