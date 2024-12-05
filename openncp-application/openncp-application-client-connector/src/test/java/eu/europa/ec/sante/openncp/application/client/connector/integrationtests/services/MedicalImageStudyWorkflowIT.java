package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.application.client.connector.request.ImmutableMedicalImagingStudyRequest;
import eu.europa.ec.sante.openncp.application.client.connector.request.MedicalImagingStudyRequest;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.ObjectFactory;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;


public class MedicalImageStudyWorkflowIT extends BaseIntegrationTest {

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Test
    public void findDocumentReferences_no_searchParams() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final MedicalImagingStudyRequest medicalImagingStudyRequest = ImmutableMedicalImagingStudyRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .build();

        final ResponseEntity<String> responseEntity = clientConnectorService.queryMedicalImagingStudyDocumentReferences(medicalImagingStudyRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    public void findDocumentReferences_with_searchParams() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final MedicalImagingStudyRequest medicalImagingStudyRequest = ImmutableMedicalImagingStudyRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .modalityCode("CT")
                .bodyPartCode("38266002") // SNOMED CT code for whole body
                .build();

        final ResponseEntity<String> responseEntity = clientConnectorService.queryMedicalImagingStudyDocumentReferences(medicalImagingStudyRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
