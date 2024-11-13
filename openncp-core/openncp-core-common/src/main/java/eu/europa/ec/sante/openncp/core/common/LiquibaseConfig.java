package eu.europa.ec.sante.openncp.core.common;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, Environment env) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.xml");
        liquibase.setContexts(env.getProperty("liquibase.contexts", ""));
        liquibase.setLabels(env.getProperty("liquibase.labels", ""));
        liquibase.setShouldRun(env.getProperty("liquibase.enabled", Boolean.class, true));
        return liquibase;
    }
}

