package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorException;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.STSClientException;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.*;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPFaultException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    void retrieveDocumentWithNOK() throws ClientConnectorException, STSClientException, MarshallingException, MalformedURLException {
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

        final Assertion nextOfKinAssertion = AssertionUtils.createNOKAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT", "NOKID");
        assertions.put(AssertionType.NOK, nextOfKinAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.PS_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.PS_TITLE);

        final EpsosDocument document = clientConnectorService.retrieveDocument(assertions, "BE", documentId, "1.3.6.1.4.1.48336", classCode, null);
        assertThat(document).isNotNull();
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

    @Test
    void bugtrigger_EHEALTH_9827() throws MalformedURLException, MarshallingException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        final var documentId = objectFactory.createDocumentId();
        documentId.setDocumentUniqueId("not_existing_document_id");
        documentId.setRepositoryUniqueId("1.3.6.1.4.1.48336");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.PS_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.PS_TITLE);

        assertThatExceptionOfType(SOAPFaultException.class)
                .isThrownBy(() -> clientConnectorService.retrieveDocument(assertions, "BE", documentId, "1.3.6.1.4.1.48336", classCode, null))
                .extracting(SOAPFaultException::getFault)
                .satisfies(fault -> {
                    assertThat(fault.getFaultCode()).contains("Receiver");
                    assertThat(fault.getFaultString()).contains(OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING.getDescription());

                    final List<QName> subCodes = StreamSupport.stream(
                            ((Iterable<QName>) fault::getFaultSubcodes).spliterator(), false
                    ).collect(Collectors.toList());
                    assertThat(subCodes)
                            .hasSize(1)
                            .extracting(QName::getLocalPart)
                            .containsExactly(OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING.getCode());
                });
    }

    @Test
    void bugtrigger_EHEALTH_13227_missing_fault_code() throws MalformedURLException, MarshallingException {

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("unknown-patient");

        final PatientDemographics patientDemographics = objectFactory.createPatientDemographics();
        patientDemographics.getPatientId().add(patientId);

        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, clinicalAssertion);

        assertThatExceptionOfType(SOAPFaultException.class)
                .isThrownBy(() -> clientConnectorService.queryPatient(assertions, "BE", patientDemographics))
                .extracting(SOAPFaultException::getFault)
                .satisfies(fault -> {
                    assertThat(fault.getFaultCode()).contains("Receiver");
                    assertThat(fault.getFaultString()).contains("NoPatientIdDiscoveredException");

                    final List<QName> subCodes = StreamSupport.stream(
                            ((Iterable<QName>) fault::getFaultSubcodes).spliterator(), false
                    ).collect(Collectors.toList());
                    assertThat(subCodes)
                            .hasSize(1)
                            .extracting(QName::getLocalPart)
                            .containsExactly("ERROR_PI_NO_MATCH");
                });
    }
}
