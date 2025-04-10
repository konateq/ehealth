package eu.europa.ec.sante.openncp.core.server.ihe.xca;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.util.AssertionUtil;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import eu.europa.ec.sante.openncp.core.common.assertion.PolicyAssertionManager;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.OpenNCPErrorCodeException;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidationResult;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidator;
import eu.europa.ec.sante.openncp.core.common.ihe.RegistryErrorSeverity;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.*;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.Base64Util;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.error.TMError;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import eu.europa.ec.sante.openncp.core.server.EventLogUtil;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.*;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xca.DocumentSearchInterface;
import eu.europa.ec.sante.openncp.core.server.ihe.AdhocQueryResponseStatus;
import eu.europa.ec.sante.openncp.core.server.ihe.IheErrorCode;
import eu.europa.ec.sante.openncp.core.server.ihe.NationalConnectorFactory;
import eu.europa.ec.sante.openncp.core.server.ihe.RegistryErrorUtils;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.extrinsicobjectbuilder.ep.EPExtrinsicObjectBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.extrinsicobjectbuilder.orcd.OrCDExtrinsicObjectBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.extrinsicobjectbuilder.ps.PSExtrinsicObjectBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.soap.SOAPHeader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static eu.europa.ec.sante.openncp.common.ClassCode.*;

@Service
public class XcaServiceServerSideImpl implements XcaServiceServerSide {

    private static final DatatypeFactory DATATYPE_FACTORY;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (final DatatypeConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(XcaServiceServerSideImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final AssertionValidator assertionValidator;
    private final PolicyAssertionManager policyAssertionManager;
    private final CDATransformationService cdaTransformationService;
    private final NationalConnectorFactory nationalConnectorFactory;

    /**
     * Public Constructor for IHE XCA Profile implementation, the default constructor will handle the loading of
     * the National Connector implementation by using the <class>ServiceLoader</class>
     *
     * @see ServiceLoader
     */
    public XcaServiceServerSideImpl(final AssertionValidator assertionValidator, final PolicyAssertionManager policyAssertionManager, final CDATransformationService cdaTransformationService, final NationalConnectorFactory nationalConnectorFactory) {
        this.assertionValidator = Validate.notNull(assertionValidator, "assertionValidator must not be null");
        this.policyAssertionManager = Validate.notNull(policyAssertionManager, "policyAssertionManager must not be null");
        this.cdaTransformationService = Validate.notNull(cdaTransformationService, "cdaTransformationService must not be null");
        this.nationalConnectorFactory = Validate.notNull(nationalConnectorFactory, "nationalConnectorFactory must not be null");
    }

    /**
     * XCA list operation implementation, returns the list of patient summaries or ePrescriptions, depending on the query.
     */
    @Override
    public AdhocQueryResponse queryDocument(final AdhocQueryRequest adhocQueryRequest, final SOAPHeader soapHeader, final EventLog eventLog) throws Exception {
        final DocumentSearchInterface documentSearchInterface = nationalConnectorFactory.createDocumentSearchInstance();
        try {
            return adhocQueryResponseBuilder(documentSearchInterface, adhocQueryRequest, soapHeader, eventLog);
        } catch (final UnsupportedOperationException uoe) {
            return handleUnsupportedOperationException(adhocQueryRequest, uoe);
        }
    }

    /**
     * XCA retrieve operation implementation, returns the particular document requested by the caller.
     * The response is placed in the OMElement
     */
    @Override
    public RetrieveDocumentSetResponse retrieveDocument(final RetrieveDocumentSetRequest request, final SOAPHeader soapHeader, final EventLog eventLog)
            throws Exception {
        final DocumentSearchInterface documentSearchService = nationalConnectorFactory.createDocumentSearchInstance();
        return retrieveDocumentSetBuilder(documentSearchService, request, soapHeader, eventLog);
    }

    public static List<ClassCode> getClassCodesOrCD() {
        final List<ClassCode> list = new ArrayList<>();
        list.add(ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE);
        list.add(ORCD_LABORATORY_RESULTS_CLASSCODE);
        list.add(ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE);
        list.add(ORCD_MEDICAL_IMAGES_CLASSCODE);
        return list;
    }

    private void prepareEventLogForQuery(final EventLog eventLog, final AdhocQueryRequest request, final AdhocQueryResponse response, final Element sh, final ClassCode classCode) {

        logger.info("method prepareEventLogForQuery(Request: '{}', ClassCode: '{}')", request.getId(), classCode);
        eventLog.setEventType(EventType.XCA_SERVICE_LIST);
        if (classCode == null) {
            // In case the document is not found, audit log cannot be properly filled, as we don't know the event type
            // Log this under Order Service
            eventLog.setEI_TransactionName(TransactionName.ORDER_SERVICE_RETRIEVE);
        } else {
            eventLog.setEI_TransactionName(TransactionName.determineTransactionNameForXCAQuery(List.of(classCode)));
        }
        eventLog.setEI_EventActionCode(EventActionCode.EXECUTE);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        eventLog.setPS_ParticipantObjectIDs(EventLogUtil.getDocumentEntryPatientId(request));

        if (response.getRegistryObjectList() != null) {
            final List<String> documentIds = getDocumentIds(response);
            eventLog.setEventTargetParticipantObjectIds(documentIds);
        }

        // Set the operation status to the response
        handleEventLogStatus(eventLog, response, request);

        final String userIdAlias = SoapElementHelper.getAssertionsSPProvidedId(sh);
        eventLog.setHR_UserID(
                StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "<" + SoapElementHelper.getUserID(sh) + "@" + SoapElementHelper.getAssertionsIssuer(sh) + ">");
        eventLog.setHR_AlternativeUserID(SoapElementHelper.getAlternateUserID(sh));
        eventLog.setHR_RoleID(SoapElementHelper.getRoleID(sh));
        eventLog.setSP_UserID(HttpUtil.getSubjectDN(true));
        eventLog.setPT_ParticipantObjectIDs(EventLogUtil.getDocumentEntryPatientId(request));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (response.getRegistryErrorList() != null) {
            final RegistryError registryError = response.getRegistryErrorList().getRegistryErrors().get(0);
            eventLog.setEM_ParticipantObjectID(registryError.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(registryError.getCodeContext().getBytes());
        }
    }

    @NotNull
    private static List<String> getDocumentIds(final AdhocQueryResponse response) {
        final List<String> documentIds = new ArrayList<>();
        for (var i = 0; i < response.getRegistryObjectList().getIdentifiables().size(); i++) {
            if (!(response.getRegistryObjectList().getIdentifiables().get(i).getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType eot = (ExtrinsicObjectType) response.getRegistryObjectList().getIdentifiables().get(i).getValue();
            for (final ExternalIdentifierType externalIdentifierType : eot.getExternalIdentifiers()) {
                if (externalIdentifierType.getIdentificationScheme().equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME)) {
                    documentIds.add(externalIdentifierType.getValue());
                }
            }
        }
        return documentIds;
    }

    private static void handleEventLogStatus(final EventLog eventLog, final AdhocQueryResponse queryResponse, final AdhocQueryRequest queryRequest) {

        if (queryResponse.getRegistryObjectList() == null) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            // In case of failure, the document class code has been provided to the event log as event target as there is no
            // reference available as resources (document ID etc.).
            EventLogUtil.setDocumentType(eventLog, queryRequest);
        } else if (queryResponse.getRegistryErrorList() == null) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
            // In case of failure, the document class code has been provided to the event log as event target as there is no
            // reference available as resources (document ID etc.).
            EventLogUtil.setDocumentType(eventLog, queryRequest);
        }
    }

    private void prepareEventLogForRetrieve(final EventLog eventLog, final RetrieveDocumentSetRequest request, final boolean errorsDiscovered,
                                            final boolean documentReturned, final RegistryErrorList registryErrorList, final Element sh, final ClassCode classCode) {

        logger.info("method prepareEventLogForRetrieve({})", classCode);
        eventLog.setEventType(EventType.XCA_SERVICE_RETRIEVE_NCP_A);
        eventLog.setEI_TransactionName(classCode != null ? TransactionName.determineTransactionNameForXCARetrieve(classCode) : TransactionName.ORDER_SERVICE_RETRIEVE);
        eventLog.setEI_EventActionCode(EventActionCode.READ);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        eventLog.getEventTargetParticipantObjectIds().add(request.getDocumentRequests().get(0).getDocumentUniqueId());

        if (!documentReturned) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
        } else if (!errorsDiscovered) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
        }

        final String userIdAlias = SoapElementHelper.getAssertionsSPProvidedId(sh);
        eventLog.setHR_UserID(
                StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "<" + SoapElementHelper.getUserID(sh) + "@" + SoapElementHelper.getAssertionsIssuer(sh) + ">");
        eventLog.setHR_AlternativeUserID(SoapElementHelper.getAlternateUserID(sh));
        eventLog.setHR_RoleID(SoapElementHelper.getRoleID(sh));
        eventLog.setSP_UserID(HttpUtil.getSubjectDN(true));
        eventLog.setPT_ParticipantObjectIDs(List.of(extractFullPatientId(sh)));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (errorsDiscovered) {
            final List<RegistryError> registryErrors = registryErrorList.getRegistryErrors();
            //Include only the first error in the audit log.
            if (!registryErrors.isEmpty()) {
                final RegistryError error = registryErrors.get(0);
                if (logger.isDebugEnabled()) {
                    try {
                        logger.debug("Error to be included in audit: '{}'", registryErrorToXml(error));
                    } catch (final Exception e) {
                        logger.debug("Exception: '{}'", e.getMessage(), e);
                    }
                }
                eventLog.setEM_ParticipantObjectID(error.getErrorCode());
                eventLog.setEM_ParticipantObjectDetail(error.getCodeContext().getBytes());
            }
        }
    }

