package eu.europa.ec.sante.openncp.core.server.fhir.transcoding.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DiagnosticReportLabMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiagnosticReportTranscodingService extends AbstractResourceTranscodingService<DiagnosticReportLabMyHealthEu> {

    public DiagnosticReportTranscodingService(final TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.DiagnosticReport;
    }

    @Override
    public DiagnosticReportLabMyHealthEu transcodeTypedResource(final DiagnosticReportLabMyHealthEu diagnosticReportLabMyHealthEu,
                                                                final List<ITMTSAMError> errors,
                                                                final List<ITMTSAMError> warnings) {

        transcodeCodeableConceptsList(diagnosticReportLabMyHealthEu.getCategory(), errors, warnings);
        transcodeCodeableConcept(diagnosticReportLabMyHealthEu.getCode(), errors, warnings);

        return diagnosticReportLabMyHealthEu;
    }
}
