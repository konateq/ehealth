package eu.europa.ec.sante.openncp.core.server.fhir.transcoding.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ServiceRequestLabMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceRequestTranscodingService extends AbstractResourceTranscodingService<ServiceRequestLabMyHealthEu> {

    public ServiceRequestTranscodingService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.ServiceRequest;
    }

    @Override
    public ServiceRequestLabMyHealthEu transcodeTypedResource(final ServiceRequestLabMyHealthEu serviceRequestLabMyHealthEu,
                                                              final List<ITMTSAMError> errors,
                                                              final List<ITMTSAMError> warnings) {

        transcodeCodeableConcept(serviceRequestLabMyHealthEu.getCode(), errors, warnings);
        transcodeCodeableConceptsList(serviceRequestLabMyHealthEu.getReasonCode(), errors, warnings);

        return serviceRequestLabMyHealthEu;
    }
}
