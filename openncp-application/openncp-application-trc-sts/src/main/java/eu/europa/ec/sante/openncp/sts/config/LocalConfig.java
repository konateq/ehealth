package eu.europa.ec.sante.openncp.sts.config;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.List;

/**
 * This configuration class is only used with the "local" profile.
 * It will apply the given datasource properties (defined by {@link DataSourcePropertyHolder}) to the JNDI
 * data resources, used in {@link eu.europa.ec.sante.openncp.core.common.database.DatabaseConfiguration} to simulate Tomcat's server.xml configuration style.
 * <p>
 * This allows us to run the client from within IntelliJ to greatly facilitate local debugging.
 */
@Configuration
@EnableConfigurationProperties
@Profile("local")
public class LocalConfig {
    @Bean(destroyMethod="")
    @ConfigurationProperties(prefix = "spring.datasource.properties")
    public DataSourcePropertyHolder propertiesDataSourcePropertyHolder() {
        return new DataSourcePropertyHolder();
    }

    @Bean(destroyMethod="")
    @ConfigurationProperties(prefix = "spring.datasource.tsam")
    public DataSourcePropertyHolder tsamDataSourcePropertyHolder() {
        return new DataSourcePropertyHolder();
    }

    @Bean(destroyMethod="")
    public TomcatServletWebServerFactory tomcatFactory(final List<DataSourcePropertyHolder> dataSourcePropertyHolders) {
        final TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected TomcatWebServer getTomcatWebServer(final Tomcat tomcat) {
                tomcat.enableNaming();
                return super.getTomcatWebServer(tomcat);
            }

            @Override
            protected void postProcessContext(final Context context) {
                dataSourcePropertyHolders.stream()
                        .map(DataSourcePropertyHolder::asContextResource)
                        .forEach(contextResource -> context.getNamingResources().addResource(contextResource));
            }
        };
        return tomcat;
    }

    public static class DataSourcePropertyHolder {
        private String url;
        private String driverClassName;
        private String username;
        private String password;

        private String jndiName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
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

        public String getJndiName() {
            return jndiName;
        }

        public void setJndiName(String jndiName) {
            this.jndiName = jndiName;
        }

        public ContextResource asContextResource() {
            final ContextResource resource = new ContextResource();
            resource.setName(this.getJndiName());
            resource.setType(DataSource.class.getName());
            resource.setProperty("driverClassName", this.getDriverClassName());
            resource.setProperty("url", this.getUrl());
            resource.setProperty("username", this.getUsername());
            resource.setProperty("password", this.getPassword());
            return resource;
        }
    }
}
