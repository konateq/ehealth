package eu.europa.ec.sante.openncp.webmanager.backend.module.eadc;

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
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = "eu.europa.ec.sante.openncp.webmanager.backend.module.eadc",
        entityManagerFactoryRef = "eadcEntityManagerFactory",
        transactionManagerRef = "eadcTransactionManager"
)
public class EadcConfiguration {

    @Bean(name = "eadcDataSourceProperties", destroyMethod="")
    @ConfigurationProperties(prefix = "spring.datasource.jndi.eadc")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "eadcDataSource", destroyMethod = "")
    public DataSource dataSource(@Qualifier("eadcDataSourceProperties") final DataSourceProperties dataSourceProperties) {
        final JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
        return dataSourceLookup.getDataSource(dataSourceProperties.getJndiName());
    }

    @Bean(name = "eadcEntityManagerFactory", destroyMethod="")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final EntityManagerFactoryBuilder builder,
                                                                       @Qualifier("eadcDataSource") final DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("eu.europa.ec.sante.openncp.webmanager.backend.module.eadc")
                .build();
    }

    @Bean(name = "eadcTransactionManager", destroyMethod="")
    public PlatformTransactionManager transactionManager(@Qualifier("eadcEntityManagerFactory") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
