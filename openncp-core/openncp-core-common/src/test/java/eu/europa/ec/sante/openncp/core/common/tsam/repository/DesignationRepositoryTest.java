package eu.europa.ec.sante.openncp.core.common.tsam.repository;

import eu.europa.ec.sante.openncp.core.common.DummyApplication;
import eu.europa.ec.sante.openncp.core.common.tsam.domain.CodeSystemConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.domain.Designation;
import eu.europa.ec.sante.openncp.core.common.tsam.domain.ValueSetVersion;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = DummyApplication.class)
@RunWith(SpringRunner.class)
public class DesignationRepositoryTest {

    private static boolean setUpIsDone = false;

    private static final String CURRENT_STATUS = "current";

    private static long valueSetVersionId1;
    private static long valueSetVersionId2;

    @Autowired
    private ValueSetVersionRepository valueSetVersionRepository;

    @Autowired
    private DesignationRepository designationRepository;

    @Test
    public void testFindByValueSetVersionId() {
        List<Designation> designations = designationRepository.findByValueSetVersionId(valueSetVersionId1);
        Assert.assertFalse(designations.isEmpty());
        Assert.assertEquals(2, designations.size());
    }

    @Test
    public void testGetDistinctLanguageCode() {
        List<String> designations = designationRepository.findAllAvailableLanguageCodes();
        Assert.assertFalse(designations.isEmpty());
        Assert.assertEquals(1, designations.size());
        Assert.assertEquals("en-GB", designations.get(0));
    }

    @Before
    public void prefillDatabase() {
        if (!setUpIsDone) {
            final ValueSetVersion vsv1 = valueSetVersionRepository.save(buildValueSetVersion(CURRENT_STATUS, "202310"));
            valueSetVersionId1 = vsv1.getId();
            final ValueSetVersion vsv2 = valueSetVersionRepository.save(buildValueSetVersion(CURRENT_STATUS, "202210"));
            valueSetVersionId2 = vsv2.getId();
            setUpIsDone=true;
        }
    }

    public ValueSetVersion buildValueSetVersion(String status, String description) {

        var valueSetVersion = new ValueSetVersion();
        valueSetVersion.setDescription(description);
        valueSetVersion.setStatus(status);
        valueSetVersion.addConcept(buildCodeSystemConcept("123", "active"));
        valueSetVersion.addConcept(buildCodeSystemConcept("456", "active"));
        return valueSetVersion;
    }

    private CodeSystemConcept buildCodeSystemConcept(String code, String status) {
        var codeSystemConcept = new CodeSystemConcept();
        codeSystemConcept.setCode(code);
        codeSystemConcept.setStatus(status);
        codeSystemConcept.addDesignation(buildDesignation("en-GB", code+"_translation"));
        return codeSystemConcept;
    }

    private Designation buildDesignation(String languageCode, String translation) {
        var designation = new Designation();
        designation.setLanguageCode(languageCode);
        designation.setDesignation(translation);
        return designation;
    }
}
