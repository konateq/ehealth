package eu.europa.ec.sante.openncp.api.common.handler;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.core.common.fhir.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.fhir.transformation.translations.TranslationService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class TranslateBundleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslateBundleHandler.class);

    private final TranslationService translationService;
    private final ConfigurationManager configurationManager;

    public TranslateBundleHandler(final TranslationService translationService, final ConfigurationManager configurationManager) {
        this.translationService = Validate.notNull(translationService);
        this.configurationManager = configurationManager;

    }

    public Bundle handle(final Bundle bundle) {
        final String targetLanguage = configurationManager.getProperty(Constant.LANGUAGE_CODE);
        LOGGER.info("Translating FHIR bundle from MyHealth@EU format into national language [{}]", targetLanguage);
        final TMResponseStructure translatedBundle = translationService.translate(bundle, targetLanguage);
        return Validate.notNull(translatedBundle.getFhirDocument());
    }
}
