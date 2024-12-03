package eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings.resources;

import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.SpecimenMyHealthEu;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Specimen;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SpecimenTranscodingServiceTest extends AbstractTranscodingServiceTest {


    @Test
    public void testTranscode() throws IOException {

        final Specimen input = parser.parseResource(Specimen.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("transcodings/in/specimen-in.json"),
                StandardCharsets.UTF_8));

        final Specimen expectedOutput = parser.parseResource(Specimen.class, IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("transcodings/out/specimen-out.json"),
                StandardCharsets.UTF_8));
        assertFhirResourcesAreEqual(expectedOutput, input);
    }
}
