package eu.europa.ec.sante.openncp.core.server.fhir.transcoding.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DeviceSpecimenMyHealthEu;
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
class DeviceSpecimenTranscodingServiceTest extends AbstractTranscodingServiceTest {

    @Mock
    private TerminologyService mockedTerminologyService;

    private ResourceTranscodingService<DeviceSpecimenMyHealthEu> deviceSpecimenTranscodingService;


    @BeforeEach
    void init() {
        deviceSpecimenTranscodingService = new DeviceSpecimenTranscodingService(mockedTerminologyService);
    }


    @Test
    void transcode() throws IOException {
        assertThat(deviceSpecimenTranscodingService).isNotNull();

        final DeviceSpecimenMyHealthEu input = parser.parseResource(DeviceSpecimenMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("in/deviceSpecimen-in.json"),
                StandardCharsets.UTF_8));

        // Type
        final CodeConcept targetType = ImmutableCodeConcept.builder()
                .code("P0909")
                .codeSystemVersion("1.1")
                .displayName("KNEE PROSTHESES")
                .codeSystemName("http://snomed.info/sct")
                .codeSystemOid("2.16.840.1.113883.6.96")
                .build();
        mockTranscoding(mockedTerminologyService, input.getType().getCoding().iterator().next(), targetType, "en-GB");

        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();
        final DeviceSpecimenMyHealthEu transcoded = deviceSpecimenTranscodingService.transcode(input, errors, warnings);

        System.out.println(parser.encodeResourceToString(transcoded));

        final DeviceSpecimenMyHealthEu output = parser.parseResource(DeviceSpecimenMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("out/deviceSpecimen-out.json"),
                StandardCharsets.UTF_8));

        assertFhirResourcesAreEqual(output, transcoded);
    }
}
