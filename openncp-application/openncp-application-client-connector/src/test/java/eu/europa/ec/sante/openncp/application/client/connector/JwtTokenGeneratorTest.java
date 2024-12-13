package eu.europa.ec.sante.openncp.application.client.connector;

import eu.europa.ec.sante.openncp.application.client.connector.fhir.security.JwtTokenGenerator;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services.BaseIntegrationTest;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class JwtTokenGeneratorTest extends BaseIntegrationTest {
    private final Logger LOGGER = LoggerFactory.getLogger(JwtTokenGeneratorTest.class);

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Test
    void generateJwtToken() {
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final String jwtToken = jwtTokenGenerator.generate(Map.of(AssertionType.HCP, clinicalAssertion));
        LOGGER.info("The jwttoken: " + jwtToken);

    }
}
