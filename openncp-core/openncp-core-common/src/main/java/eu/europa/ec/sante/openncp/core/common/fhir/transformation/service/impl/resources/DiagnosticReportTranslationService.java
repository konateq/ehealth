package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DiagnosticReportLabMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DiagnosticReportTranslationService extends AbstractResourceTranslationService<DiagnosticReportLabMyHealthEu> {

    public DiagnosticReportTranslationService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public DiagnosticReportLabMyHealthEu translateTypedResource(final DiagnosticReportLabMyHealthEu diagnosticReportLabMyHealthEu,
                                                                final List<ITMTSAMError> errors,
                                                                final List<ITMTSAMError> warnings,
                                                                final String targetLanguage) {

        translateCodeableConceptsList(diagnosticReportLabMyHealthEu.getCategory(), errors, warnings, targetLanguage);
        translateCodeableConcept(diagnosticReportLabMyHealthEu.getCode(), errors, warnings, targetLanguage);

        return diagnosticReportLabMyHealthEu;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.DiagnosticReport;
    }
}
