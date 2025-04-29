package eu.europa.ec.sante.openncp.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = "eu.europa.ec.sante.openncp.common.property",
        entityManagerFactoryRef = "propertiesEntityManagerFactory",
        transactionManagerRef = "propertiesTransactionManager")
public class PropertyDatabaseConfiguration {
    @Configuration
    @Profile("!local")
    public static class PropertiesDatabaseConfiguration {
        @Bean(destroyMethod="")
        @ConfigurationProperties(prefix = "spring.datasource.jndi.properties")
        public JndiPropertyHolder propertiesJndiPropertyHolder() {
            return new JndiPropertyHolder();
        }

        @Bean(destroyMethod="")
        @Primary
        public DataSource propertiesDataSource() {
            final JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
            final JndiPropertyHolder jndiPropertyHolder = propertiesJndiPropertyHolder();
            final DataSource dataSource = dataSourceLookup.getDataSource(jndiPropertyHolder.getJndiName());
            return dataSource;
        }
    }

    @Primary
    @Bean(destroyMethod="")
    public LocalContainerEntityManagerFactoryBean propertiesEntityManagerFactory(final EntityManagerFactoryBuilder builder, final DataSource propertiesDataSource) {
        return builder
                .dataSource(propertiesDataSource)
                .packages("eu.europa.ec.sante.openncp.common.property")
                .build();
    }

    @Primary
    @Bean(destroyMethod="")
    public PlatformTransactionManager propertiesTransactionManager(@Qualifier("propertiesEntityManagerFactory") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
