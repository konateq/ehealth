package eu.europa.ec.sante.openncp.webmanager.backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "eu.europa.ec.sante.openncp.webmanager.backend.persistence",
        entityManagerFactoryRef = "webmanagerEntityManagerFactory",
        transactionManagerRef = "webmanagerTransactionManager")
public class JpaConfiguration {
    @Bean(destroyMethod="")
    public LocalContainerEntityManagerFactoryBean webmanagerEntityManagerFactory(final EntityManagerFactoryBuilder builder, @Qualifier("propertiesDataSource") final DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("eu.europa.ec.sante.openncp.webmanager.backend.persistence.model")
                .build();
    }

    @Bean(destroyMethod="")
    public PlatformTransactionManager webmanagerTransactionManager(@Qualifier("webmanagerEntityManagerFactory") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
