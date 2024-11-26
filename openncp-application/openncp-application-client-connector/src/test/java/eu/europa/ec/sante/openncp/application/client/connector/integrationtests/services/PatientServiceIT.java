package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorException;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.ObjectFactory;
import eu.europa.ec.sante.openncp.core.client.api.PatientDemographics;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;


public class PatientServiceIT extends BaseIntegrationTest {

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;


    @Test
    void queryPatient() throws ClientConnectorException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        assertions.put(AssertionType.HCP, AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu"));

        final ObjectFactory objectFactory = new ObjectFactory();
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
                        tuple(patientId.getRoot(), patientId.getExtension())
                );
    }
}
