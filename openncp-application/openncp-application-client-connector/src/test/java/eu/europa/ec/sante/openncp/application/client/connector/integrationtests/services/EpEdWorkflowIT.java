package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorException;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionService;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.model.DispenseRequest;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.model.PackageSize;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.util.CDAUtils;
import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.*;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class EpEdWorkflowIT extends BaseIntegrationTest {

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Autowired
    private AssertionService assertionService;

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Test
    void testHappyFlow() throws ClientConnectorException, IOException, MarshallingException, ParserConfigurationException, SAXException {

        final PatientId patientId = createPatientId("1-1234-W8");

        final PatientDemographics patientDemographics = objectFactory.createPatientDemographics();
        patientDemographics.getPatientId().add(patientId);

        final List<PatientDemographics> xcpdResponse = clientConnectorService.queryPatient(buildAssertionMapForPharmacist(patientId), "BE", patientDemographics);
        assertThat(xcpdResponse).isNotNull().hasSize(1);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.EP_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.EP_TITLE);

        final List<EpsosDocument> xcaListResponse = clientConnectorService.queryDocuments(buildAssertionMapForPharmacist(patientId), "BE", patientId, List.of(classCode), null);
        assertThat(xcaListResponse).isNotNull().hasSize(2);

        final String uuid = xcaListResponse.get(1).getUuid();
        final String repositoryId = xcaListResponse.get(1).getRepositoryId();
        final var documentId = objectFactory.createDocumentId();
        documentId.setDocumentUniqueId(uuid);
        documentId.setRepositoryUniqueId(repositoryId);

        final EpsosDocument xcaRetrieve = clientConnectorService.retrieveDocument(buildAssertionMapForPharmacist(patientId), "BE", documentId, "1.3.6.1.4.1.48336", classCode, null);
        assertThat(xcaRetrieve).isNotNull();
        final byte[] base64Binary = xcaRetrieve.getBase64Binary();
        final String cda = new String(base64Binary, StandardCharsets.UTF_8);
        final Document epDocument = createDocumentFromString(cda);

        var edUid = generateIdentifierExtension();
        var edOid = configurationManager.getProperty("PORTAL_DISPENSATION_OID");

        final DispenseRequest dispenseRequest = new DispenseRequest();
        dispenseRequest.setCountryCode("BE");
        dispenseRequest.setSubstitution(true);
        dispenseRequest.setPrescriptionId("3000.1");
        final PackageSize packageSize = new PackageSize();
        packageSize.setQuantity("5");
        dispenseRequest.setPackageSize(packageSize);
        dispenseRequest.setNumberOfPackage(5);
        dispenseRequest.setProductName("Lantus SoloStar");

        final byte[] fileContent = CDAUtils.generateDispensationDocument(dispenseRequest, epDocument, edUid);

        final EpsosDocument dispensationDocument = buildDispensationDocument("My Testing Portal", edOid, edUid, fileContent);

        final SubmitDocumentResponse submitDocumentResponse = clientConnectorService.submitDocument(buildAssertionMapForPharmacist(patientId), "BE", dispensationDocument, xcpdResponse.get(0));
        assertThat(submitDocumentResponse).isNotNull();
        assertThat(submitDocumentResponse.getResponseStatus()).isEqualTo("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success");
    }

    @Test
    void testQueryPrescriptionsWithWrongProfile() throws MalformedURLException, MarshallingException, ClientConnectorException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createMedicalDoctorAssertion(keyStoreManager, "Gregory House", "gregory@ehdsi.eu");

        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("1-1234-W8");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.EP_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.EP_TITLE);

        assertThatExceptionOfType(ClientConnectorException.class)
                .isThrownBy(() -> clientConnectorService.queryDocuments(assertions, "BE", patientId, List.of(classCode), null))
                .satisfies(clientConnectorException -> {
                    assertThat(clientConnectorException.getOpenNCPErrorCode().get()).isEqualTo(OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS);
                    assertThat(!clientConnectorException.getNiErrorMessage().isPresent());
                })
                .withMessageContaining(OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS.getDescription());
    }

    private Document createDocumentFromString(String input) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(input));
        return builder.parse(is);
    }

    private PatientId createPatientId(String patientId) {

        final PatientId patientIdObject = objectFactory.createPatientId();
        patientIdObject.setRoot("1.3.6.1.4.1.48336");
        patientIdObject.setExtension(patientId);

        return patientIdObject;
    }

    private Map<AssertionType, Assertion> buildAssertionMapForPharmacist(final PatientId patientId) throws MalformedURLException, MarshallingException {
        final Assertion clinicalAssertion = AssertionUtils.createPharmacistAssertion(keyStoreManager, "Doctor House", "house@ehdsi.eu");
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, clinicalAssertion);
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);
        return assertions;
    }
    private Map<AssertionType, Assertion> buildAssertionMapForPhysician(final PatientId patientId) throws MalformedURLException, MarshallingException {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, clinicalAssertion);
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);
        return assertions;
    }

    private EpsosDocument buildDispensationDocument(String authorPerson, String dispenseRoot, String dispenseExtension, byte[] dispense) {

        GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.ED_CLASSCODE.getCode());
        classCode.setSchema(IheConstants.CLASSCODE_SCHEME);
        classCode.setValue(Constants.ED_TITLE);

        GenericDocumentCode formatCode = objectFactory.createGenericDocumentCode();
        formatCode.setSchema(IheConstants.DISPENSATION_FORMATCODE_CODINGSCHEMA);
        formatCode.setNodeRepresentation(IheConstants.DISPENSATION_FORMATCODE_NODEREPRESENTATION);
        formatCode.setValue(IheConstants.DISPENSATION_FORMATCODE_DISPLAYNAME);

        EpsosDocument document = objectFactory.createEpsosDocument();
        var author = objectFactory.createAuthor();
        author.setPerson(authorPerson);
        document.getAuthors().add(author);
        var timeZone = TimeZone.getTimeZone("UTC");
        document.setCreationDate(Calendar.getInstance(timeZone));
        document.setDescription(Constants.ED_TITLE);
        document.setTitle(Constants.ED_TITLE);
        document.setUuid(dispenseRoot + "^" + dispenseExtension);
        document.setSubmissionSetId("2.1.2.3.4.5.6.7.8.9");
        document.setClassCode(classCode);
        document.setFormatCode(formatCode);
        document.setBase64Binary(dispense);

        return document;
    }

    private String generateIdentifierExtension() {
        Random secureRandom = new SecureRandom();
        var bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        var extension = Base64.encodeBase64String(bytes);
        return extension.substring(0, 16);
    }
}
