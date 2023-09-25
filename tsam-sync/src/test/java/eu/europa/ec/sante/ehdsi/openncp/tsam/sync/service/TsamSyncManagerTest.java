package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.service;

import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.config.CtsProperties;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.config.DatabaseProperties;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.converter.ConceptConverter;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.converter.ValueSetVersionConverter;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.cts.CtsClient;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain.*;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.repository.*;
import eu.europa.ec.sante.ehdsi.termservice.web.rest.model.sync.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class TsamSyncManagerTest {

    @InjectMocks
    private TsamSyncManager tsamSyncManager;

    @Mock
    private CtsClient ctsClient;

    @Mock
    private CtsProperties ctsProperties;

    @Mock
    private  DatabaseProperties databaseProperties;

    @Mock
    private MappingRepository mappingRepository;

    @Mock
    private  ValueSetRepository valueSetRepository;

    @Mock
    private CodeSystemRepository codeSystemRepository;

    @Mock
    private ValueSetVersionConverter valueSetVersionConverter;

    @Mock
    private ValueSetVersionRepository valueSetVersionRepository;

    @Mock
    private PropertyService propertyService;

    @Mock
    private ConceptConverter conceptConverter;

    @Mock
    private  ConceptRepository conceptRepository;


    @Test
    public void test(){
        //ReflectionTestUtils
        ValueSetCatalogModel valueSetCatalogModel = new ValueSetCatalogModel();
        ValueSetVersionModel valueSetVersionModel = new ValueSetVersionModel();
        valueSetVersionModel.setVersionId("Version 1");
        ValueSetModel valueSetModel = new ValueSetModel();
        valueSetModel.setId("1");
        valueSetVersionModel.setValueSet(valueSetModel);
        valueSetCatalogModel.setValueSetVersions(Arrays.asList(valueSetVersionModel));
        ValueSetVersion valueSetVersion = new ValueSetVersion();
        ValueSet valueSet = new ValueSet();
        valueSet.setId(1L);
        valueSetVersion.setValueSet(valueSet);

        CodeSystemConceptModel codeSystemConceptModel = new CodeSystemConceptModel();
        DesignationModel designationModel = new DesignationModel();
        designationModel.setLanguage("fr");
        DesignationModel designationModel1 = new DesignationModel();
        designationModel1.setLanguage("en");
        codeSystemConceptModel.setDesignations(Arrays.asList(designationModel, designationModel1));

        when(valueSetVersionConverter.convert(any())).thenReturn(valueSetVersion);
        when(ctsClient.fetchCatalogue()).thenReturn(Optional.of(valueSetCatalogModel));
        when(ctsClient.fetchConcepts(any(), anyString(), anyByte(), anyByte())).thenReturn(
                Arrays.asList(codeSystemConceptModel)
        );
        Concept concept = new Concept();
        Designation designation = new Designation();
        designation.setLanguageCode("fr");
        concept.addDesignation(designation);
        Designation designation1 = new Designation();
        designation1.setLanguageCode("en");
        concept.addDesignation(designation1);
        when(conceptConverter.convert(codeSystemConceptModel)).thenReturn(concept);
        when(valueSetVersionRepository.save(any())).thenReturn(valueSetVersion);
        Property property = new Property();
        property.setValue("nl, ge");
        when(propertyService.getProperty(anyString())).thenReturn(property);

        tsamSyncManager.setPageSize(250);

        tsamSyncManager.synchronize();
        assertEquals("nl, ge,fr,en", property.getValue());
    }
  
}