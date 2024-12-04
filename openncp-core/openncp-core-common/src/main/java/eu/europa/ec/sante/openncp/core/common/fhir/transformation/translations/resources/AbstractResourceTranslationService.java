package eu.europa.ec.sante.openncp.core.common.fhir.transformation.translations.resources;


import eu.europa.ec.sante.openncp.core.common.fhir.transformation.utils.ToolingExtensions;
import eu.europa.ec.sante.openncp.core.common.tsam.CodeConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.TSAMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Optional;


public abstract class AbstractResourceTranslationService<R extends Resource> implements ResourceTranslationService<R> {

    private final TerminologyService terminologyService;

    public AbstractResourceTranslationService(final TerminologyService terminologyService) {
        this.terminologyService = Validate.notNull(terminologyService);
    }

    @Override
    public boolean accepts(final Resource resource) {
        Validate.notNull(resource);
        return resource.getResourceType() == getResourceType();
    }

    @Override
    public R translate(final Resource resource, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings, final String targetLanguage) {
        Validate.notNull(resource);
        return this.translateTypedResource(getTypedResource(resource), errors, warnings, targetLanguage);
    }

    private R getTypedResource(final Resource resource) {
        return (R) resource;
    }

    protected abstract R translateTypedResource(R typedResource, List<ITMTSAMError> errors, List<ITMTSAMError> warnings, String targetLanguage);

    private void addTranslation(final CodeableConcept codeableConcept, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings, final String targetLanguage) {
        if (codeableConcept != null) {
            final Optional<Coding> coding = retrieveCoding(codeableConcept);
            coding.ifPresent(value -> {
                final var translatedValue = getTranslation(value, targetLanguage);
                Validate.notNull(translatedValue);
                errors.addAll(CollectionUtils.emptyIfNull(translatedValue.getErrors()));
                warnings.addAll(CollectionUtils.emptyIfNull(translatedValue.getWarnings()));
                if (StringUtils.isNotEmpty(translatedValue.getDesignation())) {
                    ToolingExtensions.addLanguageTranslation(value.getDisplayElement(), targetLanguage, translatedValue.getDesignation());
                }
            });
        }
    }

    private TSAMResponseStructure getTranslation(final Coding coding, final String targetLanguageCode) {
        final CodeConcept codeConcept = CodeConcept.from(coding);
        return terminologyService.getDesignation(codeConcept, targetLanguageCode);
    }

    Optional<Coding> retrieveCoding(final CodeableConcept codeableConcept) {

        return codeableConcept.getCoding()
                .stream()
                .filter(coding -> !coding.getUserSelected())
                .findFirst()
                .or(() -> codeableConcept.getCoding().stream().findFirst());
    }

    protected void translateCodeableConcept(final CodeableConcept codeableConcept, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings, final String targetLanguage) {
        addTranslation(codeableConcept, errors, warnings, targetLanguage);
    }

    protected void translateCodeableConceptsList(final List<CodeableConcept> codeableConcepts, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings, final String targetLanguage) {
        for (final CodeableConcept codeableConcept : codeableConcepts) {
            translateCodeableConcept(codeableConcept, errors, warnings, targetLanguage);
        }
    }
}
