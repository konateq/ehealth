package eu.europa.ec.sante.openncp.core.common.tsam.config;

import eu.europa.ec.sante.openncp.common.JndiPropertyHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@EnableJpaRepositories(
            basePackages = "eu.europa.ec.sante.openncp.core.common.tsam",
            entityManagerFactoryRef = "tsamEntityManagerFactory",
            transactionManagerRef = "tsamTransactionManager"
    )
    @Configuration
    public class TsamDatabaseConfiguration {
        @Bean(destroyMethod="")
        @ConfigurationProperties(prefix = "spring.datasource.jndi.tsam")
        public JndiPropertyHolder tsamJndiPropertyHolder() {
            return new JndiPropertyHolder();
        }

        @Bean(destroyMethod="")
        public DataSource tsamDataSource() {
            final JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
            final DataSource dataSource = dataSourceLookup.getDataSource(tsamJndiPropertyHolder().getJndiName());
            return dataSource;
        }

        @Bean(destroyMethod="")
        public LocalContainerEntityManagerFactoryBean tsamEntityManagerFactory(EntityManagerFactoryBuilder builder) {
            return builder
                    .dataSource(tsamDataSource())
                    .packages("eu.europa.ec.sante.openncp.core.common.tsam")
                    .build();
        }

        @Bean(destroyMethod="")
        public PlatformTransactionManager tsamTransactionManager(@Qualifier("tsamEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
