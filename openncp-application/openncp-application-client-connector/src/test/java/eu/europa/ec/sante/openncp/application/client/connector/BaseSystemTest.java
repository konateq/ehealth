package eu.europa.ec.sante.openncp.application.client.connector;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = DummyApplication.class)
@Testcontainers
@Profile("local")
public abstract class BaseSystemTest {
}
