package eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings.resources;

import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientTranscodingService extends AbstractResourceTranscodingService<Patient> {

    public PatientTranscodingService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Patient;
    }

    @Override
    public Patient transcodeTypedResource(final Patient patient,
                                          final List<ITMTSAMError> errors,
                                          final List<ITMTSAMError> warnings) {
        return patient;
    }
}
