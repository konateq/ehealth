package eu.europa.ec.sante.openncp.core.server.fhir.transcoding.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DiagnosticReportLabMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.tsam.CodeConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.ImmutableCodeConcept;
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
class DiagnosticReportTranscodingServiceTest extends AbstractTranscodingServiceTest {

    @Mock
    private TerminologyService mockedTerminologyService;

    private ResourceTranscodingService<DiagnosticReportLabMyHealthEu> diagnosticReportResourceTranscodingService;


    @BeforeEach
    void init() {
        diagnosticReportResourceTranscodingService = new DiagnosticReportTranscodingService(mockedTerminologyService);
    }


    @Test
    void transcode() throws IOException {
        assertThat(diagnosticReportResourceTranscodingService).isNotNull();

        final DiagnosticReportLabMyHealthEu input = parser.parseResource(DiagnosticReportLabMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("in/diagnosticReport-in.json"),
                StandardCharsets.UTF_8));

        // Language

        // Category: studyType
        final CodeConcept targetStudyType = ImmutableCodeConcept.builder()
                .code("18717-9")
                .codeSystemVersion("2.59")
                .displayName("Blood bank studies (set)")
                .codeSystemName("http://loinc.org")
                .codeSystemOid("2.16.840.1.113883.6.1")
                .build();
        mockTranscoding(mockedTerminologyService, input.getCategory().iterator().next().getCoding().iterator().next(), targetStudyType, "en-GB");

        // Code
        final CodeConcept targetCode = ImmutableCodeConcept.builder()
                .code("11502-2")
                .codeSystemVersion("2.59")
                .displayName("Laboratory report")
                .codeSystemName("http://loinc.org")
                .codeSystemOid("2.16.840.1.113883.6.1")
                .build();
        mockTranscoding(mockedTerminologyService, input.getCode().getCoding().iterator().next(), targetCode, "en-GB");
        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();
        final DiagnosticReportLabMyHealthEu transcoded = diagnosticReportResourceTranscodingService.transcode(input, errors, warnings);

        final DiagnosticReportLabMyHealthEu output = parser.parseResource(DiagnosticReportLabMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("out/diagnosticReport-out.json"),
                StandardCharsets.UTF_8));

        assertFhirResourcesAreEqual(output, transcoded);
    }
}
