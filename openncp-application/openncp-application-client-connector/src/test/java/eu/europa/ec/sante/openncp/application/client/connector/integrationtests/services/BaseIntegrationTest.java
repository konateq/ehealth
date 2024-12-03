package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.SpringConfiguration;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerImpl;
import eu.europa.ec.sante.openncp.common.property.PropertyService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(SpringExtension.class)
@SetEnvironmentVariable(key = "EPSOS_PROPS_PATH", value = "")
@ContextConfiguration(classes = {SpringConfiguration.class})
@ActiveProfiles({"local", "test"})
@TestPropertySource(locations = "classpath:application-local.properties")
@Configuration
public class BaseIntegrationTest {

    @Bean
    @Primary
    public ConfigurationManager configurationManager(final PropertyService propertyService) {
        final ConfigurationManager spy = spy(new ConfigurationManagerImpl(propertyService));

        doReturn("src/test/resources/gazelle-signature-keystore.jks").when(spy).getProperty(eq("NCP_SIG_KEYSTORE_PATH"));
        doReturn("gazelle").when(spy).getProperty(eq("NCP_SIG_KEYSTORE_PASSWORD"));
        doReturn("src/test/resources/gazelle-service-consumer-keystore.jks").when(spy).getProperty(eq("SC_KEYSTORE_PATH"));
        doReturn("gazelle").when(spy).getProperty(eq("SC_KEYSTORE_PASSWORD"));
        doReturn("src/test/resources/eu-truststore.jks").when(spy).getProperty(eq("TRUSTSTORE_PATH"));
        doReturn("changeit").when(spy).getProperty(eq("TRUSTSTORE_PASSWORD"));
        doReturn("https://localhost:6443/openncp-client-connector/services/ClientService").when(spy).getProperty(eq("PORTAL_CLIENT_CONNECTOR_URL"));
        doReturn("http://localhost:8091/openncp-client-connector/fhir/").when(spy).getProperty(eq("FHIR_REST_CLIENT_API"));
        doReturn("https://localhost:2443/TRC-STS/SecurityTokenService").when(spy).getProperty(eq("secman.sts.url"));
        doReturn("false").when(spy).getProperty(eq("secman.sts.checkHostname"));
        doReturn("false").when(spy).getProperty(eq("automated.validation"));
        return spy;
    }
}
