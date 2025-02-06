package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ServiceRequestLabMyHealthEu;
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
class ServiceRequestTranslationServiceTest extends AbstractTranslationServiceTest {

    @Mock
    private TerminologyService mockedTerminologyService;

    private ResourceTranslationService<ServiceRequestLabMyHealthEu> serviceRequestTranslationService;


    @BeforeEach
    void init() {
        serviceRequestTranslationService = new ServiceRequestTranslationService(mockedTerminologyService);
    }


    @Test
    void testTranslate() throws IOException {
        assertThat(serviceRequestTranslationService).isNotNull();

        final ServiceRequestLabMyHealthEu input = parser.parseResource(ServiceRequestLabMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("in/serviceRequest-in.json"),
                StandardCharsets.UTF_8));

        // Code
        mockTranslation(mockedTerminologyService, input.getCode().getCoding().iterator().next(), "Hemoglobine- en hematocrietpaneel - Bloed");

        // ReasonCode
        mockTranslation(mockedTerminologyService, input.getReasonCode().iterator().next().getCoding().iterator().next(), "Gele koorts");

        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();


        final ServiceRequestLabMyHealthEu translated = serviceRequestTranslationService.translate(input, errors, warnings, targetLanguageCode);

        System.out.println(parser.encodeResourceToString(translated));

        final ServiceRequestLabMyHealthEu expectedOutput = parser.parseResource(ServiceRequestLabMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("out/serviceRequest-out.json"),
                StandardCharsets.UTF_8));
        assertFhirResourcesAreEqual(expectedOutput, translated);
    }
}
