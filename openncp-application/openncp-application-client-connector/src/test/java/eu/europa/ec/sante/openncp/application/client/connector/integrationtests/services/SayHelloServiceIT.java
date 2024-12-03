package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;


public class SayHelloServiceIT extends BaseIntegrationTest {

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Test
    public void sayHello() {
        final String helloResponse = clientConnectorService.sayHello(new HashMap<>(), "Kim");
        assertThat(helloResponse).isEqualTo("Hello Kim");
    }
}
