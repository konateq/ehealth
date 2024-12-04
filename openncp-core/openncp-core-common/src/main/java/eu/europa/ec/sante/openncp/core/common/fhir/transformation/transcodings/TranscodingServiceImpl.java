package eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings;

import eu.europa.ec.sante.openncp.core.common.fhir.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings.resources.ResourceTranscodingService;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TranscodingServiceImpl implements TranscodingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscodingServiceImpl.class);

    private final List<ResourceTranscodingService<? extends Resource>> resourceTranscodingServices;

    public TranscodingServiceImpl(final List<ResourceTranscodingService<? extends Resource>> resourceTranscodingServices) {
        this.resourceTranscodingServices = Validate.notNull(resourceTranscodingServices, "resourceTranscodingServices must not be null");
    }

    @Override
    public TMResponseStructure transcode(final Bundle fhirDocument) {
        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();
        fhirDocument.getEntry().forEach(bundleEntryComponent -> {
            final Resource resource = bundleEntryComponent.getResource();
            retrieveTranscodingLogic(resource).ifPresentOrElse(resourceTranscodingService -> resourceTranscodingService.transcode(resource, errors, warnings),
                                                               () -> LOGGER.warn("No transcoding logic service found for resource [{}]", resource));
        });

        return new TMResponseStructure(fhirDocument, "success", errors, warnings);
    }

    private Optional<ResourceTranscodingService<?>> retrieveTranscodingLogic(final Resource resource) {
        return resourceTranscodingServices.stream().filter(resourceTranscodingService -> resourceTranscodingService.accepts(resource)).findFirst();
    }
}
