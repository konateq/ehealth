package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ServiceRequestLabMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceRequestTranslationService extends AbstractResourceTranslationService<ServiceRequestLabMyHealthEu> {

    public ServiceRequestTranslationService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public ServiceRequestLabMyHealthEu translateTypedResource(final ServiceRequestLabMyHealthEu serviceRequest,
                                                              final List<ITMTSAMError> errors,
                                                              final List<ITMTSAMError> warnings,
                                                              final String targetLanguage) {

        translateCodeableConcept(serviceRequest.getCode(), errors, warnings, targetLanguage);
        translateCodeableConceptsList(serviceRequest.getReasonCode(), errors, warnings, targetLanguage);

        return serviceRequest;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.ServiceRequest;
    }
}
