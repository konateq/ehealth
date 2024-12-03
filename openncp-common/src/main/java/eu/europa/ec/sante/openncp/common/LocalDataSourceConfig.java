package eu.europa.ec.sante.openncp.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@Profile("local")
@EnableJpaRepositories(
        basePackages = "eu.europa.ec.sante.openncp.common.property",
        entityManagerFactoryRef = "propertiesEntityManagerFactory",
        transactionManagerRef = "propertiesTransactionManager")
public class LocalDataSourceConfig {
    private final Environment environment;

    public LocalDataSourceConfig(final Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public DataSource propertiesDataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setDriverClassName(Objects.requireNonNull(environment.getProperty("spring.datasource.properties.driver-class-name")));
        dataSource.setUrl(Objects.requireNonNull(environment.getProperty("spring.datasource.properties.url")));
        dataSource.setUsername(Objects.requireNonNull(environment.getProperty("spring.datasource.properties.username")));
        dataSource.setPassword(Objects.requireNonNull(environment.getProperty("spring.datasource.properties.password")));
        return dataSource;
    }
}
