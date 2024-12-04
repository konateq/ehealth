package eu.europa.ec.sante.openncp.core.common.fhir.transformation.translations;

import eu.europa.ec.sante.openncp.core.common.fhir.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.fhir.transformation.exception.TranslationException;
import eu.europa.ec.sante.openncp.core.common.fhir.transformation.translations.resources.ResourceTranslationService;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class TranslationServiceImpl implements TranslationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationServiceImpl.class);

    private final List<ResourceTranslationService<? extends Resource>> resourceTranslationServices;

    public TranslationServiceImpl(final List<ResourceTranslationService<? extends Resource>> resourceTranslationServices) {
        this.resourceTranslationServices = Validate.notNull(resourceTranslationServices);
    }

    @Override
    public TMResponseStructure translate(final Bundle fhirDocument, final String targetLanguage) {
        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = Collections.emptyList();
        fhirDocument.getEntry().forEach(bundleEntryComponent -> {
            final Resource resource = bundleEntryComponent.getResource();
            retrieveTranslationLogic(resource)
                    .map(resourceTranslationService -> resourceTranslationService.translate(resource, errors, warnings, targetLanguage))
                    .orElseThrow(() -> new TranslationException(String.format("No transcoding logic service found for resource [%s]", resource)));
        });

        return new TMResponseStructure(fhirDocument, "success", errors, warnings);
    }

    private Optional<ResourceTranslationService<?>> retrieveTranslationLogic(final Resource resource) {
        return resourceTranslationServices.stream().filter(resourceTranslationService -> resourceTranslationService.accepts(resource)).findFirst();
    }
}
