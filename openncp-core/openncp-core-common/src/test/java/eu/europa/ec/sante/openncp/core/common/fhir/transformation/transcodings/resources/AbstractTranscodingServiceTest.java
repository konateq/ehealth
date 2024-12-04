package eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.europa.ec.sante.openncp.core.common.fhir.transformation.domain.CodeSystem;
import eu.europa.ec.sante.openncp.core.common.tsam.CodeConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.ImmutableCodeConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.TSAMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Assert;

import static org.mockito.Mockito.when;

public abstract class AbstractTranscodingServiceTest {

    final protected FhirContext ctx = FhirContext.forR4();

    // Instantiate a new parser
    final protected IParser parser = ctx.newJsonParser();

    protected void assertFhirResourcesAreEqual(final Resource resource1, final Resource resource2) {
        Assert.assertEquals(parser.encodeResourceToString(resource1), parser.encodeResourceToString(resource2));
    }

    protected void mockTranscoding(final TerminologyService terminologyService, final Coding coding, final CodeConcept targetCodeConcept, final String targetLanguage) {
        final TSAMResponseStructure tsamResponseStructureTranscoding = new TSAMResponseStructure(targetCodeConcept);
        final CodeConcept codeConcept = CodeConcept.from(coding);
        tsamResponseStructureTranscoding.setCode(targetCodeConcept.getCode());
        targetCodeConcept.getCodeSystemOid().ifPresent(codeSystemOid -> tsamResponseStructureTranscoding.setCodeSystem(codeSystemOid));
        targetCodeConcept.getCodeSystemUrl().ifPresent(codeSystemUrl -> tsamResponseStructureTranscoding.setCodeSystem(codeSystemUrl));
        targetCodeConcept.getCodeSystemVersion().ifPresent(codeSystemVersion -> tsamResponseStructureTranscoding.setCodeSystemVersion(codeSystemVersion));
        tsamResponseStructureTranscoding.setCodeSystemName(targetCodeConcept.getCodeSystemName().get());
        tsamResponseStructureTranscoding.setDesignation(targetCodeConcept.getDisplayName().get());
        when(terminologyService.getTargetConcept(codeConcept)).thenReturn(tsamResponseStructureTranscoding);
        final TSAMResponseStructure tsamResponseStructureTranslation = new TSAMResponseStructure(targetCodeConcept);
        tsamResponseStructureTranslation.setDesignation("translation");
        final CodeConcept expectedTargetCodeConcept = ImmutableCodeConcept.builder()
                .code(targetCodeConcept.getCode())
                .codeSystemVersion(targetCodeConcept.getCodeSystemVersion())
                .displayName(targetCodeConcept.getDisplayName())
                .codeSystemName(targetCodeConcept.getCodeSystemName())
                .codeSystemUrl(CodeSystem.getUrlBasedOnOid(targetCodeConcept.getCodeSystemOid().get()))
                .build();
        when(terminologyService.getDesignation(expectedTargetCodeConcept, targetLanguage)).thenReturn(tsamResponseStructureTranslation);
    }
}
