package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorException;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.STSClientException;
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

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PatientSummaryWorkflowIT extends BaseIntegrationTest {

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Test
    void queryPatient() throws ClientConnectorException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu"));

        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        final PatientDemographics patientDemographics = objectFactory.createPatientDemographics();
        patientDemographics.getPatientId().add(patientId);

        final List<PatientDemographics> response = clientConnectorService.queryPatient(assertions, "BE", patientDemographics);
        assertThat(response).isNotNull();
    }

    @Test
    void queryDocuments() throws ClientConnectorException, STSClientException, MarshallingException, MalformedURLException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.PS_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.PS_TITLE);

        final List<EpsosDocument> documentList = clientConnectorService.queryDocuments(assertions, "BE", patientId, List.of(classCode), null);
        assertThat(documentList).isNotNull().hasSize(2);
    }

    @Test
    void retrieveDocument() throws ClientConnectorException, STSClientException, MarshallingException, MalformedURLException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        final var documentId = objectFactory.createDocumentId();
        documentId.setDocumentUniqueId("1.2.752.129.2.1.2.1^PS_W8_EU.1");
        documentId.setRepositoryUniqueId("1.3.6.1.4.1.48336");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.PS_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.PS_TITLE);

        final EpsosDocument document = clientConnectorService.retrieveDocument(assertions, "BE", documentId, "1.3.6.1.4.1.48336", classCode, null);
        assertThat(document).isNotNull();
    }

    @Test
    void retrieveUnknownDocument() throws ClientConnectorException, STSClientException, MarshallingException, MalformedURLException, TransformerException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        final var documentId = objectFactory.createDocumentId();
        documentId.setDocumentUniqueId("wrongId");
        documentId.setRepositoryUniqueId("1.3.6.1.4.1.48336");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.PS_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.PS_TITLE);

        try {
            EpsosDocument be = clientConnectorService.retrieveDocument(assertions, "BE", documentId, "1.3.6.1.4.1.48336", classCode, null);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (e instanceof SOAPFaultException){
                StringWriter sw = new StringWriter();
                TransformerFactory.newInstance().newTransformer().transform(
                        new DOMSource(((SOAPFaultException)e).getFault()), new StreamResult(sw));
                System.out.println(sw);
            }
            throw e;
        }

//        assertThatExceptionOfType(SOAPFaultException.class)
//                .isThrownBy(() -> )
//                .withMessageContaining("^[National Infrastructure Mock] No PS List Found");
    }

    @Test
    void bugtrigger_EHEALTH_10535() throws MalformedURLException, MarshallingException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final PatientId correctPatientId = objectFactory.createPatientId();
        correctPatientId.setRoot("1.3.6.1.4.1.48336");
        correctPatientId.setExtension("2-1234-W8");

        // This test will use a different patient id in the document than the one in the TRC assertion
        final PatientId differentPatientId = objectFactory.createPatientId();
        differentPatientId.setRoot("1.3.6.1.4.1.48336");
        differentPatientId.setExtension("different-patient-id");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, correctPatientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.PS_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.PS_TITLE);

        assertThatExceptionOfType(SOAPFaultException.class)
                .isThrownBy(() -> clientConnectorService.queryDocuments(assertions, "BE", differentPatientId, List.of(classCode), null))
                .withMessageContaining("The request is not containing a proper PS identifier.");
    }
}
