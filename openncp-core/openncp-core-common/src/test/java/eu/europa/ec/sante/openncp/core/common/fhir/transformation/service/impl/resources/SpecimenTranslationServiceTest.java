package eu.europa.ec.sante.openncp.core.common.fhir.transformation.service.impl.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.SpecimenMyHealthEu;
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
class SpecimenTranslationServiceTest extends AbstractTranslationServiceTest {

    @Mock
    private TerminologyService mockedTerminologyService;

    private ResourceTranslationService<SpecimenMyHealthEu> specimenTranslationService;


    @BeforeEach
    void init() {
        specimenTranslationService = new SpecimenTranslationService(mockedTerminologyService);
    }


    @Test
    void testTranslate() throws IOException {
        assertThat(specimenTranslationService).isNotNull();

        final SpecimenMyHealthEu input = parser.parseResource(SpecimenMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("in/specimen-in.json"),
                StandardCharsets.UTF_8));

        // Type
        mockTranslation(mockedTerminologyService, input.getType().getCoding().iterator().next(), "Bloedmonster");

        // Collection - Body Site
        mockTranslation(mockedTerminologyService, input.getCollection().getBodySite().getCoding().iterator().next(), "Neoaortaklep");

        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();

        final SpecimenMyHealthEu translated = specimenTranslationService.translate(input, errors, warnings, targetLanguageCode);

        final SpecimenMyHealthEu expectedOutput = parser.parseResource(SpecimenMyHealthEu.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("out/specimen-out.json"),
                StandardCharsets.UTF_8));
        assertFhirResourcesAreEqual(expectedOutput, translated);
    }
}
