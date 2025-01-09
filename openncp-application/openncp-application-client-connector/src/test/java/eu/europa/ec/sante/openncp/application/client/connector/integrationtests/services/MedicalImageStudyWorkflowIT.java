package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.application.client.connector.request.*;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.ObjectFactory;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


public class MedicalImageStudyWorkflowIT extends BaseIntegrationTest {

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Test
    public void findDocumentReferences_no_searchParams() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(this.keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final MedicalImagingStudyRequest medicalImagingStudyRequest = ImmutableMedicalImagingStudyRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .build();

        final ResponseEntity<String> responseEntity = this.clientConnectorService.queryMedicalImagingStudyDocumentReferences(medicalImagingStudyRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    public void findDocumentReferences_with_searchParams() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(this.keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final MedicalImagingStudyRequest medicalImagingStudyRequest = ImmutableMedicalImagingStudyRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .procedureDateBetweenRange(ImmutableDateRange.builder()
                        .from(LocalDate.of(2024, Month.DECEMBER, 5))
                        .to(LocalDate.of(2024, Month.DECEMBER, 5))
                        .build()
                )
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .modalityCode("CT")
                .bodyPartCode("38266002") // SNOMED CT code for whole body
                .build();

        final ResponseEntity<String> responseEntity = this.clientConnectorService.queryMedicalImagingStudyDocumentReferences(medicalImagingStudyRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    public void findDocumentReferenceById() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(this.keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final DocumentReferenceByIdRequest documentReferenceByIdRequest = ImmutableDocumentReferenceByIdRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .id("110053")
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .build();

        final ResponseEntity<String> responseEntity = this.clientConnectorService.queryDocumentReferenceByIdFhir(documentReferenceByIdRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
    }

    /**
     * For now this test only works if you upload the dicom file (.dcm) from
     * <a href="https://citnet.tech.ec.europa.eu/CITnet/jira/browse/EHEALTH-12584">EHEALTH-12584</a> to the dicom docker
     * instance (orthanc) since it contains the correct UIDs to make this test work.
     */
    @Test
    public void getMedicalImage_wadors_only_study() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(this.keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final FetchMedicalImagesRequest fetchMedicalImagesRequest = ImmutableFetchMedicalImagesRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .studyUid("1.2.276.0.7230010.3.1.2.296485376.1.1521713414.1800996")
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .build();

        final ResponseEntity<byte[]> responseEntity = this.clientConnectorService.fetchMedicalImagesRequest(fetchMedicalImagesRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(responseEntity.getBody()).isNotNull();
    }

    /**
     * For now this test only works if you upload the dicom file (.dcm) from
     * <a href="https://citnet.tech.ec.europa.eu/CITnet/jira/browse/EHEALTH-12584">EHEALTH-12584</a> to the dicom docker
     * instance (orthanc) since it contains the correct UIDs to make this test work.
     */
    @Test
    public void getMedicalImage_wadors_study_and_series() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(this.keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final FetchMedicalImagesRequest fetchMedicalImagesRequest = ImmutableFetchMedicalImagesRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .studyUid("1.2.276.0.7230010.3.1.2.296485376.1.1521713414.1800996")
                .seriesUid("1.2.276.0.7230010.3.1.3.296485376.1.1521713419.1802493")
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .build();

        final ResponseEntity<byte[]> responseEntity = this.clientConnectorService.fetchMedicalImagesRequest(fetchMedicalImagesRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(responseEntity.getBody()).isNotNull();
    }

    /**
     * For now this test only works if you upload the dicom file (.dcm) from
     * <a href="https://citnet.tech.ec.europa.eu/CITnet/jira/browse/EHEALTH-12584">EHEALTH-12584</a> to the dicom docker
     * instance (orthanc) since it contains the correct UIDs to make this test work.
     */
    @Test
    public void getMedicalImage_wadors_study_series_and_instance() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(this.keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final FetchMedicalImagesRequest fetchMedicalImagesRequest = ImmutableFetchMedicalImagesRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .studyUid("1.2.276.0.7230010.3.1.2.296485376.1.1521713414.1800996")
                .seriesUid("1.2.276.0.7230010.3.1.3.296485376.1.1521713419.1802493")
                .instanceUid("1.2.276.0.7230010.3.1.4.296485376.1.1521713419.1802510")
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .build();

        final ResponseEntity<byte[]> responseEntity = this.clientConnectorService.fetchMedicalImagesRequest(fetchMedicalImagesRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(responseEntity.getBody()).isNotNull();
    }

    @Test
    public void fullWorkflow() throws JsonProcessingException {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(this.keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final MedicalImagingStudyRequest medicalImagingStudyRequest = ImmutableMedicalImagingStudyRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .procedureDateBetweenRange(ImmutableDateRange.builder()
                        .from(LocalDate.of(2024, Month.DECEMBER, 5))
                        .to(LocalDate.of(2024, Month.DECEMBER, 5))
                        .build()
                )
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .modalityCode("CT")
                .bodyPartCode("38266002") // SNOMED CT code for whole body
                .build();

        final ResponseEntity<String> responseEntityForMedicalImagingStudyDocumentReference = this.clientConnectorService.queryMedicalImagingStudyDocumentReferences(medicalImagingStudyRequest);
        assertThat(responseEntityForMedicalImagingStudyDocumentReference).isNotNull();
        assertThat(responseEntityForMedicalImagingStudyDocumentReference.getStatusCode().is2xxSuccessful()).isTrue();
        final FhirContext fhirContext = FhirContext.forR4();
        final IParser parser = fhirContext.newJsonParser();
        final Bundle returnedBundle = parser.parseResource(Bundle.class, responseEntityForMedicalImagingStudyDocumentReference.getBody());
        final DocumentReference documentReference = (DocumentReference) returnedBundle.getEntry().iterator().next().getResource();
        final String url = documentReference.getContent().iterator().next().getAttachment().getUrl();
        final String documentReferenceId = url.substring(url.lastIndexOf("/") + 1);

        final DocumentReferenceByIdRequest documentReferenceByIdRequest = ImmutableDocumentReferenceByIdRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .id(documentReferenceId)
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .build();

        final ResponseEntity<String> responseEntityForDocumentReferenceById = this.clientConnectorService.queryDocumentReferenceByIdFhir(documentReferenceByIdRequest);
        assertThat(responseEntityForDocumentReferenceById).isNotNull();
        assertThat(responseEntityForDocumentReferenceById.getStatusCode().is2xxSuccessful()).isTrue();

        final DocumentReference returnedDocumentReference = parser.parseResource(DocumentReference.class, responseEntityForDocumentReferenceById.getBody());
        String studyInstanceUid = null;
        String seriesInstanceUid = null;
        String instanceUid = null;
        for (DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent : returnedDocumentReference.getContent()) {
            if (documentReferenceContentComponent.getAttachment().getContentType().equals(MediaType.APPLICATION_JSON.toString())) {
                final String returnedDicomStudy = new String(documentReferenceContentComponent.getAttachment().getData(), StandardCharsets.UTF_8);
                ObjectMapper mapper = new ObjectMapper();
                assertThat(returnedDicomStudy.contains("1.2.276.0.7230010.3.1.2.296485376.1.1521713414.1800996"));
                studyInstanceUid = "1.2.276.0.7230010.3.1.2.296485376.1.1521713414.1800996";
                assertThat(returnedDicomStudy.contains("1.2.276.0.7230010.3.1.3.296485376.1.1521713419.1802493"));
                seriesInstanceUid = "1.2.276.0.7230010.3.1.3.296485376.1.1521713419.1802493";
                assertThat(returnedDicomStudy.contains("1.2.276.0.7230010.3.1.4.296485376.1.1521713419.1802510"));
                instanceUid = "1.2.276.0.7230010.3.1.4.296485376.1.1521713419.1802510";
            }
        }

        final FetchMedicalImagesRequest fetchMedicalImagesRequest = ImmutableFetchMedicalImagesRequest.builder()
                .countryCode("BE")
                .patientId(patientId)
                .studyUid(studyInstanceUid)
                .seriesUid(seriesInstanceUid)
                .instanceUid(instanceUid)
                .putAssertion(AssertionType.HCP, clinicalAssertion)
                .build();

        final ResponseEntity<byte[]> responseEntity = this.clientConnectorService.fetchMedicalImagesRequest(fetchMedicalImagesRequest);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(responseEntity.getBody()).isNotNull();
    }
}
