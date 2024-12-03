package eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings.resources;

import eu.europa.ec.sante.openncp.core.common.fhir.transformation.domain.CodeSystem;
import eu.europa.ec.sante.openncp.core.common.fhir.transformation.utils.ToolingExtensions;
import eu.europa.ec.sante.openncp.core.common.tsam.CodeConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.TSAMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public abstract class AbstractResourceTranscodingService<R extends Resource> implements ResourceTranscodingService<R> {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractResourceTranscodingService.class);

    private final TerminologyService terminologyService;

    public AbstractResourceTranscodingService(final TerminologyService terminologyService) {
        this.terminologyService = Validate.notNull(terminologyService);
    }

    @Override
    public boolean accepts(final Resource resource) {
        Validate.notNull(resource);
        return resource.getResourceType() == getResourceType();
    }

    @Override
    public R transcode(final Resource resource, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings) {
        Validate.notNull(resource);
        return this.transcodeTypedResource(getTypedResource(resource), errors, warnings);
    }

    private R getTypedResource(final Resource resource) {
        return (R) resource;
    }

    protected abstract R transcodeTypedResource(R typedResource, List<ITMTSAMError> errors, List<ITMTSAMError> warnings);

    private Optional<Coding> getTranscoding(final Coding coding, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings) {
        final TSAMResponseStructure tsamTranscodingResponse = terminologyService.getTargetConcept(CodeConcept.from(coding));
        Validate.notNull(tsamTranscodingResponse);
        errors.addAll(CollectionUtils.emptyIfNull(tsamTranscodingResponse.getErrors()));
        warnings.addAll(CollectionUtils.emptyIfNull(tsamTranscodingResponse.getWarnings()));
        if (tsamTranscodingResponse.getCode() != null) {
            final Coding targetCoding = new Coding(CodeSystem.getUrlBasedOnOid(tsamTranscodingResponse.getCodeSystem()),
                    tsamTranscodingResponse.getCode(), tsamTranscodingResponse.getDesignation()).setVersion(tsamTranscodingResponse.getCodeSystemVersion());
            targetCoding.setUserSelected(false);

            final TSAMResponseStructure tsamTranslationResponse = terminologyService.getDesignation(CodeConcept.from(targetCoding), "en-GB");
            Validate.notNull(tsamTranslationResponse);
            errors.addAll(CollectionUtils.emptyIfNull(tsamTranslationResponse.getErrors()));
            warnings.addAll(CollectionUtils.emptyIfNull(tsamTranslationResponse.getWarnings()));
            ToolingExtensions.addLanguageTranslation(targetCoding.getDisplayElement(), "en-GB", tsamTranslationResponse.getDesignation());
            return Optional.of(targetCoding);
        } else {
            LOGGER.warn("No mapping found for code [{}] from codeSystem [{}]", coding.getCode(), coding.getSystem());
            return Optional.empty();
        }
    }

    protected Optional<Coding> retrieveCoding(final CodeableConcept codeableConcept) {
        Validate.notNull(codeableConcept);

        return codeableConcept.getCoding()
                              .stream()
                              .filter(Coding::getUserSelected)
                              .findFirst()
                              .or(() -> codeableConcept.getCoding().stream().findFirst());
    }

    protected void transcodeCodeableConcept(final CodeableConcept codeableConcept, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings) {
        final Optional<Coding> coding = getTranscoding(codeableConcept.getCoding().iterator().next(), errors, warnings);
        coding.ifPresent(transcoding -> codeableConcept.getCoding().add(transcoding));
    }

    protected void transcodeCodeableConceptsList(final List<CodeableConcept> codeableConcepts, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings) {
        for (final CodeableConcept codeableConcept : codeableConcepts) {
            transcodeCodeableConcept(codeableConcept, errors, warnings);
        }
    }
}
