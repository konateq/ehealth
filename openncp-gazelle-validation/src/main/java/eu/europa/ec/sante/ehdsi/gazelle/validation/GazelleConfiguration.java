package eu.europa.ec.sante.ehdsi.gazelle.validation;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.srdc.epsos.util.Constants;

import java.io.File;

public class GazelleConfiguration {

    private static final String NATIONAL_CONFIG = System.getenv("EPSOS_PROPS_PATH") + "validation"
            + File.separatorChar + "gazelle.ehdsi.properties";
    private static final Logger logger = LoggerFactory.getLogger((GazelleConfiguration.class));
    private static GazelleConfiguration instance;

    static {
        System.setProperty("javax.net.ssl.trustStore", Constants.TRUSTSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", Constants.TRUSTSTORE_PASSWORD);
    }

    private Configuration configuration;

    private GazelleConfiguration() throws ConfigurationException {

        logger.info("eHDSI Gazelle Initialization!");
        File file = new File(NATIONAL_CONFIG);
        String gazelleConfig;
        if (file.exists()) {
            logger.info("Loading National Gazelle Configuration");
            gazelleConfig = NATIONAL_CONFIG;
        } else {
            logger.info("Loading Default Gazelle Configuration");
            gazelleConfig = "gazelle.ehdsi.properties";
        }
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.properties().setFileName(gazelleConfig));

        configuration = builder.getConfiguration();
    }

    public static GazelleConfiguration getInstance() {

        if (instance == null) {
            try {
                instance = new GazelleConfiguration();
            } catch (ConfigurationException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
        return instance;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfig(Configuration configuration) {
        this.configuration = configuration;
    }
}
