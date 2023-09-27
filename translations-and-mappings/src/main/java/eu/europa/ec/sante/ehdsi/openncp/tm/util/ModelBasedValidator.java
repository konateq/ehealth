package eu.europa.ec.sante.ehdsi.openncp.tm.util;

import eu.europa.ec.sante.ehdsi.openncp.tm.config.TMConfiguration;
import net.ihe.gazelle.epsos.utils.ProjectDependencies;
import net.ihe.gazelle.epsos.validator.GazelleValidatorCore;
import net.ihe.gazelle.epsos.validator.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;

@Component
public class ModelBasedValidator implements TMConstants {

    private static final Logger log = LoggerFactory.getLogger(ModelBasedValidator.class);

    private static final String SCHEMA_RESULT = "/detailedResult/DocumentValidXSD/Result";
    private static final String MDA_RESULT = "/detailedResult/MDAValidation/Result";
    private static final String FINAL_RESULT = "/detailedResult/ValidationResultsOverview/ValidationTestResult";
    private static final String ERRORS = "//Error";
    private static final String PASSED = "PASSED";

    private static ModelBasedValidator instance;

    @Autowired
    private TMConfiguration config;

    private HashMap<String, String> friendlyTypes;
    private HashMap<String, String> pivotTypes;

    public ModelValidatorResult validate(String document, String docType, boolean friendly) {

        log.info("MDA validator start");
        String validator;
        ModelValidatorResult result = new ModelValidatorResult();

        // determine type of validator to be used
        if (friendly) {
            validator = friendlyTypes.get(docType);
        } else {
            validator = pivotTypes.get(docType);
        }

        // if no validator is found set validationError to true a log it
        if (validator == null) {
            result.setValidationError(true);
            log.error("No validator found for document type '{}', friendly: '{}'", docType, friendly);
        } else {
            log.info("Using '{}' validator, friendly: '{}'", validator, friendly);

            result.setSchemaValid(false);
            result.setModelValid(false);
            result.setResultValid(false);

            try {
                String mdaResult = GazelleValidatorCore.validateDocument(document, validator);
                Document mdaResultDoc = XmlUtil.stringToDom(mdaResult);

                // log validation errors
                List<Node> errors = XmlUtil.getNodeList(mdaResultDoc, ERRORS);
                if (log.isWarnEnabled()) {
                    log.warn("MDA validation errors: \n '{}'", XmlUtil.nodeListToString(errors));
                }

                // evaluate XSD validation status
                Node resultNode = XmlUtil.getNode(mdaResultDoc, SCHEMA_RESULT);
                if (resultNode != null && PASSED.equalsIgnoreCase(resultNode.getTextContent())) {
                    result.setSchemaValid(true);
                }
                if (resultNode != null) {
                    log.info("Schema validation status: '{}'", resultNode.getTextContent());
                }

                // evaluate MDA validation status
                resultNode = XmlUtil.getNode(mdaResultDoc, MDA_RESULT);
                if (resultNode != null && PASSED.equalsIgnoreCase(resultNode.getTextContent())) {
                    result.setModelValid(true);
                }
                if (resultNode != null) {
                    log.info("MDA validation status: '{}'", resultNode.getTextContent());
                }

                // evaluate total validation status
                resultNode = XmlUtil.getNode(mdaResultDoc, FINAL_RESULT);
                if (resultNode != null && PASSED.equalsIgnoreCase(resultNode.getTextContent())) {
                    result.setResultValid(true);
                }
                if (resultNode != null) {
                    log.info("Final validation status: '{}'", resultNode.getTextContent());
                }

                result.setValidationError(false);
            } catch (Exception e) {
                log.error("MDA validation error: '{}'", e.getMessage(), e);
                result.setValidationError(true);
            }
        }
        return result;
    }

    public void setConfig(TMConfiguration config) {
        this.config = config;
    }

    public void afterPropertiesSet() {

        ProjectDependencies.CDA_XSD = config.getMdaCdaXsdPath();
        ProjectDependencies.CDA_EPSOS_XSD = config.getMdaCdaEpsosXsdPath();
        ProjectDependencies.CDA_XSL_TRANSFORMER = config.getMdaCdaXslTransformerPath();
        ProjectDependencies.VALUE_SET_REPOSITORY = config.getMdaValuesetRepositoryPath();

        friendlyTypes = new HashMap<>();
        friendlyTypes.put(PATIENT_SUMMARY3, Validators.EPSOS_PS_FRIENDLY.getValue());
        friendlyTypes.put(PATIENT_SUMMARY1, Validators.EPSOS_PS_FRIENDLY.getValue());
        friendlyTypes.put(EDISPENSATION3, Validators.EPSOS_ED_FRIENDLY.getValue());
        friendlyTypes.put(EPRESCRIPTION3, Validators.EPSOS_EP_FRIENDLY.getValue());
        friendlyTypes.put(EPRESCRIPTION1, Validators.EPSOS_EP_FRIENDLY.getValue());
        friendlyTypes.put(HCER3, Validators.EPSOS_HCER.getValue());
        friendlyTypes.put(HCER1, Validators.EPSOS_HCER.getValue());
        friendlyTypes.put(MRO3, Validators.EPSOS_MRO.getValue());
        friendlyTypes.put(MRO1, Validators.EPSOS_MRO.getValue());

        pivotTypes = new HashMap<>();
        pivotTypes.put(PATIENT_SUMMARY3, Validators.EPSOS_PS_PIVOT.getValue());
        pivotTypes.put(PATIENT_SUMMARY1, Validators.EPSOS_PS_PIVOT.getValue());
        pivotTypes.put(EDISPENSATION3, Validators.EPSOS_ED_PIVOT.getValue());
        pivotTypes.put(EPRESCRIPTION3, Validators.EPSOS_EP_PIVOT.getValue());
        pivotTypes.put(EPRESCRIPTION1, Validators.EPSOS_EP_PIVOT.getValue());
        pivotTypes.put(HCER3, Validators.EPSOS_HCER.getValue());
        pivotTypes.put(HCER1, Validators.EPSOS_HCER.getValue());
        pivotTypes.put(MRO3, Validators.EPSOS_MRO.getValue());
        pivotTypes.put(MRO1, Validators.EPSOS_MRO.getValue());
    }
}
