package eu.europa.ec.sante.openncp.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileSystemUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemUtils.class);

    /**
     * This method will check if a directory exists, if not, it will create it.
     *
     * @param path the complete path for the directory.
     * @return a boolean flag stating the success of the operation.
     */
    public static boolean createDirIfNotExists(final String path) {

        final File dir = new File(path);

        if (!dir.exists()) {
            LOGGER.info("Creating directory in: '[{}]'", path);
            if (!dir.mkdirs()) {
                LOGGER.error("An error has occurred during the creation of validation report directory.");
                return false;
            } else {
                return true;
            }
        }
        return true;
    }
}
