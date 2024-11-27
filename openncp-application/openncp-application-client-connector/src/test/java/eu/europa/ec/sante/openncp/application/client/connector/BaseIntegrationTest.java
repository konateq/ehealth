package eu.europa.ec.sante.openncp.application.client.connector;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
 
@SpringBootTest(classes = DummyApplication.class)
@Testcontainers
@Profile("local")
public abstract class BaseIntegrationTest {
 
    @Container
    private static final MySQLContainer<?> mysqlDBContainer = new MySQLContainer<>("mysql:8")
            .withDatabaseName("ehealth")
            .withUsername("testuser")
            .withPassword("testpass");
 
    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        mysqlDBContainer.start();
        registry.add("spring.datasource.url", mysqlDBContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlDBContainer::getUsername);
        registry.add("spring.datasource.password", mysqlDBContainer::getPassword);
        registry.add("spring.datasource.password", mysqlDBContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}