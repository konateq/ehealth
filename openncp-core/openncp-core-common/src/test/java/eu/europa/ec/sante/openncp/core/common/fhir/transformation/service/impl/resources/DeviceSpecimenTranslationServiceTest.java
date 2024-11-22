package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DeviceSpecimenMyHealthEu;
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
class DeviceSpecimenTranslationServiceTest extends AbstractTranslationServiceTest {

    @Mock
    private TerminologyService mockedTerminologyService;

    private ResourceTranslationService<DeviceSpecimenMyHealthEu> deviceSpecimenTranslationService;


    @BeforeEach
    void init() {
        deviceSpecimenTranslationService = new DeviceSpecimenTranslationService(mockedTerminologyService);
    }


    @Test
    void testTranslate() throws IOException {
        assertThat(deviceSpecimenTranslationService).isNotNull();

        final DeviceSpecimenMyHealthEu input = parser.parseResource(DeviceSpecimenMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("in/deviceSpecimen-in.json"),
                StandardCharsets.UTF_8));

        // Type
        mockTranslation(mockedTerminologyService, input.getType().getCoding().iterator().next(), "Cardiale septum prothese");

        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();

        final DeviceSpecimenMyHealthEu translated = deviceSpecimenTranslationService.translate(input, errors, warnings, targetLanguageCode);

        final DeviceSpecimenMyHealthEu expectedOutput = parser.parseResource(DeviceSpecimenMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("out/deviceSpecimen-out.json"),
                StandardCharsets.UTF_8));
        System.out.println(parser.encodeResourceToString(expectedOutput));
        System.out.println(parser.encodeResourceToString(translated));

        assertFhirResourcesAreEqual(expectedOutput, translated);
    }
}
