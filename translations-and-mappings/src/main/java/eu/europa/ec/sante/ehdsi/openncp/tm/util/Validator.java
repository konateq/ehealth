package eu.europa.ec.sante.ehdsi.openncp.tm.util;

import eu.europa.ec.sante.ehdsi.openncp.tm.config.TMConfiguration;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

    /**
     * Provides access to validation methods for CDA Document.
     */
    @Service
    public class Validator implements TMConstants {

        private static final Logger LOGGER = LoggerFactory.getLogger(Validator.class);
        private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");

        @Autowired
        private ModelBasedValidator modelBasedValidator;

        @Autowired
        private TMConfiguration configuration;

        private Validator() {
        }

        /**
         * Validation against schema
         *
         * @param document - Validated document
         */
        public boolean validateToSchema(Document document) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("XSD Validation of CDA based on: '{}'", configuration.getSchemaFilePath());
            }
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Source schemaFile = new StreamSource(new File(configuration.getSchemaFilePath()));
                Schema schema = factory.newSchema(schemaFile);
                javax.xml.validation.Validator validator = schema.newValidator();
                if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
                    LOGGER_CLINICAL.debug("[Transformation Manager] Validation of CDA document:\n'{}", XMLUtil.prettyPrint(document));
                }
                validator.validate(new DOMSource(document));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[Transformation Manager] CDA document is valid according XSD definition");
                }
                return true;
            } catch (Exception e) {
                LOGGER.error("[Transformation Manager] Schema validation error, Invalid Document!", e);
                return false;
            }
        }

        private static boolean isScannedDocument(String docType) {

            return docType.equals(PATIENT_SUMMARY1) || docType.equals(EPRESCRIPTION1)
                    || docType.equals(HCER1) || docType.equals(MRO1);
        }

        /**
         * Validation using Schematron
         *
         * @param document        - Validated document
         * @param cdaDocumentType - type of CDA document (PatientSummary, ePrescription, eDispensation)
         * @param friendly        - if true validate against friendly scheme, else against pivot
         */
        public SchematronResult validateSchematron(Document document, String cdaDocumentType, boolean friendly) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("--> method SchematronResult validateSchematron('{}', '{})", cdaDocumentType, friendly);
            }
            SchematronResult result;
            String schemaPath;
            SchematronValidator schValidator = SchematronValidator.getInstance();

            // Fix docType for schematron validation.
            // Schematron has special validators for L1 documents, ignoring actual doc type.
            if (isScannedDocument(cdaDocumentType)) {
                if (friendly) {
                    cdaDocumentType = SCANNED1;
                } else {
                    cdaDocumentType = SCANNED3;
                }
            }

            if (friendly) {
                schemaPath = schValidator.getFriendlyType().get(cdaDocumentType);
            } else {
                schemaPath = schValidator.getPivotType().get(cdaDocumentType);
            }

            if (schemaPath == null) {
                // if no schematron is found return empty false result
                result = new SchematronResult();

                result.setValid(false);
                NodeList emptyList = document.createDocumentFragment().getChildNodes();
                result.setErrors(emptyList);

                return result;
            }
            result = SchematronValidator.validate(new File(StringUtils.trim(schemaPath)), document);
            return result;
        }

        /**
         * Validates according Model Based.
         *
         * @param document - CDA document as String.
         * @param docType  - CDA document type.
         * @param friendly - true|false if the document is a friendly one.
         * @return ModelValidatorResult as a report of the validation executed by the system.
         * @deprecated - Should not be used anymore as the rules embedded are not aligned with eHDSI CDA IG.
         */
        @Deprecated
        public ModelValidatorResult validateMDA(String document, String docType, boolean friendly) {
            ModelBasedValidator modelBasedValidator = new ModelBasedValidator();
            return modelBasedValidator.validate(document, docType, friendly);
        }
}