    public static String registryErrorToXml(final RegistryError error) throws Exception {
        final JAXBContext jaxbContext = JAXBContext.newInstance(RegistryError.class);
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        final StringWriter writer = new StringWriter();
        marshaller.marshal(error, writer);
        return writer.toString();
    }

    private static List<ClassCode> getDocumentEntryClassCodes(final AdhocQueryRequest request) {
        final List<ClassCode> classCodes = new ArrayList<>();
        for (final Slot slotType1 : request.getAdhocQuery().getSlots()) {
            if (slotType1.getName().equals("$XDSDocumentEntryClassCode")) {
                var fullClassCodeString = slotType1.getValueList().getValues().get(0);
                final var pattern = "\\(?\\)?\\'?";
                fullClassCodeString = fullClassCodeString.replaceAll(pattern, "");
                final String[] classCodeString = fullClassCodeString.split(",");
                for (String classCode : classCodeString) {
                    classCode = classCode.substring(0, classCode.indexOf("^^"));
                    classCodes.add(getByCode(classCode));
                }
            }
        }
        return classCodes;
    }

    private static ClassCode getFirstClassCode(final List<ClassCode> classCodeList) {
        return classCodeList.stream().findFirst().orElse(null);
    }

    private static FilterParams getFilterParams(final AdhocQueryRequest request) {

        final var filterParams = new FilterParams();

        for (final Slot slotType : request.getAdhocQuery().getSlots()) {
            switch (slotType.getName()) {
                case XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERCREATEDAFTER_SLOT_NAME:
                    filterParams.setCreatedAfter(Instant.parse(slotType.getValueList().getValues().get(0)));
                    break;
                case XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERCREATEDBEFORE_SLOT_NAME:
                    filterParams.setCreatedBefore(Instant.parse(slotType.getValueList().getValues().get(0)));
                    break;
                case XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERMAXIMUMSIZE_SLOT_NAME:
                    filterParams.setMaximumSize(Long.parseLong(slotType.getValueList().getValues().get(0)));
                    break;
                default:
                    break;
            }
        }
        return filterParams;
    }

