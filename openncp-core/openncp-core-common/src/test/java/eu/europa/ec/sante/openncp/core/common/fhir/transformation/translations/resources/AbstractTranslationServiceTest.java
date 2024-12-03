package eu.europa.ec.sante.openncp.core.common.fhir.transformation.translations.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.europa.ec.sante.openncp.core.common.tsam.CodeConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.TSAMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Assert;

import static org.mockito.Mockito.when;

public abstract class AbstractTranslationServiceTest {

    final private FhirContext ctx = FhirContext.forR4();
    final protected String targetLanguageCode = "nl-BE";

    // Instantiate a new parser
    final protected IParser parser = ctx.newJsonParser();

    protected void mockTranslation (TerminologyService terminologyService, Coding coding, String translation) {
        final CodeConcept codeConcept = CodeConcept.from(coding);
        final TSAMResponseStructure tsamResponseStructure = new TSAMResponseStructure(codeConcept);
        tsamResponseStructure.setDesignation(translation);
        when(terminologyService.getDesignation(codeConcept, targetLanguageCode)).thenReturn(tsamResponseStructure);
    }

    protected void assertFhirResourcesAreEqual(Resource resource1, Resource resource2) {
        Assert.assertEquals(parser.encodeResourceToString(resource1), parser.encodeResourceToString(resource2));
    }
}
