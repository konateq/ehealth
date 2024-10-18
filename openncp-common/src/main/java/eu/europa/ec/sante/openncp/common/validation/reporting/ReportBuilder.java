package eu.europa.ec.sante.openncp.common.validation.reporting;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.util.FileSystemUtils;
import eu.europa.ec.sante.openncp.common.validation.GazelleConfiguration;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import net.ihe.gazelle.jaxb.result.sante.DetailedResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ReportBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportBuilder.class);
    private static final String REPORT_FILES_FOLDER = "validation";
    private static final boolean GAZELLE_HTML_REPORT;
    private static final boolean GAZELLE_FORMATTED_REPORT;

    static {
        GAZELLE_HTML_REPORT = Boolean.parseBoolean((String) GazelleConfiguration.getInstance()
                .getConfiguration().getProperty("GAZELLE_HTML_REPORT"));
        GAZELLE_FORMATTED_REPORT = Boolean.parseBoolean((String) GazelleConfiguration.getInstance()
                .getConfiguration().getProperty("GAZELLE_FORMATTED_REPORT"));
    }

    private ReportBuilder() {
    }

    public static boolean build(final String reportDate, final String model, final String objectType,
                                final String validationObject, final NcpSide ncpSide) {

        return build(reportDate, model, objectType, validationObject, null, null, ncpSide);
    }

    /**
     * This is the main operation in the report building process. Its main responsibility is to generate a report based
     * on a supplied model, validation object and detailed result.
     *
     * @param model            the model used in the Web Service invocation.
     * @param validationObject the validated object.
     * @param validationResult the validation result.
     * @return A boolean flag, indicating if the reporting process succeed or not.
     */
    public static boolean build(final String reportDate, final String model, final String objectType,
                                final String validationObject, final DetailedResult validationResult,
                                String validationResponse, final NcpSide ncpSide) {

        String sideFolder;
        if (ncpSide == null || StringUtils.isEmpty(ncpSide.getName())) {
            sideFolder = "NCP-X";
        } else {
            sideFolder = ncpSide.getName();
        }

        LOGGER.info("Build report for '{}' Model for '{}' side", objectType, sideFolder);
        String reportFileName;
        String reportDirName;
        String validationTestResult;
        String validationBody;
        File reportFile;

        if (StringUtils.isEmpty(model)) {
            LOGGER.error("The specified model is null or empty.");
            return false;
        }
        if (StringUtils.isEmpty(objectType)) {
            LOGGER.error("The specified objectType is null or empty.");
            return false;
        }
        if (StringUtils.isEmpty(validationObject)) {
            LOGGER.error("The specified validation object is null or empty.");
            return false;
        }
        if (validationResult == null) {
            LOGGER.warn("The specified validation result object is null. Assigning empty String to validation result.");
            validationTestResult = "";
        } else {
            LOGGER.info("Validation Result: '{}'", validationResult.getValidationResultsOverview() != null ?
                    validationResult.getValidationResultsOverview().getValidationTestResult() : "Validation Test Result Not available");
            validationTestResult = validationResult.getValidationResultsOverview().getValidationTestResult();
        }
        if (StringUtils.isEmpty(validationResponse)) {
            validationBody = "<!-- Validation report not available -->";
        } else {
            validationBody = validationResponse.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");
        }

        reportDirName = Constants.EPSOS_PROPS_PATH + REPORT_FILES_FOLDER + File.separator + sideFolder;
        reportFileName = reportDirName + File.separator + buildReportFileName(reportDate, model, objectType, validationTestResult);

        if (FileSystemUtils.createDirIfNotExists(reportDirName)) {

            LOGGER.info("Writing validation report in: '{}'", reportFileName);
            reportFile = new File(reportFileName);

            if (!reportFile.exists()) {
                try {
                    boolean fileCreated = reportFile.createNewFile();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("File has been created: '{}'", fileCreated);
                    }
                } catch (IOException ex) {
                    LOGGER.error("An I/O error has occurred while creating the report file, please check the stack trace for more information.", ex);
                    return false;
                }
            }

            LOGGER.info("Validation report written with success");
            if (GAZELLE_HTML_REPORT) {
                File repostHtmlFile = new File(StringUtils.replace(reportFileName, ".xml", ".html"));
                try (BufferedWriter htmlReport = new BufferedWriter(new FileWriter(repostHtmlFile.getAbsoluteFile()))) {
                    ReportTransformer reportTransformer = new ReportTransformer(validationBody, (Base64.isBase64(validationObject)
                            ? new String(Base64.decodeBase64(validationObject), StandardCharsets.UTF_8) : validationObject));
                    LOGGER.info("HTML:\n{}", reportTransformer.getHtmlReport());
                    htmlReport.write(reportTransformer.getHtmlReport());
                } catch (IOException e) {
                    LOGGER.error("An I/O error has occurred while writing the HTML report file, please check the stack trace for more information.", e);
                }
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile.getAbsoluteFile()))) {

                if (!OpenNCPValidation.isRemoteValidationEnable() && !GAZELLE_FORMATTED_REPORT) {
                    bw.write((Base64.isBase64(validationObject) ? new String(Base64.decodeBase64(validationObject), StandardCharsets.UTF_8) : validationObject));
                } else {
                    bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                    bw.write("\n");
                    bw.write("<validationReport>");
                    bw.write("\n");
                    bw.write("<validatedObject>");
                    bw.write("<![CDATA[\"" + (Base64.isBase64(validationObject)
                            ? new String(Base64.decodeBase64(validationObject), StandardCharsets.UTF_8) :
                            validationObject) + "\"]]>");
                    bw.write("</validatedObject>");
                    bw.write("\n");
                    bw.write("<validationResult>");
                    bw.write("<![CDATA[\"" + validationBody + "\"]]>");
                    bw.write("</validationResult>");
                    bw.write("\n");
                    bw.write("</validationReport>");
                }
                return true;

            } catch (IOException ex) {
                LOGGER.error("An I/O error has occurred while writing the report file, please check the stack trace for more information.", ex);
                return false;
            }
        }
        return false;
    }

    /**
     * This method will generate a file name for the report, based on a set of parameters.
     * Format mask: [timestamp]_[validator model]_[validation result].txt
     *
     * @param model                the model used in the validation.
     * @param validationTestResult the validation result object.
     * @return a report file name.
     */
    private static String buildReportFileName(final String reportDate, final String model, final String objectType,
                                              final String validationTestResult) {

        final String SEPARATOR = "_";
        final String FILE_EXTENSION = ".xml";
        String modelNormalized = model.replace(" ", "-");
        modelNormalized = modelNormalized.replace(":", "_");

        StringBuilder fileName = new StringBuilder();
        fileName.append(reportDate);

        if (StringUtils.isNotBlank(objectType)) {
            fileName.append(SEPARATOR);
            fileName.append(objectType);
        }

        if (StringUtils.isNotBlank(modelNormalized)) {
            fileName.append(SEPARATOR);
            fileName.append(modelNormalized.toUpperCase());
        }

        if (StringUtils.isNotBlank(validationTestResult)) {
            fileName.append(SEPARATOR);
            fileName.append(validationTestResult.toUpperCase());
        } else {
            fileName.append(SEPARATOR);
            fileName.append("NOT-TESTED");
        }

        fileName.append(FILE_EXTENSION);
        return fileName.toString();
    }

    /**
     * Util method generating a Reporting Date used as a prefix of report filename produced.
     *
     * @return ReportingDate in UTC formatted as String.
     */
    public static String formatDate() {

        //ISO 8601 format: 2017-11-25T10:59:53Z
        String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        return StringUtils.replace(date, ":", "-");
    }
}
