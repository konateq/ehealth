package eu.europa.ec.sante.openncp.core.server.fhir.transcoding.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ServiceRequestLabMyHealthEu;
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
class ServiceRequestTranscodingServiceTest extends AbstractTranscodingServiceTest {

    @Mock
    private TerminologyService mockedTerminologyService;

    private ResourceTranscodingService<ServiceRequestLabMyHealthEu> serviceRequestTranscodingService;


    @BeforeEach
    void init() {
        serviceRequestTranscodingService = new ServiceRequestTranscodingService(mockedTerminologyService);
    }


    @Test
    void transcode() throws IOException {
        assertThat(serviceRequestTranscodingService).isNotNull();

        final ServiceRequestLabMyHealthEu input = parser.parseResource(ServiceRequestLabMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("in/serviceRequest-in.json"),
                StandardCharsets.UTF_8));

        /** Code **/
        final CodeConcept targetCode = ImmutableCodeConcept.builder()
                .code("18717-9")
                .codeSystemVersion("2.59")
                .displayName("Blood bank studies (set)")
                .codeSystemName("http://loinc.org")
                .codeSystemOid("2.16.840.1.113883.6.1")
                .build();
        mockTranscoding(mockedTerminologyService, input.getCode().getCoding().iterator().next(), targetCode, "en-GB");

        /** Reason code **/
        final CodeConcept targetReasonCode = ImmutableCodeConcept.builder()
                .code("11502-2")
                .codeSystemVersion("2.59")
                .displayName("Laboratory report")
                .codeSystemName("http://loinc.org")
                .codeSystemOid("2.16.840.1.113883.6.1")
                .build();
        mockTranscoding(mockedTerminologyService, input.getReasonCode().iterator().next().getCoding().iterator().next(), targetReasonCode, "en-GB");

        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();
        final ServiceRequestLabMyHealthEu transcoded = serviceRequestTranscodingService.transcode(input, errors, warnings);

        final ServiceRequestLabMyHealthEu output = parser.parseResource(ServiceRequestLabMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("out/serviceRequest-out.json"),
                StandardCharsets.UTF_8));

        assertFhirResourcesAreEqual(output, transcoded);
    }
}
