package eu.europa.ec.sante.openncp.tools.tsam.sync.config;

import eu.europa.ec.sante.openncp.tools.tsam.sync.domainehealthproperty.model.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableJpaRepositories(
        entityManagerFactoryRef = "secondEntityManager",
        transactionManagerRef = "secondPlatformTransactionManager",
        basePackages = {"eu.europa.ec.sante.openncp.tools.tsam.sync.domainehealthproperty.repository"}
)
@EntityScan(basePackageClasses = {Property.class})
@ConfigurationProperties(prefix = "tsam-sync.datasource-ehealth-properties")
public class DataSourceEHealthProperties {

    private String host;

    private String port;

    private String username;

    private String password;

    private String database;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @Autowired
    private Environment env;

    @Bean(destroyMethod="")
    @ConfigurationProperties(prefix = "spring.datasource.datasource2")
    public DataSource secondDataSource(){
        return DataSourceBuilder.create().build();
    }

    @Bean(name="secondEntityManager", destroyMethod="")
    public LocalContainerEntityManagerFactoryBean secondEntityManager(){
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(secondDataSource());
        em.setPackagesToScan("eu.europa.ec.sante.openncp.tools.tsam.sync.domainehealthproperty.model");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2dll.auto", env.getProperty("hibernate.hbm2dll.auto"));
        properties.put("hibernate.dialect", env.getProperty("hibernate.dialect"));
        em.setJpaPropertyMap(properties);
        return em;
    }

    @Bean(name = "secondPlatformTransactionManager", destroyMethod="")
    public PlatformTransactionManager secondPlatformTransactionManager(){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(secondEntityManager().getObject());
        return transactionManager;
    }

}
