package eu.europa.ec.sante.openncp.webmanager.backend.module.atna;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.xml.bind.Marshaller;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "eu.europa.ec.sante.openncp.webmanager.backend.module.atna",
        entityManagerFactoryRef = "atnaEntityManagerFactory",
        transactionManagerRef = "atnaTransactionManager"
)
public class AtnaConfiguration {

    @Bean(name = "atnaDataSourceProperties", destroyMethod="")
    @ConfigurationProperties(prefix = "spring.datasource.jndi.atna")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "atnaDataSource", destroyMethod = "")
    public DataSource dataSource(@Qualifier("atnaDataSourceProperties") final DataSourceProperties dataSourceProperties) {
        final JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
        return dataSourceLookup.getDataSource(dataSourceProperties.getJndiName());
    }

    @Bean(name = "atnaEntityManagerFactory", destroyMethod="")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final EntityManagerFactoryBuilder builder,
                                                                       @Qualifier("atnaDataSource") final DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("eu.europa.ec.sante.openncp.webmanager.backend.module.atna")
                .build();
    }

    @Bean(name = "atnaTransactionManager", destroyMethod="")
    public PlatformTransactionManager transactionManager(@Qualifier("atnaEntityManagerFactory") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(destroyMethod="")
    public Jaxb2Marshaller marshaller() {
        final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("net.RFC3881.dicom");

        final Map<String, Object> properties = new HashMap<>();
        properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setMarshallerProperties(properties);
        return marshaller;
    }
}
