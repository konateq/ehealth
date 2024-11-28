package eu.europa.ec.sante.openncp.application.client.connector;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

@SetEnvironmentVariable(key = "EPSOS_PROPS_PATH", value = "")
public class ITtest extends BaseSystemTest {
    @Autowired
    private ClientConnectorService clientConnectorService;

    @Test
    public void test() {
        clientConnectorService.sayHello(new HashMap<>(), "tis");
    }
}
