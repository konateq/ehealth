package eu.europa.ec.sante.openncp.core.common;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to provide a {@link DatatypeFactory} without the typical static code block.
 */
@Configuration
public class DatatypeFactoryConfig {

    @Bean
    public DatatypeFactory datatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Failed to initialize DatatypeFactory", e);
        }
    }
}
