package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DeviceSpecimenMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceSpecimenTranslationService extends AbstractResourceTranslationService<DeviceSpecimenMyHealthEu> {

    public DeviceSpecimenTranslationService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public DeviceSpecimenMyHealthEu translateTypedResource(final DeviceSpecimenMyHealthEu deviceSpecimenMyHealthEu,
                                                           final List<ITMTSAMError> errors,
                                                           final List<ITMTSAMError> warnings,
                                                           final String targetLanguage) {

        translateCodeableConcept(deviceSpecimenMyHealthEu.getType(), errors, warnings, targetLanguage);

        return deviceSpecimenMyHealthEu;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Device;
    }
}