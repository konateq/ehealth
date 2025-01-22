package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorException;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionService;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services.BaseIntegrationTest;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.*;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class EpWorkflowITTest extends BaseIntegrationTest {
    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Autowired
    private AssertionService assertionService;

    //private final DispensationDocCreation dispensationDocCreation = new DispensationDocCreation();

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Test
    void testHappyFlowEp() throws MalformedURLException, MarshallingException {
        final PatientId patientId = createPatientId("1-1234-W8");

        final PatientDemographics patientDemographics = objectFactory.createPatientDemographics();
        patientDemographics.getPatientId().add(patientId);

        final List<PatientDemographics> xcpdResponse = clientConnectorService.queryPatient(buildTrcAssertions(patientId), "BE", patientDemographics);
        assertThat(xcpdResponse).isNotNull().hasSize(1);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.EP_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.EP_TITLE);

        final List<EpsosDocument> xcaListResponse = clientConnectorService.queryDocuments(buildTrcAssertions(patientId), "BE", patientId, List.of(classCode), null);
        assertThat(xcaListResponse).isNotNull().hasSize(2);

        final String uuid = xcaListResponse.get(1).getUuid();
        final String repositoryId = xcaListResponse.get(1).getRepositoryId();
        final var documentId = objectFactory.createDocumentId();
        documentId.setDocumentUniqueId(uuid);
        documentId.setRepositoryUniqueId(repositoryId);

        final EpsosDocument xcaRetrieve = clientConnectorService.retrieveDocument(buildTrcAssertions(patientId), "BE", documentId, "1.3.6.1.4.1.48336", classCode, null);
        assertThat(xcaRetrieve).isNotNull();
        final byte[] base64Binary = xcaRetrieve.getBase64Binary();
        final String cda = new String(base64Binary, StandardCharsets.UTF_8);
        System.out.println(cda);

    }
    @Test
    void testAssertion()throws MalformedURLException, MarshallingException {
        final PatientId patientId= createPatientId("1-1234-W8");

        final PatientDemographics patientDemographics= objectFactory.createPatientDemographics();
        patientDemographics.getPatientId().add(patientId);

        final List<PatientDemographics> xcpdResponse=clientConnectorService.queryPatient(buildTrcAssertionsPhysician(patientId), "BE", patientDemographics);
        assertThat(xcpdResponse).isNotNull().hasSize(1);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.EP_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.EP_TITLE);

        final List<EpsosDocument> xcaListResponse = clientConnectorService.queryDocuments(buildTrcAssertionsPhysician(patientId), "BE", patientId, List.of(classCode), null);
        assertThat(xcaListResponse).isNotNull().hasSize(0);


    }

    @Test
    void testDispensationDiscard()throws MalformedURLException, MarshallingException {

    }
    //build EpsosDocument based on eP
    documentSubmitDocumentRespon sub = clientConnectorService.submitDocument(assertionTypeAssertionMap, "BE", document, objectFactory.createPatientDemographics());




    private PatientId createPatientId(String patientId) {

        final PatientId patientIdObject = objectFactory.createPatientId();
        patientIdObject.setRoot("1.3.6.1.4.1.48336");
        patientIdObject.setExtension(patientId);

        return patientIdObject;
    }

    private Map<AssertionType, Assertion> buildTrcAssertions(final PatientId patientId) throws MalformedURLException, MarshallingException {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertionPharmacist(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, clinicalAssertion);
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);
        return assertions;
    }
    private Map<AssertionType, Assertion> buildTrcAssertionsPhysician(final PatientId patientId) throws MalformedURLException, MarshallingException {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertionPharmacist(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, clinicalAssertion);
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);
        return assertions;
    }
    public String submitDiscard(Assertion clinicianAssertion, @Nullable Assertion nextOfKinAssertion,
                                Assertion treatmentConfirmationAssertion, String patientIdentifier,
                                String purposeOfUse, String repositoryId, String homeCommunityId,
                                String documentIdentifier, String countryCode, DiscardRequest discardRequest) throws ClientConnectorException {

        Map<AssertionType, Assertion> assertionMap = new EnumMap<>(AssertionType.class);
        assertionMap.put(AssertionType.HCP, clinicianAssertion);
        if (nextOfKinAssertion != null) {
            assertionMap.put(AssertionType.NOK, nextOfKinAssertion);
        }
        assertionMap.put(AssertionType.TRC, treatmentConfirmationAssertion);

        var patientDemographics = discardRequest.getPatientDemographics();

        logger.info("Patient in session: '{}'", getSession().getAttribute(patientIdentifier));
        var file = loadMedication(documentIdentifier);
        var discardResponse = new MedicationDispensed.DiscardResponse();
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String eDUid = generateIdentifierExtension();
            String edOid = configurationManager.getProperty("PORTAL_DISPENSATION_OID");
            var document = buildDiscardDispenseDocument("My Testing Portal", edOid, eDUid, fileContent);

            SubmitDocumentResponse documentResponse = clientConnectorService.submitDocument(assertionMap, countryCode,
                    document, patientDemographics);
            logger.info("Submit dispense status: '{}'", documentResponse.getResponseStatus());
            discardResponse.setStatus(documentResponse.getResponseStatus());

        } catch (IOException e) {
            logger.error("IOException: '{}'", e.getMessage());
            discardResponse.setStatus("Failed");
        }

        return discardResponse.getStatus();
    }

    }
