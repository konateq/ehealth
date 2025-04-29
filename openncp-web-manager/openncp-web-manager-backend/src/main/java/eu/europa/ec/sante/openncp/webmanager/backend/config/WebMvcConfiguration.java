package eu.europa.ec.sante.openncp.webmanager.backend.config;

import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

@Configuration
@PropertySource("classpath:smpeditor.properties")
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(final CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*").allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    @Bean(destroyMethod="")
    @Profile("local")
    public TomcatServletWebServerFactory tomcatFactory(final Environment environment) {
        return new TomcatServletWebServerFactory() {
            @Override
            protected TomcatWebServer getTomcatWebServer(final org.apache.catalina.startup.Tomcat tomcat) {
                tomcat.enableNaming();
                return super.getTomcatWebServer(tomcat);
            }

            // Parameter for Datasource used in Embedded Tomcat
            @Override
            protected void postProcessContext(final Context context) {
                final ContextResource defaultResource = new ContextResource();
                defaultResource.setName("jdbc/ConfMgr");
                defaultResource.setType(DataSource.class.getName());
                defaultResource.setProperty("driverClassName", environment.getProperty("spring.datasource.properties.driver-class-name", "com.mysql.cj.jdbc.Driver"));
                defaultResource.setProperty("factory", "com.zaxxer.hikari.HikariJNDIFactory");
                defaultResource.setProperty("jdbcUrl", environment.getProperty("spring.datasource.properties.url", "jdbc:mysql://localhost:3306/ehealth_properties"));
                defaultResource.setProperty("username", environment.getProperty("spring.datasource.properties.username", "myUsername"));
                defaultResource.setProperty("password", environment.getProperty("spring.datasource.properties.password", "myPassword"));
                context.getNamingResources().addResource(defaultResource);

                final ContextResource atnaResource = new ContextResource();
                atnaResource.setName("jdbc/OPEN_ATNA");
                atnaResource.setType(DataSource.class.getName());
                atnaResource.setProperty("driverClassName", environment.getProperty("spring.datasource.atna.driver-class-name","com.mysql.cj.jdbc.Driver"));
                atnaResource.setProperty("factory", "com.zaxxer.hikari.HikariJNDIFactory");
                atnaResource.setProperty("jdbcUrl", environment.getProperty("spring.datasource.atna.url","jdbc:mysql://localhost:3306/ehealth_atna"));
                atnaResource.setProperty("username",  environment.getProperty("spring.datasource.atna.username","myUsername"));
                atnaResource.setProperty("password", environment.getProperty("spring.datasource.atna.password","myPassword"));
                context.getNamingResources().addResource(atnaResource);

                final ContextResource eadcResource = new ContextResource();
                eadcResource.setName("jdbc/EADC");
                eadcResource.setType(DataSource.class.getName());
                eadcResource.setProperty("driverClassName", environment.getProperty("spring.datasource.eadc.driver-class-name","com.mysql.cj.jdbc.Driver"));
                eadcResource.setProperty("factory", "com.zaxxer.hikari.HikariJNDIFactory");
                eadcResource.setProperty("jdbcUrl", environment.getProperty("spring.datasource.eadc.url","jdbc:mysql://localhost:3306/ehealth_eadc"));
                eadcResource.setProperty("username",  environment.getProperty("spring.datasource.eadc.username","myUsername"));
                eadcResource.setProperty("password", environment.getProperty("spring.datasource.eadc.password","myPassword"));
                context.getNamingResources().addResource(eadcResource);
            }
        };
    }
}
