package eu.europa.ec.sante.openncp.core.common.ihe.transformation.service;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.config.TMConfiguration;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMStatus;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.exception.TMException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.*;
import eu.europa.ec.sante.openncp.core.common.ihe.tsam.util.CodedElement;
import eu.europa.ec.sante.openncp.core.common.tsam.CodeConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.RetrievedConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.TSAMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.error.TMError;
import eu.europa.ec.sante.openncp.core.common.tsam.error.TMErrorCtx;
import eu.europa.ec.sante.openncp.core.common.tsam.service.TerminologyService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CDATransformationServiceImpl implements CDATransformationService, TMConstants {

    private static final DatatypeFactory DATATYPE_FACTORY;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (final DatatypeConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    private HashMap<String, String> level1Type;
    private HashMap<String, String> level3Type;

    private final TMConfiguration config;

    private final CodedElementList codedElementList;
    private final TerminologyService terminologyService;
    private final Validator validator;

    public CDATransformationServiceImpl(final TMConfiguration config, final CodedElementList codedElementList, final TerminologyService terminologyService, final Validator validator) {
        this.config = Validate.notNull(config, "TMConfiguration must not be null");
        this.codedElementList = Validate.notNull(codedElementList, "CodedElementList must not be null");
        this.terminologyService = Validate.notNull(terminologyService, "TerminologyService must not be null");
        this.validator = Validate.notNull(validator, "Validator must not be null");

        fillServiceTypeMaps();
    }

    public TMResponseStructure translate(final Document pivotCDA, final String targetLanguageCode) {
        return translate(pivotCDA, targetLanguageCode, null);
    }

    @Override
    public TMResponseStructure translate(final Document pivotCDA, final String targetLanguageCode, final NcpSide ncpSide) {
        logger.info("Translating OpenNCP CDA Document [START]");
        final StopWatch watch = new StopWatch();
        watch.start();
        final TMResponseStructure responseStructure = process(pivotCDA, targetLanguageCode, false, ncpSide);
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            try {
                loggerClinical.debug("Translate CDA: \n'{}'", XMLUtil.prettyPrint(responseStructure.getDocument()));
            } catch (final Exception e) {
                logger.error("Exception: '{}'", e.getMessage(), e);
            }
        }
        watch.stop();
        logger.info("Translation of CDA executed in: '{}ms'", watch.getTotalTimeMillis());
        logger.info("Translating OpenNCP CDA Document [END]");
        logger.info("Translation of OpenNCP CDA Document ended with status [{}]", responseStructure.getStatus().name());
        logTransformationResult(responseStructure);
        return responseStructure;
    }

    /**
     * @param friendlyCDA Medical document in its original data format as provided from the NationalConnector
     *                    to this component. The provided document is compliant with the epSOS pivot CDA
     *                    (see D 3.5.2 Appendix C) unless the adoption of the element binding with the epSOS
     *                    reference Value Sets. [Mandatory]
     * @return
     */
    public TMResponseStructure transcode(final Document friendlyCDA) {
        return transcode(friendlyCDA, null);
    }

    @Override
    public TMResponseStructure transcode(final Document friendlyCDA, final NcpSide ncpSide) {
        logger.info("Transcoding OpenNCP CDA Document [START]");
        final StopWatch watch = new StopWatch();
        watch.start();
        final TMResponseStructure responseStructure = process(friendlyCDA, null, true, ncpSide);

        watch.stop();
        logger.info("Transformation of CDA executed in: '{}ms'", watch.getTotalTimeMillis());
        logger.info("Transcoding OpenNCP CDA Document [END]");
        logger.info("Transcoding of OpenNCP CDA Document ended with status [{}]", responseStructure.getStatus().name());
        logTransformationResult(responseStructure);
        return responseStructure;
    }

    @Override
    public ValueSet translateValueSet(final String oid, final String targetLanguage) {
        final List<RetrievedConcept> valueSetConcepts = terminologyService.getValueSetConcepts(oid, null, targetLanguage);
        final var valueSet = new ValueSet();
        valueSet.setId(oid);
        final ValueSet.ConceptSetComponent conceptSetComponent = new ValueSet.ConceptSetComponent();
        valueSetConcepts.forEach(retrievedConcept ->
                conceptSetComponent.addConcept(buildConcept(retrievedConcept.getCode(), retrievedConcept.getDesignation(), targetLanguage)));
        valueSet.getCompose().addInclude(conceptSetComponent);
        return valueSet;
    }

    private ValueSet.ConceptReferenceComponent buildConcept(final String code, final String designation, final String targetLanguage) {
        final var conceptReferenceComponent = new ValueSet.ConceptReferenceComponent();
        conceptReferenceComponent.setCode(code);
        final var conceptReferenceDesignationComponent = new ValueSet.ConceptReferenceDesignationComponent();
        conceptReferenceDesignationComponent.setLanguage(targetLanguage);
        conceptReferenceDesignationComponent.setValue(designation);
        conceptReferenceComponent.addDesignation(conceptReferenceDesignationComponent);
        return conceptReferenceComponent;
    }

    private TMResponseStructure process(final Document inputDocument, final String targetLanguageCode, final boolean isTranscode, final NcpSide ncpSide) {

        logger.info("Processing CDA Document: '{}', Target Language: '{}', Transcoding: '{}'",
                inputDocument.toString(), targetLanguageCode, isTranscode);
        TMResponseStructure responseStructure;
        final List<ITMTSAMError> errors = new ArrayList<>();
        final List<ITMTSAMError> warnings = new ArrayList<>();
        final byte[] inputDocbytes;

        try {
            // validate schema
            inputDocbytes = XmlUtil.doc2bytes(inputDocument);
            final Document namespaceAwareDoc = XmlUtil.getNamespaceAwareDocument(inputDocbytes);

            // Checking Document type and if the Document is structured or unstructured
            final String cdaDocumentType = getCDADocumentType(inputDocument);

            // XSD Validation disabled: boolean schemaValid = Validator.validateToSchema(namespaceAwareDoc);

            // MDA validation
            if (config.isModelValidationEnabled()) {
                final ModelValidatorResult validateMDA = validator.validateMDA(new String(inputDocbytes), cdaDocumentType, isTranscode);

                if (!validateMDA.isSchemaValid()) {
                    warnings.add(TMError.WARNING_INPUT_XSD_VALIDATION_FAILED);
                }
                if (!validateMDA.isModelValid()) {
                    warnings.add(TMError.WARNING_OUTPUT_MDA_VALIDATION_FAILED);
                }
            }

            //  XSD Schema Validation
            if (config.isSchemaValidationEnabled()) {

                final boolean schemaValid = validator.validateToSchema(namespaceAwareDoc);
                // if model validation is enabled schema validation is done as part of it so there is no point doing it again
                if (!schemaValid) {
                    warnings.add(TMError.WARNING_INPUT_XSD_VALIDATION_FAILED);
                }
            }

            // Schematron Validation
            if (config.isSchematronValidationEnabled()) {
                // if transcoding, validate against friendly scheme,
                // else against pivot scheme
                final SchematronResult result = validator.validateSchematron(inputDocument, cdaDocumentType, isTranscode);
                if (result == null || !result.isValid()) {
                    warnings.add(TMError.WARNING_INPUT_SCHEMATRON_VALIDATION_FAILED);
                    logger.error("Schematron validation error, input document is invalid!");
                    if (result != null) {
                        logger.error(result.toString());
                    }
                }
            } else {
                logger.info("Schematron validation disabled");
            }
            logger.info(isTranscode ? "Transcoding of the CDA Document: '{}'" : "Translating of the CDA Document: '{}'", cdaDocumentType);
            // transcode/translate document
            if (isTranscode) {
                transcodeDocument(inputDocument, errors, warnings, cdaDocumentType);
            } else {
                translateDocument(inputDocument, targetLanguageCode, errors, warnings, cdaDocumentType);
            }

            final Document finalDoc = XmlUtil.removeEmptyXmlns(inputDocument);

            if (config.isModelValidationEnabled()) {
                final ModelValidatorResult validateMDA = validator.validateMDA(XmlUtil.xmlToString(finalDoc),
                        cdaDocumentType, !isTranscode);
                if (!validateMDA.isSchemaValid()) {
                    warnings.add(TMError.WARNING_OUTPUT_XSD_VALIDATION_FAILED);
                }
                if (!validateMDA.isModelValid()) {
                    warnings.add(TMError.WARNING_OUTPUT_MDA_VALIDATION_FAILED);
                }
            }
            // validate RESULT (schematron)
            if (config.isSchematronValidationEnabled()) {

                final SchematronResult result = validator.validateSchematron(finalDoc, cdaDocumentType, !isTranscode);
                if (result == null || !result.isValid()) {
                    warnings.add(TMError.WARNING_OUTPUT_SCHEMATRON_VALIDATION_FAILED);
                    responseStructure = new TMResponseStructure(Base64Util.encode(finalDoc), errors, warnings);
                    logger.error("Schematron validation error, result document is invalid!");
                    if (logger.isErrorEnabled() && result != null) {
                        logger.error(result.toString());
                    }
                    return responseStructure;
                }
            } else {
                logger.debug("Schematron validation disabled");
            }

            // create & fill TMResponseStructure
            responseStructure = new TMResponseStructure(Base64Util.encode(finalDoc), errors, warnings);
            if (logger.isDebugEnabled()) {
                logger.debug("TM result:\n{}", responseStructure);
            }
        } catch( final TMException e){
            // Writing TMException to ResponseStructure
            logger.error("TMException: '{}'\nReason: '{}'", e.getMessage(), e.getReason().toString(), e);
            errors.add(e.getReason());
            responseStructure = new TMResponseStructure(Base64Util.encode(inputDocument), errors, warnings);

        } catch( final Exception e){
            // Writing ERROR to ResponseStructure
            logger.error("Exception: '{}'", e.getMessage(), e);
            errors.add(TMError.ERROR_PROCESSING_ERROR);
            responseStructure = new TMResponseStructure(Base64Util.encode(inputDocument), errors, warnings);
            logger.error("Exception: TM Error Code: '{}'", TMError.ERROR_PROCESSING_ERROR, e);
        }

        // Transformation Service - Audit Message Handling
        if (ncpSide != null) {
            writeAuditTrail(responseStructure, ncpSide);
        }

        return responseStructure;
    }

    /**
     * Method checks for CDA document code and body and returns constant determining Document type
     * (PatientSummary, ePrescription, eDispensation) and level of CDA document (1 - unstructured, or 3 - structured)
     *
     * @param document input CDA document
     * @return Constant which determines one of six Document types
     * @throws Exception
     */
    public String getCDADocumentType ( final Document document) throws Exception {

        final List<Node> nodeList = XmlUtil.getNodeList(document, XPATH_CLINICALDOCUMENT_CODE);

        // Document type code
        final String docTypeCode;
        // exactly 1 document type element should exist
        if (nodeList.size() == 1 && nodeList.get(0).getNodeType() == Node.ELEMENT_NODE) {
            final Element docTypeCodeElement = (Element) nodeList.get(0);
            docTypeCode = docTypeCodeElement.getAttribute(CODE);
            logger.info("CDA Document Type Code: '{}'", docTypeCode);
            if (StringUtils.isBlank(docTypeCode)) {
                throw new TMException(TMError.ERROR_DOCUMENT_CODE_NOT_EXIST);
            }
        } else {
            logger.error("Problem obtaining document type code ! found /ClinicalDocument/code elements: '{}'", nodeList.size());
            throw new TMException(TMError.ERROR_DOCUMENT_CODE_NOT_EXIST);
        }

        // Document level (1 - unstructured or 3 - structured)
        final boolean level3Doc;
        // check if structuredDocument
        final Node nodeStructuredBody = XmlUtil.getNode(document, XPATH_STRUCTUREDBODY);
        // LEVEL 3 document
        level3Doc = nodeStructuredBody != null;

        final String docTypeConstant;
        // find constant for Document type
        if (level3Doc) {
            docTypeConstant = level3Type.get(docTypeCode);
        } else {
            docTypeConstant = level1Type.get(docTypeCode);
        }

        if (docTypeConstant == null) {
            throw new TMException(new TMErrorCtx(TMError.ERROR_DOCUMENT_CODE_UNKNOWN, docTypeCode));
        }
        return docTypeConstant;
    }

    /**
     * Method iterates document for translated coded elements, calls for each
     * TSAM.getDesignationByEpSOSConcept method, Input document is enriched with
     * translation elements (translated Concept), list of errors & warnings is
     * filled, finally status of operation is returned
     *
     * @param document           - translated CDA document
     * @param targetLanguageCode - language Code
     * @param errors             empty list for TMErrors
     * @param warnings           empty list for TMWarnings
     */
    private void translateDocument ( final Document document, final String targetLanguageCode,
                                     final List<ITMTSAMError> errors,
                                     final List<ITMTSAMError> warnings, final String cdaDocumentType){

        logger.info("Translating Document '{}' to target Language: '{}'", cdaDocumentType, targetLanguageCode);
        processDocument(document, targetLanguageCode, errors, warnings, cdaDocumentType, Boolean.FALSE);
    }

    /**
     * Method iterates document for coded elements, calls for each TSAM.getEpSOSConceptByCode method,
     * Input document is enriched with translation elements (transcoded Concept), list of errors & warnings is filled,
     * finally status of operation is returned
     *
     * @param document        Original CDA document
     * @param errors          Empty list for TMErrors
     * @param warnings        Empty list for TMWarnings
     * @param cdaDocumentType Type of CDA document to process
     */
    private void transcodeDocument ( final Document document, final List<ITMTSAMError> errors,
                                     final List<ITMTSAMError> warnings, final String cdaDocumentType){

        logger.info("Transcoding Document '{}'", cdaDocumentType);
        processDocument(document, null, errors, warnings, cdaDocumentType, Boolean.TRUE);
    }

    /**
     * @param document
     * @param targetLanguageCode
     * @param errors
     * @param warnings
     * @param cdaDocumentType
     * @param isTranscode
     */
    private void processDocument ( final Document document, final String targetLanguageCode,
                                   final List<ITMTSAMError> errors,
                                   final List<ITMTSAMError> warnings, final String cdaDocumentType, final boolean isTranscode){

        //TODO: Check is an attribute shall/can also be translated and/or transcoded like the XML element.
        logger.info("Processing Document '{}' to target Language: '{}' Transcoding: '{}'", cdaDocumentType, targetLanguageCode, isTranscode);
        // hashMap for ID of referencedValues and transcoded/translated DisplayNames
        final HashMap<String, String> hmReffId_DisplayName = new HashMap<>();

        if (config.isConfigurableElementIdentification()) {

            final Collection<CodedElementListItem> ceList = codedElementList.getList(cdaDocumentType);
            logger.info("Configurable Element Identification is set, CodedElementList for '{}' contains elements: '{}'",
                    cdaDocumentType, ceList.size());
            if (logger.isDebugEnabled()) {
                for (final CodedElementListItem listItem : ceList) {
                    logger.debug("Usage: '{}', XPath: '{}', VgalueSet: '{}'", listItem.getUsage(), listItem.getxPath(), listItem.getValueSet());
                }
            }
            if (ceList.isEmpty()) {
                warnings.add(TMError.WARNING_CODED_ELEMENT_LIST_EMPTY);
            }
            final Iterator<CodedElementListItem> iProcessed = ceList.iterator();
            CodedElementListItem codedElementListItem;
            String xPathExpression;
            List<Node> nodeList;
            boolean isRequired;
            String celTargetLanguageCode;
            boolean useCELTargetLanguageCode;

            while (iProcessed.hasNext()) {

                codedElementListItem = iProcessed.next();
                xPathExpression = codedElementListItem.getxPath();
                isRequired = codedElementListItem.isRequired();

                if (!isTranscode) {
                    celTargetLanguageCode = codedElementListItem.getTargetLanguageCode();

                    // if targetLanguageCode is specified in CodedElementList, this is used for translation
                    useCELTargetLanguageCode = StringUtils.isNotEmpty(celTargetLanguageCode);
                    logger.debug("Language has been specified for Coded Element: '{}' - '{}'", codedElementListItem.getxPath(),
                            useCELTargetLanguageCode ? codedElementListItem.getTargetLanguageCode() : useCELTargetLanguageCode);
                }

                nodeList = XmlUtil.getNodeList(document, xPathExpression);
                logger.debug("Found: '{}' elements", (nodeList == null ? "NULL" : nodeList.size()));

                if (isRequired && (nodeList == null || nodeList.isEmpty())) {

                    if (logger.isErrorEnabled()) {
                        logger.error("Required element is missing: '{}'", codedElementListItem);
                    }
                    errors.add(new TMErrorCtx(TMError.ERROR_REQUIRED_CODED_ELEMENT_MISSING, codedElementListItem.toString()));
                } else {

                    Element originalElement;
                    if (nodeList != null) {

                        for (final Node aNodeList : nodeList) {
                            // Iterate elements for processing
                            if (aNodeList.getNodeType() == Node.ELEMENT_NODE) {
                                originalElement = (Element) aNodeList;
                                // Checking if xsi:type is "CE" or "CD"
                                checkCodedElementType(originalElement, warnings);

                                // Calling TSAM transcode/translate method for each coded element configured according CDA type.
                                final boolean success = (isTranscode ?
                                        transcodeElement(originalElement, document, hmReffId_DisplayName, null, null, errors, warnings)
                                        : translateElement(originalElement, document, targetLanguageCode, hmReffId_DisplayName, null, null, errors, warnings));

                                // If is required & processing is unsuccessful, report ERROR
                                if (isRequired && !success) {
                                    final String ctx = XmlUtil.getElementPath(originalElement);
                                    errors.add(isTranscode ? new TMErrorCtx(TMError.ERROR_REQUIRED_CODED_ELEMENT_NOT_TRANSCODED, ctx)
                                            : new TMErrorCtx(TMError.ERROR_REQUIRED_CODED_ELEMENT_NOT_TRANSLATED, ctx));
                                    logger.error("Required coded element was not translated");
                                }
                            }
                        }
                    }
                }
            }
        } else {

            logger.info("Configurable Element Identification is NOT set - looking for //*[@code] elements");
            final List<Node> nodeList = XmlUtil.getNodeList(document, XPATH_ALL_ELEMENTS_WITH_CODE_ATTR);
            logger.info("Found '{}' elements to translate/transcode", nodeList.size());
            Element originalElement;
            for (final Node aNodeList : nodeList) {

                if (aNodeList.getNodeType() == Node.ELEMENT_NODE) {
                    // iterate elements for translation
                    originalElement = (Element) aNodeList;
                    // if element name is translation, don't do anything
                    if (TRANSLATION.equals(originalElement.getLocalName())) {

                        final CodedElement ce = new CodedElement(originalElement);
                        if (logger.isDebugEnabled()) {
                            logger.debug("translation element - skipping: '{}'", ce);
                        }
                        continue;
                    }
                    // check if xsi:type is "CE" or "CD"
                    checkCodedElementType(originalElement, warnings);

                    // call TSAM transcode/translate method for each coded element
                    final boolean success = (isTranscode ?
                            transcodeElement(originalElement, document, hmReffId_DisplayName, null, null, errors, warnings) :
                            translateElement(originalElement, document, targetLanguageCode, hmReffId_DisplayName, null, null, errors, warnings));
                    if (!success) {
                        logger.error("Required coded element was not translated");
                    }
                }
            }
        }
    }

    /**
     * Calls TSAM.getEpSOSConceptByCode method, if transcoding is successful, constructs translation element
     * for original data, new/transcoded data are placed in original element.
     *
     * @param originalElement     - transcoded Coded Element
     * @param document            - input CDA document
     * @param hmReffIdDisplayName hashMap for ID of referencedValues and
     *                            transcoded DisplayNames
     * @param warnings
     * @param errors
     * @return boolean - true if SUCCES otherwise false
     */
    private boolean transcodeElement ( final Element originalElement, final Document document,
                                       final HashMap<String, String> hmReffIdDisplayName,
                                       final String valueSet, final String valueSetVersion, final List<ITMTSAMError> errors,
                                       final List<ITMTSAMError> warnings){

        return processElement(originalElement, document, null, hmReffIdDisplayName, valueSet,
                valueSetVersion, true, errors, warnings);
    }

    private boolean translateElement ( final Element originalElement, final Document document,
                                       final String targetLanguageCode,
                                       final HashMap<String, String> hmReffIdDisplayName, final String valueSet, final String valueSetVersion,
                                       final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings){

        return processElement(originalElement, document, targetLanguageCode, hmReffIdDisplayName, valueSet,
                valueSetVersion, false, errors, warnings);
    }

    /**
     * @param originalElement
     * @param document
     * @param targetLanguageCode
     * @param hmReffIdDisplayName
     * @param valueSet
     * @param valueSetVersion
     * @param isTranscode
     * @param errors
     * @param warnings
     * @return
     */
    private boolean processElement ( final Element originalElement, final Document document,
                                     final String targetLanguageCode,
                                     final HashMap<String, String> hmReffIdDisplayName, final String valueSet, final String valueSetVersion,
                                     final boolean isTranscode, final List<ITMTSAMError> errors, final List<ITMTSAMError> warnings){

        //TODO: Update the translation Node while the translation/transcoding process
        try {
            // Checking mandatory attributes
            final Boolean checkAttributes = checkAttributes(originalElement, warnings);
            if (checkAttributes != null) {
                return checkAttributes;
            }

            final CodeConcept codeConcept = CodeConcept.from(originalElement, valueSet, valueSetVersion);

            // looking for a nested translation element
            Node oldTranslationElement = findOldTranslation(originalElement);

            final TSAMResponseStructure tsamResponse = isTranscode ? terminologyService.getTargetConcept(codeConcept)
                    : terminologyService.getDesignation(codeConcept, targetLanguageCode);

            if (tsamResponse.isStatusSuccess()) {
                logger.debug("Processing successful '{}'", codeConcept);
                // +++++ Element editing BEGIN +++++

                // NEW TRANSLATION element
                final Element newTranslation = document.createElementNS(EHDSI_HL7_NAMESPACE, TRANSLATION);
                if (originalElement.getPrefix() != null) {
                    newTranslation.setPrefix(originalElement.getPrefix());
                }
                boolean attributesFilled = false;
                // check - no repeated attributed in translation element by
                // transcoding
                // if codeSystem && code for source and target are same
                if (StringUtils.isNotEmpty(tsamResponse.getCodeSystem()) && codeConcept.getCodeSystemOid().isPresent()
                        && !codeConcept.getCodeSystemOid().get().equalsIgnoreCase(tsamResponse.getCodeSystem())
                        || (codeConcept.getCodeSystemOid().get().equalsIgnoreCase(tsamResponse.getCodeSystem())
                        && !codeConcept.getCode().equals(tsamResponse.getCode()))) {
                    // code
                    if (StringUtils.isNotEmpty(codeConcept.getCode())) {
                        newTranslation.setAttribute(CODE, codeConcept.getCode());
                    }
                    // codeSystem
                    codeConcept.getCodeSystemOid()
                            .filter(StringUtils::isNotEmpty)
                            .ifPresent(codeSystemOid -> newTranslation.setAttribute(CODE_SYSTEM, codeSystemOid));
                    // codeSystemName
                    codeConcept.getCodeSystemName()
                            .filter(StringUtils::isNotEmpty)
                            .ifPresent(codeSystemName -> newTranslation.setAttribute(CODE_SYSTEM_NAME, codeSystemName));
                    // codeSystemVersion
                    codeConcept.getCodeSystemVersion()
                            .filter(StringUtils::isNotEmpty)
                            .ifPresent(codeSystemVersion -> newTranslation.setAttribute(CODE_SYSTEM_VERSION, codeSystemVersion));
                    attributesFilled = true;
                }
                // designation (only if source and target differs)
                if (codeConcept.getDisplayName().isPresent() && !tsamResponse.getDesignation().equals(codeConcept.getDisplayName().get())) {
                    codeConcept.getDisplayName().ifPresent(displayName -> newTranslation.setAttribute(DISPLAY_NAME, displayName));

                    if (StringUtils.isNotEmpty(codeConcept.getCode())) {
                        newTranslation.setAttribute(CODE, codeConcept.getCode());
                    }

                    if (codeConcept.getCodeSystemOid().isPresent()) {
                        newTranslation.setAttribute(CODE_SYSTEM, codeConcept.getCodeSystemOid().get());
                    }

                    codeConcept.getCodeSystemName()
                            .filter(StringUtils::isNotEmpty)
                            .ifPresent(codeSystemName -> newTranslation.setAttribute(CODE_SYSTEM_NAME, codeSystemName));
                    attributesFilled = true;
                } else {
                    logger.debug("Translation is same as original: '{}'", tsamResponse.getDesignation());
                }
                if (attributesFilled) {
                    if (oldTranslationElement != null) {
                        oldTranslationElement = originalElement.removeChild(oldTranslationElement);
                        newTranslation.appendChild(oldTranslationElement);
                    }
                    originalElement.appendChild(newTranslation);
                }

                // CHANGE original attributes code
                if (StringUtils.isNotEmpty(tsamResponse.getCode())) {
                    originalElement.setAttribute(CODE, tsamResponse.getCode());
                }
                // codeSystem
                if (StringUtils.isNotEmpty(tsamResponse.getCodeSystem())) {
                    originalElement.setAttribute(CODE_SYSTEM, tsamResponse.getCodeSystem());
                }
                // codeSystemName
                if (StringUtils.isNotEmpty(tsamResponse.getCodeSystemName())) {
                    originalElement.setAttribute(CODE_SYSTEM_NAME, tsamResponse.getCodeSystemName());
                }
                // codeSystemVersion
                if (StringUtils.isNotEmpty(tsamResponse.getCodeSystemVersion())) {
                    originalElement.setAttribute(CODE_SYSTEM_VERSION, tsamResponse.getCodeSystemVersion());
                }
                // designation
                if (StringUtils.isNotEmpty(tsamResponse.getDesignation())) {
                    originalElement.setAttribute(DISPLAY_NAME, tsamResponse.getDesignation());
                }
                // +++++ Element editing END +++++
                errors.addAll(tsamResponse.getErrors());
                warnings.addAll(tsamResponse.getWarnings());
                return true;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing failure! for Code: '{}'", codeConcept);
                }
                errors.addAll(tsamResponse.getErrors());
                warnings.addAll(tsamResponse.getWarnings());
                return false;
            }
        } catch (final Exception e) {
            // system error
            logger.error("processing failure! ", e);
            return false;
        }
    }

    private void writeAuditTrail ( final TMResponseStructure responseStructure, final NcpSide ncpSide){

        logger.debug("[Transformation Service] Audit trail BEGIN");

        if (responseStructure != null) {

            try {
                final GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTime(new Date());
                final String securityHeader = "[No security header provided]";
                final EventLog eventLog = EventLog.createEventLogPivotTranslation(
                        TransactionName.PIVOT_TRANSLATION,
                        EventActionCode.EXECUTE,
                        DATATYPE_FACTORY.newXMLGregorianCalendar(calendar),
                        responseStructure.getStatus().equals(TMStatus.SUCCESS) ? EventOutcomeIndicator.FULL_SUCCESS : EventOutcomeIndicator.PERMANENT_FAILURE,
                        HttpUtil.getSubjectDN(false),
                        getOIDFromDocument(responseStructure.getDocument()),
                        getOIDFromDocument(responseStructure.getDocument()),
                        Constants.UUID_PREFIX + responseStructure.getRequestId(),
                        securityHeader.getBytes(StandardCharsets.UTF_8),
                        Constants.UUID_PREFIX + responseStructure.getRequestId(),
                        securityHeader.getBytes(StandardCharsets.UTF_8),
                        IPUtil.getPrivateServerIp()
                );
                eventLog.setEventType(EventType.PIVOT_TRANSLATION);
                eventLog.setNcpSide(ncpSide);

                AuditServiceFactory.getInstance().write(eventLog, config.getAuditTrailFacility(), config.getAuditTrailSeverity());
                logger.info("Write AuditTrail: '{}'", eventLog.getEventType());

            } catch (final Exception e) {
                logger.error("Audit trail ERROR! ", e);
            }
            logger.debug("[Transformation Service] Audit trail END");
        } else {
            logger.error("Write AuditTrail Error: Cannot process Transformation Manager response");
        }
    }

    private Node findOldTranslation ( final Element originalElement){

        Node oldTranslationElement = null;
        final NodeList nodeList = originalElement.getChildNodes();
        if (nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                final Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && StringUtils.equals(TMConstants.TRANSLATION, node.getLocalName())) {
                    oldTranslationElement = node;
                    logger.debug("Old translation found");
                    break;
                }
            }
        }
        return oldTranslationElement;
    }


    /**
     * @param originalElement
     * @param warnings
     */
    private void checkCodedElementType ( final Element originalElement, final List<ITMTSAMError> warnings){

        if (originalElement != null && StringUtils.isNotBlank(originalElement.getAttribute(XSI_TYPE))) {

            final Attr attr = originalElement.getAttributeNodeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");

            if (attr != null) {
                final String suffix;
                final int colon = attr.getValue().indexOf(':');
                if (colon == -1) {
                    suffix = attr.getValue();
                } else {
                    suffix = attr.getValue().substring(colon + 1);
                }
                if (!StringUtils.equals(suffix, CE) && !StringUtils.equals(suffix, CD)) {
                    logger.debug("TSAM Warning: '{}-'{}''", TMError.WARNING_CODED_ELEMENT_NOT_PROPER_TYPE.getCode(),
                            TMError.WARNING_CODED_ELEMENT_NOT_PROPER_TYPE.getDescription());
                    warnings.add(TMError.WARNING_CODED_ELEMENT_NOT_PROPER_TYPE);
                }
            }
        }
    }

    /**
     * Check mandatory attributes.
     *
     * @param originalElement
     * @param warnings
     * @return Returns true if it is allowed not to have mandatory attributes, false if not, null if everything is ok
     */
    private Boolean checkAttributes ( final Element originalElement, final List<ITMTSAMError> warnings){

        final String elName = XmlUtil.getElementPath(originalElement);
        if (logger.isDebugEnabled()) {
            logger.debug("Required attributes for Element Path:\n'{}'", elName);
        }
        if (originalElement.hasAttribute("nullFlavor")) {
            logger.debug("nullFlavor, skippink: '{}'", elName);
            return true;
        } else {
            boolean noCode = false;
            boolean noCodeSystem = false;
            if (!originalElement.hasAttribute("code")) {
                noCode = true;
            }

            if (!originalElement.hasAttribute("codeSystem")) {
                noCodeSystem = true;
            }
            if (noCode || noCodeSystem) {
                final NodeList origText = originalElement.getElementsByTagName("originalText");
                if (origText.getLength() > 0) {
                    // ak element obsahuje originalText, preskocit, nevyhazovat warning
                    logger.debug("Element without required attributes, but has originalText, ignoring: '{}'", elName);
                    return true;
                } else {
                    logger.debug("Element has no \"code or \"codeSystem\" attribute: '{}'", elName);
                    warnings.add(new TMErrorCtx(TMError.WARNING_MANDATORY_ATTRIBUTES_MISSING, "Element " + elName));
                    return false;
                }
            }
            return null;
        }
    }

    /**
     * Obtains the unique identifier of the document.
     *
     * @param clinicalDocument - Current CDA processed.
     * @return Formatted OID identifying the CDA document.
     */
    private String getOIDFromDocument ( final Document clinicalDocument){

        String oid = "";
        if (clinicalDocument.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "id").getLength() > 0) {
            final Node id = clinicalDocument.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "id").item(0);
            if (id.getAttributes().getNamedItem("root") != null) {
                oid = oid + id.getAttributes().getNamedItem("root").getTextContent();
            }
            if (id.getAttributes().getNamedItem("extension") != null) {
                oid = oid + "^" + id.getAttributes().getNamedItem("extension").getTextContent();
            }
        }
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.debug("Document OID: '{}'", oid);
        }
        return oid;
    }

    public void fillServiceTypeMaps () {
        logger.debug("Filling service type maps");

        level1Type = new HashMap<>();
        level1Type.put(config.getPatientSummaryCode(), PATIENT_SUMMARY1);
        level1Type.put(config.getePrescriptionCode(), EPRESCRIPTION1);

        level3Type = new HashMap<>();
        level3Type.put(config.getPatientSummaryCode(), PATIENT_SUMMARY3);
        level3Type.put(config.geteDispensationCode(), EDISPENSATION3);
        level3Type.put(config.getePrescriptionCode(), EPRESCRIPTION3);
    }

    private void logTransformationResult(TMResponseStructure responseStructure) {
        logErrorMessages(responseStructure.getErrors());
        logWarningMessages(responseStructure.getWarnings());
    }

    private void logErrorMessages(List<ITMTSAMError> errors) {
        if (!errors.isEmpty()) {
            logger.error("[{}] errors encountered during transformation process", errors.size());
            AtomicInteger counter = new AtomicInteger(1);
            errors.forEach(error ->
                    logger.error("Error {} : Code [{}] and description [{}]",
                            counter.getAndIncrement(), error.getCode(), error.getDescription())
            );
        }
    }

    private void logWarningMessages(List<ITMTSAMError> warnings) {
        if (!warnings.isEmpty()) {
            logger.warn("[{}] warnings encountered during transformation process", warnings.size());
            AtomicInteger counter = new AtomicInteger(1);
            warnings.forEach(warning ->
                    logger.warn("Warning {} : Code [{}] and description [{}]",
                            counter.getAndIncrement(), warning.getCode(), warning.getDescription())
            );
        }
    }
}
