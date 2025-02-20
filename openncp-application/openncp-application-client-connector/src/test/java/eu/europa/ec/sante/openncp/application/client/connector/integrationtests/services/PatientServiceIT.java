package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorException;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.ObjectFactory;
import eu.europa.ec.sante.openncp.core.client.api.PatientDemographics;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;


public class PatientServiceIT extends BaseIntegrationTest {

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Autowired
    private ConfigurationManager configurationManager;

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Test
    void queryPatient() throws ClientConnectorException {
        doReturn("src/test/resources/expired_keystore.jks").when(configurationManager).getProperty(eq("SC_KEYSTORE_PATH"));


        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu"));

        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        final PatientDemographics patientDemographics = objectFactory.createPatientDemographics();
        patientDemographics.getPatientId().add(patientId);

        final List<PatientDemographics> response = clientConnectorService.queryPatient(assertions, "BE", patientDemographics);
        assertThat(response)
                .isNotNull()
                .hasSize(1)
                .flatExtracting(PatientDemographics::getPatientId)
                .extracting(PatientId::getRoot, PatientId::getExtension)
                .containsExactly(
                        tuple(patientId.getRoot() + ".1000", patientId.getExtension())
                );
    }

    @Test
    void queryPatientFhir() throws ClientConnectorException {
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        assertions.put(AssertionType.HCP, clinicalAssertion);

        final Map<String, String> params = Map.of("identifier", patientId.getRoot() + "|" + patientId.getExtension());
        final ResponseEntity<String> responseEntity = clientConnectorService.queryPatientFhir(assertions, "BE", params);

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();

        final FhirContext fhirContext = FhirContext.forR4();
        final IParser parser = fhirContext.newJsonParser();
        final Bundle returnedBundle = parser.parseResource(Bundle.class, responseEntity.getBody());
        assertThat(returnedBundle).isNotNull();
    }


    @Test
    void bugtrigger_EHEALTH_12803_test_certificate_expired_error() throws ClientConnectorException {
        doReturn("src/test/resources/gazelle-service-consumer-keystore-expired-certificate.jks").when(configurationManager).getProperty(eq("SC_KEYSTORE_PATH"));

        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu"));

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("2-1234-W8");

        final PatientDemographics patientDemographics = objectFactory.createPatientDemographics();
        patientDemographics.getPatientId().add(patientId);

        try {
            clientConnectorService.queryPatient(assertions, "BE", patientDemographics);
        } catch (final Throwable e) {
            final Throwable expectedException = containsCertificateException(e);
            assertThat(expectedException).isNotNull();
            assertThat(expectedException.getMessage()).startsWith("[CERTIFICATE EXPIRED]");
        }

    }

    public Throwable containsCertificateException(Throwable e) {
        while (e != null) {
            if (e instanceof CertificateException) {
                return e; // Found CertificateException in the stack trace
            }
            e = e.getCause(); // Move up the cause chain
        }
        return null;
    }
}