    /**
     * Extracts repositoryUniqueId from request
     *
     * @return repositoryUniqueId
     */
    private static String getRepositoryUniqueId(final RetrieveDocumentSetRequest request) {

        return request.getDocumentRequests().get(0).getRepositoryUniqueId();
    }

    private AssociationType1 makeAssociation(final String source, final String target) {

        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final var association = objectFactory.createAssociationType1();
        association.setId(uuid);
        association.setAssociationType("urn:ihe:iti:2007:AssociationType:XFRM");
        association.setSourceObject(source);
        association.setTargetObject(target);
        //  Gazelle does not like this information when validating. Uncomment if really needed.
        //        association.getClassification().add(ClassificationBuilder.build(
        //                "urn:uuid:abd807a3-4432-4053-87b4-fd82c643d1f3",
        //                uuid,
        //                "epSOS pivot",
        //                "epSOS translation types",
        //                "Translation into epSOS pivot format"));
        return association;
    }

    /**
     * Main part of the XCA query operation implementation, builds a AdhocQueryResponse based on the request and SOAP data
     */
    private AdhocQueryResponse adhocQueryResponseBuilder(final DocumentSearchInterface documentSearchInterface, final AdhocQueryRequest request, final SOAPHeader soapHeader, final EventLog eventLog) {

        // Extract all necessary data in a data object

        // with this data object, do validation checks
        // List<ValidationResults> validationResults = validationRules.map(validationRule -> validationRule.doCheck(dataObject)).collect(Collectors.toList())

        // if not validation errors

        // do businesslogic

        // send to NI

        String responseStatus = AdhocQueryResponseStatus.FAILURE;
        // What's being requested: eP or PS?
        final List<ClassCode> classCodeValues = getDocumentEntryClassCodes(request);
        final RegistryErrorList registryErrorList = objectFactory.createRegistryErrorList();
        final AdhocQueryResponse adhocQueryResponse = objectFactory.createAdhocQueryResponse();
        // Create Registry Object List
        adhocQueryResponse.setRegistryObjectList(objectFactory.createRegistryObjectList());

        final RequestData requestData = extractAndValidateRequestData(documentSearchInterface, soapHeader, classCodeValues, registryErrorList);
        if (requestData.isEmpty()) {
            return adhocQueryResponse;
        }

        if (!EventLogUtil.getDocumentEntryPatientId(request).contains(requestData.getFullPatientId())) {
            // Patient ID in TRC assertion does not match the one given in the request. Return "No documents found".
            OpenNCPErrorCode code = OpenNCPErrorCode.ERROR_DOCUMENT_NOT_FOUND;
            final ClassCode classCode = getFirstClassCode(classCodeValues);
            switch (classCode) {
                case EP_CLASSCODE:
                    code = OpenNCPErrorCode.ERROR_EP_NOT_FOUND;
                    break;
                case PS_CLASSCODE:
                    code = OpenNCPErrorCode.ERROR_PS_NOT_FOUND;
                    break;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    code = OpenNCPErrorCode.ERROR_ORCD_NOT_FOUND;
                    break;
            }
            RegistryErrorUtils.addErrorMessage(registryErrorList, code, code.getDescription(), RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
            adhocQueryResponse.setStatus(AdhocQueryResponseStatus.FAILURE);
            adhocQueryResponse.setRegistryErrorList(registryErrorList);
            prepareEventLogForQuery(eventLog, request, adhocQueryResponse, requestData.getShElement(), classCode);
            return adhocQueryResponse;
        }

        var countryCode = "";
        final String distinguishedName = eventLog.getSC_UserID();
        final int cIndex = distinguishedName.indexOf("C=");

        if (cIndex > 0) {
            countryCode = distinguishedName.substring(cIndex + 2, cIndex + 4);
        }
        // This part is added for handling consents when the call is not https.
        // In this case, we check the country code of the signature certificate that ships within the HCP assertion
        // TODO: Might be necessary to remove later, although it does no harm in reality!
        else {
            logger.info("Could not get client country code from the service consumer certificate. " +
                    "The reason can be that the call was not via HTTPS. " + "Will check the country code from the signature certificate now.");
            if (requestData.getSigCountryCode() != null) {
                logger.info("Found the client country code via the signature certificate.");
                countryCode = requestData.getSigCountryCode();
            }
        }
        logger.info("The client country code to be used by the PDP: '{}'", countryCode);

        // Then, it is the Policy Decision Point (PDP) that decides according to the consent of the patient
        if (!policyAssertionManager.isConsentGiven(requestData.getFullPatientId(), countryCode)) {
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_PS_NO_CONSENT,
                    OpenNCPErrorCode.ERROR_PS_NO_CONSENT.getDescription(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        if (classCodeValues.isEmpty()) {
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN,
                    OpenNCPErrorCode.ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN.getDescription(),
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        for (final ClassCode classCodeValue : classCodeValues) {
            try {
                policyAssertionManager.xcaPermissionvalidator(requestData.getHcpAssertionDetails().getAssertion(), classCodeValue);
                switch (classCodeValue) {
                    case EP_CLASSCODE:

                        final List<DocumentAssociation<EPDocumentMetaData>> prescriptions = documentSearchInterface.getEPDocumentList(
                                DocumentFactory.createSearchCriteria().addPatientId(requestData.getFullPatientId()));

                        if (prescriptions == null) {
                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_EP_REGISTRY_NOT_ACCESSIBLE,
                                    OpenNCPErrorCode.ERROR_EP_REGISTRY_NOT_ACCESSIBLE.getDescription(),
                                    RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.FAILURE;
                        } else if (prescriptions.isEmpty()) {
                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_EP_NOT_FOUND,
                                    OpenNCPErrorCode.ERROR_EP_NOT_FOUND.getDescription(),
                                    RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.FAILURE;
                        } else {
                            // Multiple prescriptions mean multiple PDF and XML files, multiple ExtrinsicObjects and associations
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                            for (final DocumentAssociation<EPDocumentMetaData> prescription : prescriptions) {

                                logger.debug("Prescription Repository ID: '{}'", prescription.getXMLDocumentMetaData().getRepositoryId());
                                final String xmlUUID;
                                final ExtrinsicObjectType eotXML = objectFactory.createExtrinsicObjectType();
                                xmlUUID = EPExtrinsicObjectBuilder.build(request, eotXML, prescription.getXMLDocumentMetaData());
                                adhocQueryResponse.getRegistryObjectList().getIdentifiables().add(objectFactory.createExtrinsicObject(eotXML));

                                final String pdfUUID;
                                final var eotPDF = objectFactory.createExtrinsicObjectType();
                                pdfUUID = EPExtrinsicObjectBuilder.build(request, eotPDF, prescription.getPDFDocumentMetaData());
                                adhocQueryResponse.getRegistryObjectList().getIdentifiables().add(objectFactory.createExtrinsicObject(eotPDF));

                                if (StringUtils.isNotBlank(xmlUUID) && StringUtils.isNotBlank(pdfUUID)) {
                                    adhocQueryResponse.getRegistryObjectList()
                                            .getIdentifiables()
                                            .add(objectFactory.createAssociation(makeAssociation(pdfUUID, xmlUUID)));
                                }
                            }
                        }
                        break;
                    case PS_CLASSCODE:
                        final DocumentAssociation<PSDocumentMetaData> psDoc = documentSearchInterface.getPSDocumentList(
                                DocumentFactory.createSearchCriteria().addPatientId(requestData.getFullPatientId()));

                        if (psDoc == null || (psDoc.getPDFDocumentMetaData() == null && psDoc.getXMLDocumentMetaData() == null)) {

                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_PS_NOT_FOUND,
                                    "No patient summary is registered for the given patient.",
                                    RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                        } else {

                            final PSDocumentMetaData docPdf = psDoc.getPDFDocumentMetaData();
                            final PSDocumentMetaData docXml = psDoc.getXMLDocumentMetaData();
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;

                            var xmlUUID = "";
                            if (docXml != null) {
                                final var eotXML = objectFactory.createExtrinsicObjectType();
                                xmlUUID = PSExtrinsicObjectBuilder.build(request, eotXML, docXml, false);
                                adhocQueryResponse.getRegistryObjectList().getIdentifiables().add(objectFactory.createExtrinsicObject(eotXML));
                            }
                            var pdfUUID = "";
                            if (docPdf != null) {
                                final var eotPDF = objectFactory.createExtrinsicObjectType();
                                pdfUUID = PSExtrinsicObjectBuilder.build(request, eotPDF, docPdf, true);
                                adhocQueryResponse.getRegistryObjectList().getIdentifiables().add(objectFactory.createExtrinsicObject(eotPDF));
                            }
                            if (!xmlUUID.isEmpty() && !pdfUUID.isEmpty()) {
                                adhocQueryResponse.getRegistryObjectList().getIdentifiables().add(objectFactory.createAssociation(makeAssociation(pdfUUID, xmlUUID)));
                            }
                        }
                        break;
                    case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                    case ORCD_LABORATORY_RESULTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGES_CLASSCODE:
                        final var searchCriteria = DocumentFactory.createSearchCriteria().addPatientId(requestData.getFullPatientId());
                        final var filterParams = getFilterParams(request);
                        if (filterParams.getMaximumSize() != null) {
                            searchCriteria.add(SearchCriteria.Criteria.MAXIMUM_SIZE, filterParams.getMaximumSize().toString());
                        }
                        if (filterParams.getCreatedBefore() != null) {
                            searchCriteria.add(SearchCriteria.Criteria.CREATED_BEFORE, filterParams.getCreatedBefore().toString());
                        }
                        if (filterParams.getCreatedAfter() != null) {
                            searchCriteria.add(SearchCriteria.Criteria.CREATED_AFTER, filterParams.getCreatedAfter().toString());
                        }

                        final List<OrCDDocumentMetaData> orCDDocumentMetaDataList = getOrCDDocumentMetaDataList(documentSearchInterface, classCodeValue, searchCriteria);

                        if (orCDDocumentMetaDataList == null) {
                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_ORCD_GENERIC,
                                    "orCD registry could not be accessed.",
                                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                            responseStatus = AdhocQueryResponseStatus.FAILURE;
                        } else if (orCDDocumentMetaDataList.isEmpty()) {
                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_ORCD_NOT_FOUND,
                                    "There is no original clinical data of the requested type registered for the given " +
                                            "patient.", RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                        } else {

                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                            for (final OrCDDocumentMetaData orCDDocumentMetaData : orCDDocumentMetaDataList) {
                                logger.debug("OrCD Document Repository ID: '{}'", orCDDocumentMetaData.getRepositoryId());
                                buildOrCDExtrinsicObject(request, adhocQueryResponse, orCDDocumentMetaData);
                            }
                        }
                        break;

                    default:
                        RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN,
                                "Class code not supported for XCA query(" + classCodeValue + ").",
                                RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                        responseStatus = AdhocQueryResponseStatus.SUCCESS;
                        break;
                }
            } catch (final NIException e) {
                final var codeContext = e.getOpenncpErrorCode().getDescription() + "^" + e.getMessage();
                RegistryErrorUtils.addErrorMessage(registryErrorList, e.getOpenncpErrorCode(), codeContext,
                        e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                responseStatus = AdhocQueryResponseStatus.FAILURE;
            } catch (final OpenNCPErrorCodeException e) {
                final var codeContext = e.getErrorCode().getDescription();
                RegistryErrorUtils.addErrorMessage(registryErrorList, e.getErrorCode(), codeContext,
                        e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                responseStatus = AdhocQueryResponseStatus.FAILURE;
            } catch (final Throwable e) {
                final OpenNCPErrorCode openNCPErrorCode = OpenNCPErrorCode.ERROR_GENERIC;
                final var codeContext = openNCPErrorCode.getDescription();
                RegistryErrorUtils.addErrorMessage(registryErrorList, openNCPErrorCode, codeContext,
                        e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                responseStatus = AdhocQueryResponseStatus.FAILURE;
            } finally {
                try {
                    if (!registryErrorList.getRegistryErrors().isEmpty()) {
                        adhocQueryResponse.setRegistryErrorList(registryErrorList);
                    }
                    prepareEventLogForQuery(eventLog, request, adhocQueryResponse, requestData.getShElement(), classCodeValue);
                } catch (final Exception e) {
                    logger.error("Prepare Audit log failed: '{}'", e.getMessage(), e);
                    // Is this fatal?
                }
            }
        }
        if (!registryErrorList.getRegistryErrors().isEmpty()) {
            adhocQueryResponse.setRegistryErrorList(registryErrorList);
        }

        adhocQueryResponse.setStatus(responseStatus);

        return adhocQueryResponse;
    }

    private static String extractFullPatientId(final Element shElement) {
        return SoapElementHelper.getDocumentEntryPatientIdFromTRCAssertion(shElement);
    }

    private List<OrCDDocumentMetaData> getOrCDDocumentMetaDataList(final DocumentSearchInterface documentSearchService, final ClassCode classCode, final SearchCriteria searchCriteria)
            throws NIException, InsufficientRightsException {

        List<OrCDDocumentMetaData> orCDDocumentMetaDataList = new ArrayList<>();
        switch (classCode) {
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                orCDDocumentMetaDataList = documentSearchService.getOrCDHospitalDischargeReportsDocumentList(searchCriteria);
                break;
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
                orCDDocumentMetaDataList = documentSearchService.getOrCDLaboratoryResultsDocumentList(searchCriteria);
                break;
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                orCDDocumentMetaDataList = documentSearchService.getOrCDMedicalImagingReportsDocumentList(searchCriteria);
                break;
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
                orCDDocumentMetaDataList = documentSearchService.getOrCDMedicalImagesDocumentList(searchCriteria);
                break;
            default:
                // eHDSI supports only 4 types of OrCD documents.
                logger.warn("Document type requested is not currently supported!");
                break;
        }

        return orCDDocumentMetaDataList;
    }

    private void buildOrCDExtrinsicObject(final AdhocQueryRequest request, final AdhocQueryResponse response, final OrCDDocumentMetaData orCDDocumentMetaData) {

        final var eotXML = objectFactory.createExtrinsicObjectType();
        final String xmlUUID = OrCDExtrinsicObjectBuilder.build(request, eotXML, orCDDocumentMetaData);
        response.getRegistryObjectList().getIdentifiables().add(objectFactory.createExtrinsicObject(eotXML));
        if (!StringUtils.isEmpty(xmlUUID)) {
            response.getRegistryObjectList().getIdentifiables().add(objectFactory.createAssociation(makeAssociation(xmlUUID, xmlUUID)));
        }
    }

    private Document transformDocument(final Document doc, final RegistryErrorList registryErrorList, final RegistryResponseType registryResponseElement, final boolean isTranscode,
                                       final EventLog eventLog) {

        logger.debug("Transforming document, isTranscode: '{}' - Event Type: '{}'", isTranscode, eventLog.getEventType());
        if (eventLog.getReqM_ParticipantObjectDetail() != null) {
            final var requester = new String(eventLog.getReqM_ParticipantObjectDetail());
            if (loggerClinical.isDebugEnabled() &&
                    !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                loggerClinical.debug("Participant Requester: '{}'", requester);
            }
        }
        if (eventLog.getResM_ParticipantObjectDetail() != null) {
            final var responder = new String(eventLog.getResM_ParticipantObjectDetail());
            if (loggerClinical.isDebugEnabled() &&
                    !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                loggerClinical.debug("Participant Responder: '{}'", responder);
            }
        }

        final Document returnDoc;
        try {
            final TMResponseStructure tmResponse;
            final String operationType;
            if (isTranscode) {
                operationType = "transcode";
                logger.debug("Transforming friendly CDA document to pivot CDA...");
                tmResponse = cdaTransformationService.transcode(doc, NcpSide.NCP_A);
            } else {
                operationType = "translate";
                logger.debug("Translating document to [{}]'", Constants.LANGUAGE_CODE);
                tmResponse = cdaTransformationService.translate(doc, Constants.LANGUAGE_CODE, NcpSide.NCP_A);
            }

            for (final ITMTSAMError error : tmResponse.getErrors()) {
                RegistryErrorUtils.addErrorMessage(registryErrorList, error, operationType, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            }

            for (final ITMTSAMError warning : tmResponse.getWarnings()) {
                RegistryErrorUtils.addErrorMessage(registryErrorList, warning, operationType, RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
            }

            returnDoc = Base64Util.decode(tmResponse.getResponseCDA());
            if (!registryErrorList.getRegistryErrors().isEmpty()) {
                registryResponseElement.setRegistryErrorList(registryErrorList);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
        return returnDoc;
    }

    private RetrieveDocumentSetResponse retrieveDocumentSetBuilder(final DocumentSearchInterface documentSearchService, final RetrieveDocumentSetRequest request, final SOAPHeader soapHeader, final EventLog eventLog)
            throws Exception {

        final RegistryErrorList registryErrorList = objectFactory.createRegistryErrorList();
        final RetrieveDocumentSetResponse documentSetResponse = objectFactory.createRetrieveDocumentSetResponse();
        final RegistryResponseType registryResponse = objectFactory.createRegistryResponseType();
        documentSetResponse.setRegistryResponse(registryResponse);
        final RetrieveDocumentSetResponse.DocumentResponse documentResponse = objectFactory.createRetrieveDocumentSetResponseDocumentResponse();

        var documentReturned = false;
        var failure = false;

        ClassCode classCodeValue = null;

        // Start processing within a labeled block, break on certain errors
        processLabel:
        {

            documentSearchService.setSOAPHeader(soapHeader);

            final AssertionDetails hcpAssertionDetails = validateAssertionsAndGetHCPAssertion(soapHeader);
            final String documentId = request.getDocumentRequests().get(0).getDocumentUniqueId();
            final String fullPatientId = extractFullPatientId(soapHeader);
            final String repositoryId = getRepositoryUniqueId(request);
            if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                loggerClinical.debug("Retrieving clinical document by criteria:\nPatient ID: '{}'\nDocument ID: '{}'\nRepository ID: '{}'",
                        fullPatientId, documentId, repositoryId);
            }
            //try getting country code from the certificate
            String countryCode = null;
            final String distinguishedName = eventLog.getSC_UserID();
            logger.info("[Certificate] Distinguished Name: '{}'", distinguishedName);
            final int cIndex = distinguishedName.indexOf("C=");
            if (cIndex > 0) {
                countryCode = distinguishedName.substring(cIndex + 2, cIndex + 4);
            }
            // Mustafa: This part is added for handling consents when the call is not https. In this case, we check
            // the country code of the signature certificate that ships within the HCP assertion
            // TODO: Might be necessary to remove later, although it does no harm in reality!
            if (countryCode == null) {
                logger.info("Could not get client country code from the service consumer certificate. " +
                        "The reason can be that the call was not via HTTPS. " +
                        "Will check the country code from the signature certificate now.");
                countryCode = hcpAssertionDetails.getCountryCode().orElse(null);
                if (countryCode != null) {
                    logger.info("Found the client country code via the signature certificate.");
                } else {
                    failure = true;
                    RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS,
                            OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS.getDescription(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                    break processLabel;
                }
            }

            logger.info("The client country code to be used by the PDP '{}' ", countryCode);

            // Then, it is the Policy Decision Point (PDP) that decides according to the consent of the patient
            if (!policyAssertionManager.isConsentGiven(fullPatientId, countryCode)) {
                failure = true;
                RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_PS_NO_CONSENT,
                        OpenNCPErrorCode.ERROR_PS_NO_CONSENT.getDescription(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            }

            //TODO: EHNCP-1271 - Shall we indicate a specific ERROR Code???
            final EPSOSDocument epsosDoc;
            try {
                epsosDoc = documentSearchService.getDocument(DocumentFactory.createSearchCriteria()
                        .add(SearchCriteria.Criteria.DOCUMENT_ID, documentId)
                        .addPatientId(fullPatientId)
                        .add(SearchCriteria.Criteria.REPOSITORY_ID, repositoryId));
            } catch (final NIException e) {
                logger.error("NIException: '{}'", e.getMessage(), e);
                final var codeContext = e.getOpenncpErrorCode().getDescription() + "^" + e.getMessage();
                RegistryErrorUtils.addErrorMessage(registryErrorList, e.getOpenncpErrorCode(), codeContext, e,
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);

                failure = true;
                break processLabel;
            }

            if (epsosDoc == null) {

                //  Evidence for response from NI in case of failure
                //  This should be NRR of NCPA receiving from NI. This was throwing errors because we were not passing an XML document.
                //  We're passing data like: "SearchCriteria: {patientId = 12445ASD}"
                //  So we provided an XML representation of such data. Still, evidence is generated based on request data, not response.
                //  This NRR is optional as per the CP. So we leave this commented.
                //                try {
                //                    EvidenceUtils.createEvidenceREMNRR(DocumentFactory.createSearchCriteria().add(Criteria.PatientId, patientId)
                //                    .asXml(),
                //                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
                //                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
                //                            tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                //                            IHEEventType.epsosPatientServiceRetrieve.getCode(),
                //                            new DateTime(),
                //                            EventOutcomeIndicator.TEMPORAL_FAILURE.getCode().toString(),
                //                            "NI_XCA_RETRIEVE_RES_FAIL",
                //                            Helper.getTRCAssertion(soapHeaderElement).getID() + "__" + DateUtil.getCurrentTimeGMT());
                //                } catch (Exception e) {
                //                    logger.error(ExceptionUtils.getStackTrace(e));
                //                }
                logger.error("[National Connector] No document returned by the National Infrastructure");
                RegistryErrorUtils.addErrorMessage(registryErrorList, IheErrorCode.XDSMissingDocument,
                        OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING.getCode() + " : " +
                                OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING.getDescription(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            }

            // Evidence for response from NI in case of success
            /* Joao: This should be NRR of NCPA receiving from NI.
                    This was throwing errors because we were not passing an XML document.
                    We're passing data like:
                    "SearchCriteria: {patientId = 12445ASD}"
                    So we provided an XML representation of such data. Still, evidence is generated based on request data, not response.
                    This NRR is optional as per the CP. So we leave this commented */
            //            try {
            //                EvidenceUtils.createEvidenceREMNRR(DocumentFactory.createSearchCriteria().add(Criteria.PatientId, patientId).asXml(),
            //                        tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
            //                        tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
            //                        tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
            //                        IHEEventType.epsosPatientServiceRetrieve.getCode(),
            //                        new DateTime(),
            //                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
            //                        "NI_XCA_RETRIEVE_RES_SUCC",
            //                        DateUtil.getCurrentTimeGMT());
            //            } catch (Exception e) {
            //                logger.error(ExceptionUtils.getStackTrace(e));
            //            }

            classCodeValue = epsosDoc.getClassCode();
            policyAssertionManager.xcaPermissionvalidator(hcpAssertionDetails.getAssertion(), classCodeValue);

            logger.info("XCA Retrieve Request is valid.");
            documentResponse.setHomeCommunityId(request.getDocumentRequests().get(0).getHomeCommunityId());
            documentResponse.setRepositoryUniqueId(request.getDocumentRequests().get(0).getRepositoryUniqueId());
            documentResponse.setDocumentUniqueId(documentId);
            documentResponse.setMimeType(MediaType.TEXT_XML_VALUE);

            logger.info("XCA Retrieve Response has been created.");
            try {
                Document doc = epsosDoc.getDocument();
                logger.info("Client userID: '{}'", eventLog.getSC_UserID());

                if (doc != null) {
                    logger.info("[National Infrastructure] CDA Document:\n'{}'", epsosDoc.getClassCode().getCode());
                    /* Validate CDA eHDSI Friendly */
                    if (GazelleValidation.isValidationEnable()) {
                        GazelleValidation.validateCdaDocument(XMLUtil.documentToString(epsosDoc.getDocument()), NcpSide.NCP_A,
                                epsosDoc.getClassCode(), false);
                    }
                    // Transcode to eHDSI Pivot
                    if (!getClassCodesOrCD().contains(classCodeValue)) {
                        doc = transformDocument(doc, registryErrorList, registryResponse, true, eventLog);
                    }
                    if (!checkIfOnlyWarnings(registryErrorList)) {

                        // If the transformation process has raised at least one FATAL Error, we should determine which
                        // XCAError code has to be provided according the corresponding TM Error Code
                        final List<RegistryError> errors = registryErrorList.getRegistryErrors();
                        for (final RegistryError error : errors) {

                            logger.error("Error: '{}'-'{}'", error.getErrorCode(), error.getValue());
                            logger.error("TRANSCODING ERROR: '{}'-'{}'", TMError.ERROR_REQUIRED_CODED_ELEMENT_NOT_TRANSCODED.getCode(),
                                    error.getErrorCode());

                            if (StringUtils.startsWith(error.getErrorCode(), "45")) {

                                OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.ERROR_TRANSCODING_ERROR;
                                String openNcpErrorCodeDescription = openncpErrorCode.getDescription();
                                final String errorCodeContext = error.getCodeContext();

                                if (Objects.requireNonNull(classCodeValue) == EP_CLASSCODE) {
                                    openncpErrorCode = OpenNCPErrorCode.ERROR_EP_MISSING_EXPECTED_MAPPING;
                                } else if (classCodeValue == PS_CLASSCODE) {
                                    openncpErrorCode = OpenNCPErrorCode.ERROR_PS_MISSING_EXPECTED_MAPPING;
                                }
                                if (StringUtils.isNotBlank(errorCodeContext)) {
                                    openNcpErrorCodeDescription = openncpErrorCode.getDescription() + " [" + errorCodeContext + "]";
                                }

                                RegistryErrorUtils.addErrorMessage(registryErrorList, openncpErrorCode, openNcpErrorCodeDescription,
                                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                                // If the error is FATAL flag failure has been set to true
                                failure = true;
                                break;
                            }
                        }
                    } else {
                        /* Validate CDA eHDSI Pivot if no error during the transformation */
                        if (GazelleValidation.isValidationEnable()) {
                            GazelleValidation.validateCdaDocument(XMLUtil.documentToString(doc), NcpSide.NCP_A, epsosDoc.getClassCode(), true);
                        }
                    }
                }

                // If there is no failure during the process, the CDA document has been attached to the response
                logger.info("Error Registry: Failure '{}'", failure);
                if (!failure) {
                    final byte[] documentBytes = XMLUtil.documentToByteArray(doc);
                    documentResponse.setDocument(documentBytes);
                    documentReturned = true;
                }
            } catch (final Exception e) {
                OpenNCPErrorCode code = OpenNCPErrorCode.ERROR_GENERIC;

                switch (classCodeValue) {
                    case EP_CLASSCODE:
                        code = OpenNCPErrorCode.ERROR_EP_GENERIC;
                        break;
                    case PS_CLASSCODE:
                        code = OpenNCPErrorCode.ERROR_PS_GENERIC;
                        break;
                    case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                    case ORCD_LABORATORY_RESULTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGES_CLASSCODE:
                        code = OpenNCPErrorCode.ERROR_ORCD_GENERIC;
                        break;
                }

                failure = true;
                logger.error("Exception: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorMessage(registryErrorList, code, e.getMessage(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            }
        }

        // If the registryErrorList is empty or contains only Warning, the status of the request is SUCCESS
        if (registryErrorList.getRegistryErrors().isEmpty()) {
            logger.info("XCA Retrieve Document - Transformation Status: '{}'\nDefault Case", AdhocQueryResponseStatus.SUCCESS);
            registryResponse.setStatus(AdhocQueryResponseStatus.SUCCESS);
        } else {
            if (checkIfOnlyWarnings(registryErrorList)) {
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nCheck Warning", AdhocQueryResponseStatus.SUCCESS);
                registryResponse.setStatus(AdhocQueryResponseStatus.SUCCESS);
            } else if (failure) {
                // If there is a failure during the request process, the status is FAILURE
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nCheck Warning Failure: '{}'", AdhocQueryResponseStatus.FAILURE,
                        failure);
                registryResponse.setStatus(AdhocQueryResponseStatus.FAILURE);
            } else {
                //  Otherwise the status is PARTIAL SUCCESS
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nOtherwise...", AdhocQueryResponseStatus.PARTIAL_SUCCESS);
                registryResponse.setStatus(AdhocQueryResponseStatus.PARTIAL_SUCCESS);
            }
        }

        logger.info("Preparing Event Log of the Response:");
        try {
            final boolean errorsDiscovered = !registryErrorList.getRegistryErrors().isEmpty();
            if (errorsDiscovered) {
                registryResponse.setRegistryErrorList(registryErrorList);
            }

            if (documentReturned) {
                documentSetResponse.getDocumentResponses().add(documentResponse);
            }
            prepareEventLogForRetrieve(eventLog, request, errorsDiscovered, documentReturned, registryErrorList, soapHeader, classCodeValue);
        } catch (final Exception ex) {
            logger.error("Prepare Audit log failed. '{}'", ex.getMessage(), ex);
            // TODO: TWG to decide if this is this fatal
        }

        return documentSetResponse;
    }

    private AssertionDetails validateAssertionsAndGetHCPAssertion(final Element soapHeaderElement) throws InsufficientRightsException {
        final List<AssertionDetails> assertions = AssertionUtil.toAssertions(soapHeaderElement);
        final AssertionDetails hcpAssertionDetails = assertions.stream()
                .filter(assertionDetails -> assertionDetails.getAssertionType() == AssertionType.HCP)
                .findFirst()
                .orElseThrow(() -> new InsufficientRightsException("No valid HCP assertion found"));
        final List<AssertionValidationResult> assertionValidationResults = assertionValidator.validate(assertions);
        final List<String> failedValidationMessages = assertionValidationResults.stream()
                .flatMap((AssertionValidationResult assertionValidationResult) -> assertionValidationResult.getFailedValidationMessages().stream())
                .collect(Collectors.toList());
        if (!failedValidationMessages.isEmpty()) {
            throw new InsufficientRightsException(String.format("Assertion validation error: [%s]", String.join("\n", failedValidationMessages)));
        }
        return hcpAssertionDetails;
    }

    /**
     * This method will check if the Registry error list only contains Warnings.
     *
     * @return boolean value, indicating if the list only contains warnings.
     */
    private static boolean checkIfOnlyWarnings(final RegistryErrorList registryErrorList) {
        return RegistryErrorSeverity.ERROR_SEVERITY_ERROR.getText().equals(registryErrorList.getHighestSeverity());
    }

    /**
     * Method responsible for the AdhocQueryResponse message if the operation requested is not supported by the server.
     * RegistryError shall contain:
     * errorCode: required.
     * codeContext: required - Supplies additional detail for the errorCode.
     * severity: required - Indicates the severity of the error.
     * Shall be one of:
     * urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error
     * urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Warning
     * location: optional - Supplies the location of the error module name and line number or stack trace if appropriate.
     *
     * @param request - original AdhocQueryRequest
     * @param e       - Exception thrown by the system
     * @return response - populated with te specific Error Code according the document Class Code.
     */
    private AdhocQueryResponse handleUnsupportedOperationException(final AdhocQueryRequest request, final UnsupportedOperationException e) {

        final var adhocQueryResponse = objectFactory.createAdhocQueryResponse();
        final var registryErrorList = objectFactory.createRegistryErrorList();

        // Create Registry Object List
        adhocQueryResponse.setRegistryObjectList(objectFactory.createRegistryObjectList());

        final List<ClassCode> classCodeValues = getDocumentEntryClassCodes(request);
        OpenNCPErrorCode openNCPErrorCode;
        for (final ClassCode classCodeValue : classCodeValues) {
            switch (classCodeValue) {
                case EP_CLASSCODE:
                    openNCPErrorCode = OpenNCPErrorCode.ERROR_EP_NOT_FOUND;
                    break;
                case PS_CLASSCODE:
                    openNCPErrorCode = OpenNCPErrorCode.ERROR_PS_NOT_FOUND;
                    break;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    openNCPErrorCode = OpenNCPErrorCode.ERROR_ORCD_NOT_FOUND;
                    break;
                default:
                    openNCPErrorCode = OpenNCPErrorCode.ERROR_DOCUMENT_NOT_FOUND;
                    break;
            }
            RegistryErrorUtils.addErrorMessage(registryErrorList, openNCPErrorCode, openNCPErrorCode.getDescription(),
                    RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
        }
        adhocQueryResponse.setRegistryErrorList(registryErrorList);
        // Errors managed are only WARNING so the AdhocQueryResponse is considered as successful.
        adhocQueryResponse.setStatus(AdhocQueryResponseStatus.SUCCESS);
        return adhocQueryResponse;
    }


    private RequestData extractAndValidateRequestData(final DocumentSearchInterface documentSearchService, final SOAPHeader soapHeader, final List<ClassCode> classCodeValues, final RegistryErrorList registryErrorList) {
        final Element shElement = null;
        String sigCountryCode = null;
        AssertionDetails hcpAssertionDetails = null;
        try {
            documentSearchService.setSOAPHeader(soapHeader);
            hcpAssertionDetails = validateAssertionsAndGetHCPAssertion(soapHeader);
            sigCountryCode = hcpAssertionDetails.getCountryCode().orElse(null);
        } catch (final InsufficientRightsException ire) {
            logger.error(ire.getMessage(), ire);
            RegistryErrorUtils.addErrorMessage(registryErrorList, ire.getErrorCode(), ire.getMessage(), ire, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            return RequestData.empty();
        } catch (final Exception e) {
            OpenNCPErrorCode code = OpenNCPErrorCode.ERROR_GENERIC;
            switch (getFirstClassCode(classCodeValues)) {
                case EP_CLASSCODE:
                    code = OpenNCPErrorCode.ERROR_EP_GENERIC;
                    break;
                case PS_CLASSCODE:
                    code = OpenNCPErrorCode.ERROR_PS_GENERIC;
                    break;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    code = OpenNCPErrorCode.ERROR_ORCD_GENERIC;
                    break;
            }
            RegistryErrorUtils.addErrorMessage(registryErrorList, code, e.getMessage(), e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            throw e;
        }

        final String fullPatientId = extractFullPatientId(soapHeader);
        return new RequestData(sigCountryCode, soapHeader, fullPatientId, hcpAssertionDetails);
    }

    private static class RequestData {
        private final String sigCountryCode;
        private final Element shElement;
        private final String fullPatientId;
        private final AssertionDetails hcpAssertionDetails;

        public RequestData(final String sigCountryCode, final Element shElement, final String fullPatientId, final AssertionDetails hcpAssertionDetails) {
            this.sigCountryCode = sigCountryCode;
            this.shElement = shElement;
            this.fullPatientId = fullPatientId;
            this.hcpAssertionDetails = hcpAssertionDetails;
        }

        public String getSigCountryCode() {
            return sigCountryCode;
        }

        public Element getShElement() {
            return shElement;
        }

        public String getFullPatientId() {
            return fullPatientId;
        }

        public AssertionDetails getHcpAssertionDetails() {
            return hcpAssertionDetails;
        }

        public boolean isEmpty() {
            return StringUtils.isBlank(sigCountryCode)
                    && shElement == null
                    && StringUtils.isBlank(fullPatientId);
        }

        public static RequestData empty() {
            return new RequestData(null, null, null, null);
        }
    }
}

