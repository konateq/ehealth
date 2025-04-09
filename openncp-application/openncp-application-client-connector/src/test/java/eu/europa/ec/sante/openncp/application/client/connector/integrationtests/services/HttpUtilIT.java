package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpUtilIT extends BaseIntegrationTest {

    @Test
    public void testGetCommonNameFromServerCertificate() {
        assertThat(HttpUtil.getCommonNameFromServerCertificate("https://localhost:8443/openncp-ws-server/services/XCA_Service")).isEqualTo("be.ehealth.eu");
    }
}
