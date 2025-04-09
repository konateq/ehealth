package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorException;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionService;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.ObjectFactory;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LabResultReportWorkflowIT extends BaseIntegrationTest {

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Autowired
    private ObjectMapper objectMapper;

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Test
    void queryDocumentReference() throws ClientConnectorException, MalformedURLException, MarshallingException {
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final Map<String, String> params = Map.of(
                "patient.identifier", patientId.getRoot() + "|" + patientId.getExtension(),
                "type", "http://loinc.org|11502-2");

        final ResponseEntity<String> responseEntity = clientConnectorService.queryDocumentReferenceFhir(assertions, "BE", params);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();

        final FhirContext fhirContext = FhirContext.forR4();
        final IParser parser = fhirContext.newJsonParser();
        final Bundle returnedBundle = parser.parseResource(Bundle.class, responseEntity.getBody());
        assertThat(returnedBundle).isNotNull();
    }

    @Test
    void queryBundle() throws MalformedURLException, MarshallingException, ClientConnectorException {
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin");
        patientId.setExtension("89121210976");

        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final ResponseEntity<String> responseEntity = clientConnectorService.queryBundleFhirById(assertions, "BE", "SimpleChemistryResultReport");
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();

        final FhirContext fhirContext = FhirContext.forR4();
        final IParser parser = fhirContext.newJsonParser();
        final Bundle returnedBundle = parser.parseResource(Bundle.class, responseEntity.getBody());
        assertThat(returnedBundle).isNotNull();
    }

}
