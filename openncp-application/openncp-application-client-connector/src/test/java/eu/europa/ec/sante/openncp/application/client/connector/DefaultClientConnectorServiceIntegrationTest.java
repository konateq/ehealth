package eu.europa.ec.sante.openncp.application.client.connector;

import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionServiceImpl;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.STSClientException;
import eu.europa.ec.sante.openncp.application.client.connector.fhir.RestApiClientService;
import eu.europa.ec.sante.openncp.application.client.connector.fhir.security.JwtTokenGenerator;
import eu.europa.ec.sante.openncp.application.client.connector.testutils.AssertionTestUtil;
import eu.europa.ec.sante.openncp.application.client.connector.testutils.AssertionTestUtil.Concept;
import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerFactory;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.DatabasePropertiesKeyStoreManager;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mockito;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SetEnvironmentVariable(key = "EPSOS_PROPS_PATH", value = "")
@Disabled("Requires a complete environment")
class DefaultClientConnectorServiceIntegrationTest {

    private static DefaultClientConnectorService clientConnectorService;

    private static ConfigurationManager mockedConfigurationManager;
    private static JwtTokenGenerator jwtTokenGenerator;
    private static AssertionService assertionService;
    private static KeyStoreManager keyStoreManager;
    private static RestApiClientService restApiClientService;

    @BeforeAll
    static void setup() throws Exception {
        mockedConfigurationManager = Mockito.mock(ConfigurationManager.class);
        when(mockedConfigurationManager.getProperty("SC_KEYSTORE_PATH")).thenReturn("src/test/resources/gazelle-service-consumer-keystore.jks");
        when(mockedConfigurationManager.getProperty("SC_KEYSTORE_PASSWORD")).thenReturn("gazelle");
        when(mockedConfigurationManager.getProperty("SC_PRIVATEKEY_PASSWORD")).thenReturn("gazelle");
        when(mockedConfigurationManager.getProperty(Constant.TRUSTSTORE_PATH)).thenReturn("src/test/resources/eu-truststore.jks");
        when(mockedConfigurationManager.getProperty(Constant.TRUSTSTORE_PASSWORD)).thenReturn("changeit");

        when(mockedConfigurationManager.getProperty(Constant.NCP_SIG_KEYSTORE_PATH)).thenReturn("src/test/resources/gazelle-signature-keystore.jks");
        when(mockedConfigurationManager.getProperty("NCP_SIG_KEYSTORE_PATH")).thenReturn("src/test/resources/gazelle-signature-keystore.jks");
        when(mockedConfigurationManager.getProperty(Constant.NCP_SIG_KEYSTORE_PASSWORD)).thenReturn("gazelle");
        when(mockedConfigurationManager.getProperty("NCP_SIG_KEYSTORE_PASSWORD")).thenReturn("gazelle");
        when(mockedConfigurationManager.getProperty(Constant.NCP_SIG_PRIVATEKEY_ALIAS)).thenReturn("gazelle.ncp-signature.openncp.dg-sante.eu");
        when(mockedConfigurationManager.getProperty("NCP_SIG_PRIVATEKEY_ALIAS")).thenReturn("gazelle.ncp-signature.openncp.dg-sante.eu");
        when(mockedConfigurationManager.getProperty(Constant.NCP_SIG_PRIVATEKEY_PASSWORD)).thenReturn("gazelle");
        when(mockedConfigurationManager.getProperty("NCP_SIG_PRIVATEKEY_PASSWORD")).thenReturn("gazelle");

        when(mockedConfigurationManager.getProperty("secman.sts.url")).thenReturn("https://localhost:2443/TRC-STS/STSServiceService");
        when(mockedConfigurationManager.getProperty("secman.sts.checkHostname")).thenReturn("false");
        when(mockedConfigurationManager.getProperty("PORTAL_CLIENT_CONNECTOR_URL")).thenReturn("https://localhost:6443/openncp-client-connector/services/ClientService");


        setFinalStatic(ConfigurationManagerFactory.class.getDeclaredField("configurationManager"), mockedConfigurationManager);

        jwtTokenGenerator = new JwtTokenGenerator(mockedConfigurationManager);
        clientConnectorService = new DefaultClientConnectorService(mockedConfigurationManager, restApiClientService, jwtTokenGenerator);
        keyStoreManager = new DatabasePropertiesKeyStoreManager(mockedConfigurationManager);
        assertionService = new AssertionServiceImpl(keyStoreManager, mockedConfigurationManager);


    }

    static void setFinalStatic(final Field field, final Object newValue) throws Exception {
        field.setAccessible(true);
        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    @Test
    void sayHello() {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu"));
        final String response = clientConnectorService.sayHello(assertions, "Kim");
        assertThat(response).isEqualTo("Hello Kim");
    }

    @Test
    void queryPatient() throws ClientConnectorException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu"));

        final ObjectFactory objectFactory = new ObjectFactory();
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
        final Assertion clinicalAssertion = createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionTestUtil.createPatientConfirmationPlain(assertionService, new URL(mockedConfigurationManager.getProperty("secman.sts.url")), clinicalAssertion, patientId, "TREATMENT");
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
        final Assertion clinicalAssertion = createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        final var documentId = objectFactory.createDocumentId();
        documentId.setDocumentUniqueId("1.2.752.129.2.1.2.1^PS_W8_EU.1");
        documentId.setRepositoryUniqueId("1.3.6.1.4.1.48336");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionTestUtil.createPatientConfirmationPlain(assertionService, new URL(mockedConfigurationManager.getProperty("secman.sts.url")), clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.PS_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.PS_TITLE);

        final EpsosDocument document = clientConnectorService.retrieveDocument(assertions, "BE", documentId, "1.3.6.1.4.1.48336", classCode, null);
        assertThat(document).isNotNull();
    }


    private Assertion createClinicalAssertion(final KeyStoreManager keyStoreManager, final String username, final String fullName,
                                              final String email) {
        final List<String> permissions = new ArrayList<>();
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-003");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-004");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-005");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-006");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-010");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-016");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PPD-032");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PPD-033");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PPD-046");

        final Concept conceptRole = new Concept();
        conceptRole.setCode("221");
        conceptRole.setCodeSystemId("2.16.840.1.113883.2.9.6.2.7");
        conceptRole.setCodeSystemName("ISCO");
        conceptRole.setDisplayName("Medical Doctors");

        return AssertionTestUtil.createHCPAssertion(keyStoreManager, fullName, email, "BE", "BElgium", "homecommid", conceptRole,
                "eHealth OpenNCP EU Portal", "urn:hl7ii:1.2.3.4:ABCD", "Resident Physician", "TREATMENT",
                "eHDSI EU Testing MedCare Center", permissions, null);
    }
}
