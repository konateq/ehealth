package eu.europa.ec.sante.openncp.application.client.connector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.sante.openncp.application.client.connector.testutils.AssertionTestUtil;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(SpringExtension.class)
@SetEnvironmentVariable(key = "EPSOS_PROPS_PATH", value = "")
@ContextConfiguration(classes = {SpringConfiguration.class})
@ActiveProfiles({"local", "test"})
@TestPropertySource(locations = "classpath:application-local.properties")
@Configuration
public class ITtest {
    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Bean
    @Primary
    public ConfigurationManager configurationManager(final PropertyService propertyService) {
        final ConfigurationManager spy = spy(new ConfigurationManagerImpl(propertyService));

        doReturn("src/test/resources/eu-truststore.jks").when(spy).getProperty(eq("TRUSTSTORE_PATH"));
        doReturn("src/test/resources/gazelle-service-consumer-keystore.jks").when(spy).getProperty(eq("SC_KEYSTORE_PATH"));
        doReturn("gazelle").when(spy).getProperty(eq("SC_KEYSTORE_PASSWORD"));
        doReturn("src/test/resources/eu-truststore.jks").when(spy).getProperty(eq("TRUSTSTORE_PATH"));
        doReturn("changeit").when(spy).getProperty(eq("TRUSTSTORE_PASSWORD"));
        doReturn("https://localhost:6443/openncp-client-connector/services/ClientService").when(spy).getProperty(eq("PORTAL_CLIENT_CONNECTOR_URL"));

        return spy;
    }

    @Test
    public void test() {
        final String helloResponse = clientConnectorService.sayHello(new HashMap<>(), "Kim");
        assertThat(helloResponse).isEqualTo("Hello Kim");
    }

    @Test
    public void postHospitalDischargeReport() throws IOException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");
        assertions.put(AssertionType.HCP, clinicalAssertion);

        Map<String, Object> payload = jsonFileToMap("/hdr/documentReference.json");

        final ResponseEntity<String> responseEntity = clientConnectorService.postDocumentReferenceFhir(assertions, "BE", payload);
        assertThat(responseEntity).isNotNull();
    }

    private Map<String, Object> jsonFileToMap(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final InputStream is = getClass().getResourceAsStream(path);

        return mapper.readValue(is, new TypeReference<>() {});
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

        final AssertionTestUtil.Concept conceptRole = new AssertionTestUtil.Concept();
        conceptRole.setCode("221");
        conceptRole.setCodeSystemId("2.16.840.1.113883.2.9.6.2.7");
        conceptRole.setCodeSystemName("ISCO");
        conceptRole.setDisplayName("Medical Doctors");

        return AssertionTestUtil.createHCPAssertion(keyStoreManager, fullName, email, "BE", "Belgium", "homecommid", conceptRole,
                "eHealth OpenNCP EU Portal", "urn:hl7ii:1.2.3.4:ABCD", "Resident Physician", "TREATMENT",
                "eHDSI EU Testing MedCare Center", permissions, null);
    }
}
