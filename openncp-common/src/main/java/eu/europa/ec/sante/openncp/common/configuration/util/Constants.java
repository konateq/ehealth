package eu.europa.ec.sante.openncp.common.configuration.util;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.FileSystems;
import java.util.Optional;

@Component
public class Constants {

    public static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);

    public static final String UUID_PREFIX = "urn:uuid:";
    public static final String OID_PREFIX = "urn:oid:";
    public static final String HL7II_PREFIX = "urn:hl7ii";

    public static final String PatientIdentificationService = "PatientIdentificationService";
    public static final String PatientService = "PatientService";
    public static final String MroService = "MroService";
    public static final String ConsentService = "ConsentService";
    public static final String DispensationService = "DispensationService";
    public static final String OrderService = "OrderService";
    public static final String OrCDService = "OrCDService";

    public static final String PS_TITLE = "Patient Summary";
    public static final String MRO_TITLE = "MRO Summary";
    public static final String EP_TITLE = "ePrescription";
    public static final String ED_TITLE = "eDispensation";
    public static final String ORCD_HOSPITAL_DISCHARGE_REPORTS_TITLE = "OrCD Hospital Discharge Summary";
    public static final String ORCD_LABORATORY_RESULTS_TITLE = "OrCD Laboratory Report";
    public static final String ORCD_MEDICAL_IMAGING_REPORTS_TITLE = "OrCD Diagnostic Imaging Study";
    public static final String ORCD_MEDICAL_IMAGES_TITLE = "OrCD Medical Image";
    public static final String CONSENT_TITLE = "Privacy Policy Acknowledgement Document";
    public static final String UNKNOWN_TITLE = "Unknown Document Type";

    public static final String CONSENT_PUT_SUFFIX = ".2.4.1.1";
    public static final String CONSENT_DISCARD_SUFFIX = ".2.4.1.2";

    public static final String NOT_USED_FIELD = "Not Used";
    public static final String MIME_TYPE = "text/xml";
    public static String SERVER_IP;
    public static String HOME_COMM_ID;
    public static String COUNTRY_CODE;
    public static String COUNTRY_NAME;
    public static String COUNTRY_PRINCIPAL_SUBDIVISION;
    public static String LANGUAGE_CODE;
    public static final String HR_ID_PREFIX = "SPProvidedID";
    /**
     * Path to the folder containing the configuration files.
     */
    public static String EPSOS_PROPS_PATH;
    public static String TRUSTSTORE_PATH;
    public static String TRUSTSTORE_PASSWORD;
    public static String SP_KEYSTORE_PATH;
    public static String SP_KEYSTORE_PASSWORD;
    public static String SP_PRIVATEKEY_ALIAS;
    public static String SP_PRIVATEKEY_PASSWORD;
    public static String SC_KEYSTORE_PATH;
    public static String SC_KEYSTORE_PASSWORD;
    public static String SC_PRIVATEKEY_ALIAS;
    public static String SC_PRIVATEKEY_PASSWORD;
    public static String NCP_SIG_KEYSTORE_PATH;
    public static String NCP_SIG_KEYSTORE_PASSWORD;
    public static String NCP_SIG_PRIVATEKEY_ALIAS;
    public static String NCP_SIG_PRIVATEKEY_PASSWORD;

    public static String ABUSE_UNIQUE_PATIENT_REQUEST_THRESHOLD;
    public static String ABUSE_UNIQUE_PATIENT_REFERENCE_REQUEST_PERIOD;
    public static String ABUSE_UNIQUE_POC_REQUEST_THRESHOLD;
    public static String ABUSE_UNIQUE_POC_REFERENCE_REQUEST_PERIOD;
    public static String ABUSE_ALL_REQUEST_THRESHOLD;
    public static String ABUSE_ALL_REQUEST_REFERENCE_REQUEST_PERIOD;
    public static String ABUSE_SCHEDULER_TIME_INTERVAL;
    public static String ABUSE_SCHEDULER_ENABLE;

    public static String AVAILABLE_TRANSLATION_LANGUAGES;

    /**
     * Name of the System Variable containing the path to the folder containing the configuration files.
     */
    private static final String PROPS_ENV_VAR = "EPSOS_PROPS_PATH";
    private final ConfigurationManager configurationManager;

    public Constants(final ConfigurationManager configurationManager) {
        this.configurationManager = Validate.notNull(configurationManager);
    }


    @PostConstruct
    public void postConstruct() {
        String epsosPath = Optional.ofNullable(System.getenv(PROPS_ENV_VAR)).orElseGet(() -> System.getProperty(PROPS_ENV_VAR));
        if (StringUtils.isNotBlank(epsosPath)) {
            if (!epsosPath.endsWith(FileSystems.getDefault().getSeparator())) {
                epsosPath += FileSystems.getDefault().getSeparator();
            }
        } else {
            epsosPath = StringUtils.EMPTY;
            LOGGER.error("No {} set, this is needed for a fully working oNCP application, make sure to add it as an environment variable or a system property.", PROPS_ENV_VAR);
        }
        EPSOS_PROPS_PATH = epsosPath;

        LOGGER.info("OpenNCP Util Constants Initialization - EPSOS_PROPS_PATH: '{}'", EPSOS_PROPS_PATH);

        SERVER_IP = configurationManager.getProperty(Constant.SERVER_IP);

        HOME_COMM_ID = configurationManager.getProperty(Constant.HOME_COMM_ID);
        COUNTRY_CODE = configurationManager.getProperty(Constant.COUNTRY_CODE);
        COUNTRY_NAME = configurationManager.getProperty(Constant.COUNTRY_NAME);
        COUNTRY_PRINCIPAL_SUBDIVISION = configurationManager.getProperty(Constant.COUNTRY_PRINCIPAL_SUBDIVISION);
        LANGUAGE_CODE = configurationManager.getProperty(Constant.LANGUAGE_CODE);

        TRUSTSTORE_PATH = globalizePath(configurationManager.getProperty(Constant.TRUSTSTORE_PATH));
        TRUSTSTORE_PASSWORD = configurationManager.getProperty(Constant.TRUSTSTORE_PASSWORD);

        SP_KEYSTORE_PATH = globalizePath(configurationManager.getProperty(Constant.SP_KEYSTORE_PATH));
        SP_KEYSTORE_PASSWORD = configurationManager.getProperty(Constant.SP_KEYSTORE_PASSWORD);
        SP_PRIVATEKEY_ALIAS = configurationManager.getProperty(Constant.SP_PRIVATEKEY_ALIAS);
        SP_PRIVATEKEY_PASSWORD = configurationManager.getProperty(Constant.SP_PRIVATEKEY_PASSWORD);

        SC_KEYSTORE_PATH = globalizePath(configurationManager.getProperty(Constant.SC_KEYSTORE_PATH));
        SC_KEYSTORE_PASSWORD = configurationManager.getProperty(Constant.SC_KEYSTORE_PASSWORD);
        SC_PRIVATEKEY_ALIAS = configurationManager.getProperty(Constant.SC_PRIVATEKEY_ALIAS);
        SC_PRIVATEKEY_PASSWORD = configurationManager.getProperty(Constant.SC_PRIVATEKEY_PASSWORD);

        NCP_SIG_KEYSTORE_PATH = globalizePath(configurationManager.getProperty(Constant.NCP_SIG_KEYSTORE_PATH));
        NCP_SIG_KEYSTORE_PASSWORD = configurationManager.getProperty(Constant.NCP_SIG_KEYSTORE_PASSWORD);
        NCP_SIG_PRIVATEKEY_ALIAS = configurationManager.getProperty(Constant.NCP_SIG_PRIVATEKEY_ALIAS);
        NCP_SIG_PRIVATEKEY_PASSWORD = configurationManager.getProperty(Constant.NCP_SIG_PRIVATEKEY_PASSWORD);

        ABUSE_UNIQUE_PATIENT_REQUEST_THRESHOLD = configurationManager.getProperty(Constant.ABUSE_UNIQUE_PATIENT_REQUEST_THRESHOLD);
        ABUSE_UNIQUE_PATIENT_REFERENCE_REQUEST_PERIOD = configurationManager.getProperty(Constant.ABUSE_UNIQUE_PATIENT_REFERENCE_REQUEST_PERIOD);
        ABUSE_UNIQUE_POC_REQUEST_THRESHOLD = configurationManager.getProperty(Constant.ABUSE_UNIQUE_POC_REQUEST_THRESHOLD);
        ABUSE_UNIQUE_POC_REFERENCE_REQUEST_PERIOD = configurationManager.getProperty(Constant.ABUSE_UNIQUE_POC_REFERENCE_REQUEST_PERIOD);
        ABUSE_ALL_REQUEST_THRESHOLD = configurationManager.getProperty(Constant.ABUSE_ALL_REQUEST_THRESHOLD);
        ABUSE_ALL_REQUEST_REFERENCE_REQUEST_PERIOD = configurationManager.getProperty(Constant.ABUSE_ALL_REQUEST_REFERENCE_REQUEST_PERIOD);
        ABUSE_SCHEDULER_TIME_INTERVAL = configurationManager.getProperty(Constant.ABUSE_SCHEDULER_TIME_INTERVAL);
        ABUSE_SCHEDULER_ENABLE = configurationManager.getProperty(Constant.ABUSE_SCHEDULER_ENABLE);
        AVAILABLE_TRANSLATION_LANGUAGES = configurationManager.getProperty(Constant.AVAILABLE_TRANSLATION_LANGUAGES);
    }

    /**
     * check whether the input path is global, modify accordingly (global paths can come within linux)
     *
     * @param path
     * @return
     */
    private static String globalizePath(String path) {

        if (path == null) {
            return null;
        }
        if (!(path.startsWith("/") || path.matches("[A-Z]:.*"))) {
            path = EPSOS_PROPS_PATH + path;
        }

        return path;
    }
}
