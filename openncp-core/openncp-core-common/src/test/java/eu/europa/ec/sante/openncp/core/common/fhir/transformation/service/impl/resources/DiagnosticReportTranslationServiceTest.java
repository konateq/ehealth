package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DiagnosticReportLabMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DiagnosticReportTranslationServiceTest extends AbstractTranslationServiceTest {

    @Mock
    private TerminologyService mockedTerminologyService;

    private ResourceTranslationService<DiagnosticReportLabMyHealthEu> diagnosticReportTranslationService;


    @BeforeEach
    void init() {
        diagnosticReportTranslationService = new DiagnosticReportTranslationService(mockedTerminologyService);
    }


    @Test
    void testTranslate() throws IOException {
        assertThat(diagnosticReportTranslationService).isNotNull();

        final DiagnosticReportLabMyHealthEu input = parser.parseResource(DiagnosticReportLabMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("in/diagnosticReport-in.json"),
                StandardCharsets.UTF_8));

        // Category: studyType
        mockTranslation(mockedTerminologyService, input.getCategory().iterator().next().getCoding().iterator().next(), "Laboratoriumverslag");

        // Code
        mockTranslation(mockedTerminologyService, input.getCode().getCoding().iterator().next(), "Bloedbankonderzoeken (set)");

        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();

        final DiagnosticReportLabMyHealthEu translated = diagnosticReportTranslationService.translate(input, errors, warnings, targetLanguageCode);

        final DiagnosticReportLabMyHealthEu expectedOutput = parser.parseResource(DiagnosticReportLabMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("out/diagnosticReport-out.json"),
                StandardCharsets.UTF_8));
        assertFhirResourcesAreEqual(expectedOutput, translated);
    }
}
