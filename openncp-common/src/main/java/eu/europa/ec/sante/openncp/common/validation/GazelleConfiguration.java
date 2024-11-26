package eu.europa.ec.sante.openncp.common.validation;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GazelleConfiguration {

    private static final String NATIONAL_CONFIG = System.getenv("EPSOS_PROPS_PATH") + "validation"
            + File.separatorChar + "gazelle.ehdsi.properties";
    private static final Logger logger = LoggerFactory.getLogger(GazelleConfiguration.class);
    private Properties properties;

    private static GazelleConfiguration gazelleConfiguration;

    static {
        System.setProperty("javax.net.ssl.trustStore", Constants.TRUSTSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", Constants.TRUSTSTORE_PASSWORD);
    }

    private GazelleConfiguration() throws ConfigurationException {

        logger.info("eHDSI Gazelle Initialization!");
        File file = new File(NATIONAL_CONFIG);
        properties = new Properties();
        if (file.exists()) {
            logger.info("Loading National Gazelle Configuration");
            try {
                properties.load(new FileInputStream(file));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load properties file: " + NATIONAL_CONFIG, e);
            }
        } else {
            logger.info("Loading Default Gazelle Configuration");
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("gazelle.ehdsi.properties" )) {
                if (input == null) {
                    throw new IOException("Resource file not found: " + "gazelle.ehdsi.properties");
                }
                properties.load(input);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load local properties file: " + "gazelle.ehdsi.properties", e);
            }
        }
    }

    public static GazelleConfiguration getInstance() {

        if (gazelleConfiguration == null) {
            try {
                gazelleConfiguration = new GazelleConfiguration();
            } catch (ConfigurationException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
        return gazelleConfiguration;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
