package eu.europa.ec.sante.openncp.common.validation;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
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
    private final Properties properties;

    private static GazelleConfiguration gazelleConfiguration;

    static {
        System.setProperty("javax.net.ssl.trustStore", Constants.TRUSTSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", Constants.TRUSTSTORE_PASSWORD);
    }

    private GazelleConfiguration() {

        logger.info("eHDSI Gazelle Initialization!");
        final File file = new File(NATIONAL_CONFIG);
        properties = new Properties();
        if (file.exists()) {
            logger.info("Loading National Gazelle Configuration");
            try {
                properties.load(new FileInputStream(file));
            } catch (final IOException e) {
                throw new RuntimeException("Failed to load properties file: " + NATIONAL_CONFIG, e);
            }
        } else {
            logger.info("Loading Default Gazelle Configuration");
            try (final InputStream input = getClass().getClassLoader().getResourceAsStream("gazelle.ehdsi.properties")) {
                if (input == null) {
                    throw new IOException("Resource file not found: " + "gazelle.ehdsi.properties");
                }
                properties.load(input);
            } catch (final IOException e) {
                throw new RuntimeException("Failed to load local properties file: " + "gazelle.ehdsi.properties", e);
            }
        }
    }

    public static GazelleConfiguration getInstance() {

        if (gazelleConfiguration == null) {
            gazelleConfiguration = new GazelleConfiguration();
        }
        return gazelleConfiguration;
    }

    public String getProperty(final String key) {
        return properties.getProperty(key);
    }
}
