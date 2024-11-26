package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.SpringConfiguration;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.JsonUtils;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerImpl;
import eu.europa.ec.sante.openncp.common.property.PropertyService;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;


public class HospitalDischargeReportWorkflowIT extends BaseIntegrationTest{

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Test
    public void test() {
        final String helloResponse = clientConnectorService.sayHello(new HashMap<>(), "Kim");
        assertThat(helloResponse).isEqualTo("Hello Kim");
    }

    @Test
    public void postHospitalDischargeReport() throws IOException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        assertions.put(AssertionType.HCP, clinicalAssertion);

        final Map<String, Object> payload = JsonUtils.jsonFileToMap("/hdr/documentReference.json");

        final ResponseEntity<String> responseEntity = clientConnectorService.postDocumentReferenceFhir(assertions, "BE", payload);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
