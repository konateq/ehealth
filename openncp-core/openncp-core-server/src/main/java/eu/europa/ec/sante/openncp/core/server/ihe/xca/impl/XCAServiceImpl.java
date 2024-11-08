package eu.europa.ec.sante.openncp.core.server.ihe.xca.impl;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import eu.europa.ec.sante.openncp.common.util.UUIDHelper;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.constants.ihe.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.constants.ihe.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.RegistryErrorSeverity;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.OpenNCPErrorCodeException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML2Validator;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.*;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.ObjectFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.AssociationType1;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ExternalIdentifierType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ExtrinsicObjectType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.SlotType1;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryError;
import eu.europa.ec.sante.openncp.core.common.ihe.evidence.EvidenceUtils;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.Base64Util;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.error.TMError;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xca.DocumentSearchInterface;
import eu.europa.ec.sante.openncp.core.server.ihe.AdhocQueryResponseStatus;
import eu.europa.ec.sante.openncp.core.server.ihe.IheErrorCode;
import eu.europa.ec.sante.openncp.core.server.ihe.RegistryErrorUtils;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.XCAServiceInterface;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.ep.EPExtrinsicObjectBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.orcd.OrCDExtrinsicObjectBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.ps.PSExtrinsicObjectBuilder;
import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.activation.DataHandler;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.*;

import static eu.europa.ec.sante.openncp.common.ClassCode.*;

@Service
public class XCAServiceImpl implements XCAServiceInterface {

    private static final DatatypeFactory DATATYPE_FACTORY;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (final DatatypeConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(XCAServiceImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private final OMFactory omFactory = OMAbstractFactory.getOMFactory();
    private final ObjectFactory ofQuery = new ObjectFactory();
    private final eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ObjectFactory ofRim = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ObjectFactory();
    private final eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.ObjectFactory ofRs = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.ObjectFactory();
    private final DocumentSearchInterface documentSearchService;
    private final SAML2Validator saml2Validator;
    private final CDATransformationService cdaTransformationService;

    /**
     * Public Constructor for IHE XCA Profile implementation, the default constructor will handle the loading of
     * the National Connector implementation by using the <class>ServiceLoader</class>
     *
     * @see ServiceLoader
     */
    public XCAServiceImpl(final DocumentSearchInterface documentSearchService, final SAML2Validator saml2Validator, final CDATransformationService cdaTransformationService) {
        this.documentSearchService = Validate.notNull(documentSearchService);
        this.saml2Validator = Validate.notNull(saml2Validator);
        this.cdaTransformationService = cdaTransformationService;
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

        switch (classCode) {
            case EP_CLASSCODE:
                eventLog.setEventType(EventType.ORDER_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.ORDER_SERVICE_LIST);
                eventLog.setEI_EventActionCode(EventActionCode.EXECUTE);
                break;
            case PS_CLASSCODE:
                eventLog.setEventType(EventType.PATIENT_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.PATIENT_SERVICE_LIST);
                eventLog.setEI_EventActionCode(EventActionCode.EXECUTE);
                break;
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
                eventLog.setEventType(EventType.ORCD_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.ORCD_SERVICE_LIST);
                eventLog.setEI_EventActionCode(EventActionCode.EXECUTE);
                break;
            default:
                logger.warn("No event identification information found!");
                //  TODO: Analyzing if some specific codes are needed in this situation
                break;
        }
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        eventLog.setPS_ParticipantObjectIDs(getDocumentEntryPatientId(request));

        if (response.getRegistryObjectList() != null) {
            final List<String> documentIds = new ArrayList<>();
            for (var i = 0; i < response.getRegistryObjectList().getIdentifiable().size(); i++) {
                if (!(response.getRegistryObjectList().getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                final ExtrinsicObjectType eot = (ExtrinsicObjectType) response.getRegistryObjectList().getIdentifiable().get(i).getValue();
                for (final ExternalIdentifierType externalIdentifierType : eot.getExternalIdentifier()) {
                    if (externalIdentifierType.getIdentificationScheme().equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME)) {
                        documentIds.add(externalIdentifierType.getValue());
                    }
                }
            }
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
        eventLog.setPT_ParticipantObjectIDs(getDocumentEntryPatientId(request));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (response.getRegistryErrorList() != null) {
            final RegistryError registryError = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_ParticipantObjectID(registryError.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(registryError.getCodeContext().getBytes());
        }
    }

    private void handleEventLogStatus(final EventLog eventLog, final AdhocQueryResponse queryResponse, final AdhocQueryRequest queryRequest) {

        if (queryResponse.getRegistryObjectList() == null) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            // In case of failure, the document class code has been provided to the event log as event target as there is no
            // reference available as resources (document ID etc.).
            addDocumentClassCodeToEventLog(eventLog, queryRequest);
        } else if (queryResponse.getRegistryErrorList() == null) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
            // In case of failure, the document class code has been provided to the event log as event target as there is no
            // reference available as resources (document ID etc.).
            addDocumentClassCodeToEventLog(eventLog, queryRequest);
        }
    }

    private void addDocumentClassCodeToEventLog(final EventLog eventLog, final AdhocQueryRequest queryRequest) {

        for (final SlotType1 slotType1 : queryRequest.getAdhocQuery().getSlot()) {
            if (StringUtils.equals(slotType1.getName(), "$XDSDocumentEntryClassCode")) {
                String documentType = slotType1.getValueList().getValue().get(0);
                documentType = StringUtils.remove(documentType, "('");
                documentType = StringUtils.remove(documentType, "')");
                eventLog.getEventTargetParticipantObjectIds().add(documentType);
            }
        }
    }

    private void prepareEventLogForRetrieve(final EventLog eventLog, final RetrieveDocumentSetRequestType request, final boolean errorsDiscovered,
                                            final boolean documentReturned, final OMElement registryErrorList, final Element sh, final ClassCode classCode) {

        logger.info("method prepareEventLogForRetrieve({})", classCode);
        if (classCode == null) {
            // In case the document is not found, audit log cannot be properly filled, as we don't know the event type
            // Log this under Order Service
            eventLog.setEventType(EventType.ORDER_SERVICE_RETRIEVE);
            eventLog.setEI_TransactionName(TransactionName.ORDER_SERVICE_RETRIEVE);
        } else {
            switch (classCode) {
                case EP_CLASSCODE:
                    eventLog.setEventType(EventType.ORDER_SERVICE_RETRIEVE);
                    eventLog.setEI_TransactionName(TransactionName.ORDER_SERVICE_RETRIEVE);
                    eventLog.setEI_EventActionCode(EventActionCode.READ);
                    break;
                case PS_CLASSCODE:
                    eventLog.setEventType(EventType.PATIENT_SERVICE_RETRIEVE);
                    eventLog.setEI_TransactionName(TransactionName.PATIENT_SERVICE_RETRIEVE);
                    eventLog.setEI_EventActionCode(EventActionCode.READ);
                    break;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    eventLog.setEventType(EventType.ORCD_SERVICE_RETRIEVE);
                    eventLog.setEI_TransactionName(TransactionName.ORCD_SERVICE_RETRIEVE);
                    eventLog.setEI_EventActionCode(EventActionCode.READ);
                    break;
                default:
                    logger.warn("No event identification information found!");
                    //  TODO: Analyzing if some specific codes are needed in this situation
                    eventLog.setEI_EventActionCode(EventActionCode.READ);
                    break;
            }
        }
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        eventLog.getEventTargetParticipantObjectIds().add(request.getDocumentRequest().get(0).getDocumentUniqueId());

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
        eventLog.setPT_ParticipantObjectIDs(List.of(SoapElementHelper.getDocumentEntryPatientIdFromTRCAssertion(sh)));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (errorsDiscovered) {
            final Iterator<OMElement> re = registryErrorList.getChildElements();
            //Include only the first error in the audit log.
            if (re.hasNext()) {
                final OMElement error = re.next();
                if (logger.isDebugEnabled()) {
                    try {
                        logger.debug("Error to be included in audit: '{}'", XMLUtil.prettyPrint(XMLUtils.toDOM(error)));
                    } catch (final Exception e) {
                        logger.debug("Exception: '{}'", e.getMessage(), e);
                    }
                }
                eventLog.setEM_ParticipantObjectID(error.getAttributeValue(new QName("", "errorCode")));
                eventLog.setEM_ParticipantObjectDetail(error.getAttributeValue(new QName("", "codeContext")).getBytes());
            }
        }
    }

    private List<ClassCode> getDocumentEntryClassCodes(final AdhocQueryRequest request) {
        final List<ClassCode> classCodes = new ArrayList<>();
        for (final SlotType1 slotType1 : request.getAdhocQuery().getSlot()) {
            if (slotType1.getName().equals("$XDSDocumentEntryClassCode")) {
                var fullClassCodeString = slotType1.getValueList().getValue().get(0);
                final var pattern = "\\(?\\)?\\'?";
                fullClassCodeString = fullClassCodeString.replaceAll(pattern, "");
                final String[] classCodeString = fullClassCodeString.split(",");
                for (String classCode : classCodeString) {
                    classCode = classCode.substring(0, classCode.indexOf("^^"));
                    classCodes.add(ClassCode.getByCode(classCode));
                }
            }
        }
        return classCodes;
    }

    private ClassCode getClassCode(final List<ClassCode> classCodeList) {

        for (final ClassCode classCode : classCodeList) {
            for (final ClassCode classCodeValue : ClassCode.values()) {
                if (classCode.equals(classCodeValue)) {
                    return classCode;
                }
            }
        }
        return null;
    }

    /**
     * Util method extracting the XDS Patient Identifiers from the XCA query.
     *
     * @return List of HL7v2 Patient Identifier formatted Strings.
     */
    private List<String> getDocumentEntryPatientId(final AdhocQueryRequest request) {

        final List<String> patientIds = new ArrayList<>();
        for (final SlotType1 slot : request.getAdhocQuery().getSlot()) {
            if (slot.getName().equals("$XDSDocumentEntryPatientId")) {
                String patientId = slot.getValueList().getValue().get(0);
                patientId = patientId.substring(1, patientId.length() - 1);
                patientIds.add(patientId);
            }
        }
        return patientIds;
    }

    private FilterParams getFilterParams(final AdhocQueryRequest request) {

        final var filterParams = new FilterParams();

        for (final SlotType1 slotType : request.getAdhocQuery().getSlot()) {
            switch (slotType.getName()) {
                case XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERCREATEDAFTER_SLOT_NAME:
                    filterParams.setCreatedAfter(Instant.parse(slotType.getValueList().getValue().get(0)));
                    break;
                case XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERCREATEDBEFORE_SLOT_NAME:
                    filterParams.setCreatedBefore(Instant.parse(slotType.getValueList().getValue().get(0)));
                    break;
                case XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERMAXIMUMSIZE_SLOT_NAME:
                    filterParams.setMaximumSize(Long.parseLong(slotType.getValueList().getValue().get(0)));
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
    private String getRepositoryUniqueId(final RetrieveDocumentSetRequestType request) {

        return request.getDocumentRequest().get(0).getRepositoryUniqueId();
    }

    private AssociationType1 makeAssociation(final String source, final String target) {

        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final var association = ofRim.createAssociationType1();
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
     * Main part of the XCA query operation implementation, fills the AdhocQueryResponse with details
     */
    private void adhocQueryResponseBuilder(final AdhocQueryRequest request, final AdhocQueryResponse response, final SOAPHeader soapHeader, final EventLog eventLog)
            throws Exception {

        String sigCountryCode = null;
        Element shElement = null;
        String responseStatus = AdhocQueryResponseStatus.FAILURE;
        // What's being requested: eP or PS?
        final List<ClassCode> classCodeValues = getDocumentEntryClassCodes(request);
        final var registryErrorList = ofRs.createRegistryErrorList();
        // Create Registry Object List
        response.setRegistryObjectList(ofRim.createRegistryObjectListType());

        try {
            shElement = XMLUtils.toDOM(soapHeader);
            documentSearchService.setSOAPHeader(shElement);
            sigCountryCode = saml2Validator.validateXCAHeader(shElement, getClassCode(classCodeValues));
        } catch (InsufficientRightsException ire) {
            logger.error(ire.getMessage(), ire);
            RegistryErrorUtils.addErrorMessage(registryErrorList, ire.getErrorCode(), ire.getMessage(), ire, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            return;
        } catch (final SMgrException e) {
            logger.error(e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_SEC_GENERIC, e.getMessage(), e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (final InvalidFieldException e) {
            logger.error(e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_PS_INCORRECT_FORMATTING, e.getMessage(), e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (final MissingFieldException e) {
            logger.error(e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_PS_MISSING_REQUIRED_FIELDS, e.getMessage(), e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (final OpenNCPErrorCodeException e) {
            logger.error(e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(registryErrorList, e.getErrorCode(), e.getMessage(), e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (final Exception e) {
            OpenNCPErrorCode code = OpenNCPErrorCode.ERROR_GENERIC;
            switch (getClassCode(classCodeValues)) {
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

        final String fullPatientId = SoapElementHelper.getDocumentEntryPatientIdFromTRCAssertion(shElement);
        if (!getDocumentEntryPatientId(request).contains(fullPatientId)) {
            // Patient ID in TRC assertion does not match the one given in the request. Return "No documents found".
            OpenNCPErrorCode code = OpenNCPErrorCode.ERROR_DOCUMENT_NOT_FOUND;
            switch (getClassCode(classCodeValues)) {
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
            RegistryErrorUtils.addErrorMessage(registryErrorList, code, code.getDescription(), null, RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
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
            if (sigCountryCode != null) {
                logger.info("Found the client country code via the signature certificate.");
                countryCode = sigCountryCode;
            }
        }
        logger.info("The client country code to be used by the PDP: '{}'", countryCode);

        // Then, it is the Policy Decision Point (PDP) that decides according to the consent of the patient
        if (!saml2Validator.isConsentGiven(fullPatientId, countryCode)) {
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_PS_NO_CONSENT,
                    OpenNCPErrorCode.ERROR_PS_NO_CONSENT.getDescription(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        if (classCodeValues.isEmpty()) {
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN,
                    OpenNCPErrorCode.ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN.getDescription(),
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        // Evidence for call to NI for XCA List
        try {
            //  e-Sens: we MUST generate NRO when NCPA sends to NI. This was throwing errors because we were not
            //  passing an XML document. We're passing data like:"SearchCriteria: {patientId = 12445ASD}".
            //  So we provided a XML representation of such data.
            final Assertion assertionTRC = SoapElementHelper.getTRCAssertion(shElement);
            final String messageUUID = UUIDHelper.encodeAsURN(assertionTRC.getID()) + "_" + assertionTRC.getIssueInstant();

            EvidenceUtils.createEvidenceREMNRO(DocumentFactory.createSearchCriteria().addPatientId(fullPatientId).asXml(),
                    Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                    Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD,
                    Constants.SP_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                    Constants.NCP_SIG_PRIVATEKEY_ALIAS, EventType.PATIENT_SERVICE_LIST.getIheCode(), new DateTime(),
                    EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XCA_LIST_REQ", messageUUID);
        } catch (final Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_SEC_GENERIC,
                    OpenNCPErrorCode.ERROR_SEC_GENERIC.getDescription(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        for (final ClassCode classCodeValue : classCodeValues) {
            try {
                switch (classCodeValue) {
                    case EP_CLASSCODE:
                        final List<DocumentAssociation<EPDocumentMetaData>> prescriptions = documentSearchService.getEPDocumentList(
                                DocumentFactory.createSearchCriteria().addPatientId(fullPatientId));

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
                                final var eotXML = ofRim.createExtrinsicObjectType();
                                xmlUUID = EPExtrinsicObjectBuilder.build(request, eotXML, prescription.getXMLDocumentMetaData());
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotXML));

                                final String pdfUUID;
                                final var eotPDF = ofRim.createExtrinsicObjectType();
                                pdfUUID = EPExtrinsicObjectBuilder.build(request, eotPDF, prescription.getPDFDocumentMetaData());
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotPDF));

                                if (StringUtils.isNotBlank(xmlUUID) && StringUtils.isNotBlank(pdfUUID)) {
                                    response.getRegistryObjectList()
                                            .getIdentifiable()
                                            .add(ofRim.createAssociation(makeAssociation(pdfUUID, xmlUUID)));
                                }
                            }
                        }
                        break;
                    case PS_CLASSCODE:
                        final DocumentAssociation<PSDocumentMetaData> psDoc = documentSearchService.getPSDocumentList(
                                DocumentFactory.createSearchCriteria().addPatientId(fullPatientId));

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
                                final var eotXML = ofRim.createExtrinsicObjectType();
                                xmlUUID = PSExtrinsicObjectBuilder.build(request, eotXML, docXml, false);
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotXML));
                            }
                            var pdfUUID = "";
                            if (docPdf != null) {
                                final var eotPDF = ofRim.createExtrinsicObjectType();
                                pdfUUID = PSExtrinsicObjectBuilder.build(request, eotPDF, docPdf, true);
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotPDF));
                            }
                            if (!xmlUUID.isEmpty() && !pdfUUID.isEmpty()) {
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createAssociation(makeAssociation(pdfUUID, xmlUUID)));
                            }
                        }
                        break;
                    case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                    case ORCD_LABORATORY_RESULTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGES_CLASSCODE:
                        final var searchCriteria = DocumentFactory.createSearchCriteria().addPatientId(fullPatientId);
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

                        final List<OrCDDocumentMetaData> orCDDocumentMetaDataList = getOrCDDocumentMetaDataList(classCodeValue, searchCriteria);

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
                                buildOrCDExtrinsicObject(request, response, orCDDocumentMetaData);
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
            } finally {
                try {
                    if (!registryErrorList.getRegistryError().isEmpty()) {
                        response.setRegistryErrorList(registryErrorList);
                    }
                    prepareEventLogForQuery(eventLog, request, response, shElement, classCodeValue);
                } catch (final Exception e) {
                    logger.error("Prepare Audit log failed: '{}'", e.getMessage(), e);
                    // Is this fatal?
                }
            }
        }

        if (!registryErrorList.getRegistryError().isEmpty()) {
            response.setRegistryErrorList(registryErrorList);
        }
        response.setStatus(responseStatus);
    }

    private List<OrCDDocumentMetaData> getOrCDDocumentMetaDataList(final ClassCode classCode, final SearchCriteria searchCriteria)
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

        final var eotXML = ofRim.createExtrinsicObjectType();
        final String xmlUUID = OrCDExtrinsicObjectBuilder.build(request, eotXML, orCDDocumentMetaData);
        response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotXML));
        if (!StringUtils.isEmpty(xmlUUID)) {
            response.getRegistryObjectList().getIdentifiable().add(ofRim.createAssociation(makeAssociation(xmlUUID, xmlUUID)));
        }
    }

    private Document transformDocument(final Document doc, final OMElement registryErrorList, final OMElement registryResponseElement, final boolean isTranscode,
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

            final OMNamespace ns = registryResponseElement.getNamespace();
            final var ons = omFactory.createOMNamespace(ns.getNamespaceURI(), "a");

            for (final ITMTSAMError error : tmResponse.getErrors()) {
                RegistryErrorUtils.addErrorOMMessage(ons, registryErrorList, error, operationType, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            }

            for (final ITMTSAMError warning : tmResponse.getWarnings()) {
                RegistryErrorUtils.addErrorOMMessage(ons, registryErrorList, warning, operationType, RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
            }

            returnDoc = Base64Util.decode(tmResponse.getResponseCDA());
            if (registryErrorList.getChildElements().hasNext()) {
                registryResponseElement.addChild(registryErrorList);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
        return returnDoc;
    }

    private void retrieveDocumentSetBuilder(final RetrieveDocumentSetRequestType request, final SOAPHeader soapHeader, final EventLog eventLog, final OMElement omElement)
            throws Exception {

        final var omNamespace = omFactory.createOMNamespace("urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", "");
        final var registryResponse = omFactory.createOMElement("RegistryResponse", omNamespace);
        final var registryErrorList = omFactory.createOMElement("RegistryErrorList", omNamespace);
        final OMNamespace ns2 = omElement.getNamespace();
        final var documentResponse = omFactory.createOMElement("DocumentResponse", ns2);

        var documentReturned = false;
        var failure = false;

        final Element soapHeaderElement;
        ClassCode classCodeValue = null;

        // Start processing within a labeled block, break on certain errors
        processLabel:
        {
            try {
                soapHeaderElement = XMLUtils.toDOM(soapHeader);
            } catch (final Exception e) {
                logger.error(null, e);
                throw e;
            }

            documentSearchService.setSOAPHeader(soapHeaderElement);

            final String documentId = request.getDocumentRequest().get(0).getDocumentUniqueId();
            final String fullPatientId = SoapElementHelper.getDocumentEntryPatientIdFromTRCAssertion(soapHeaderElement);
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
                countryCode = saml2Validator.getCountryCodeFromHCPAssertion(soapHeaderElement);
                if (countryCode != null) {
                    logger.info("Found the client country code via the signature certificate.");
                } else {
                    failure = true;
                    RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS,
                            OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS.getDescription(),
                            RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                    break processLabel;
                }
            }

            logger.info("The client country code to be used by the PDP '{}' ", countryCode);

            // Then, it is the Policy Decision Point (PDP) that decides according to the consent of the patient
            if (!saml2Validator.isConsentGiven(fullPatientId, countryCode)) {
                failure = true;
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, OpenNCPErrorCode.ERROR_PS_NO_CONSENT,
                        OpenNCPErrorCode.ERROR_PS_NO_CONSENT.getDescription(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            }

            // Evidence for call to NI for XCA Retrieve
            /* Joao: we MUST generate NRO when NCPA sends to NI.This was throwing errors because we were not passing a XML document.
                We're passing data like:
                "SearchCriteria: {patientId = 12445ASD}"
                So we provided an XML representation of such data */
            try {
                EvidenceUtils.createEvidenceREMNRO(DocumentFactory.createSearchCriteria().addPatientId(fullPatientId).asXml(),
                        Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                        Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD,
                        Constants.SP_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH,
                        Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                        EventType.PATIENT_SERVICE_RETRIEVE.getIheCode(), new DateTime(),
                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XCA_RETRIEVE_REQ",
                        SoapElementHelper.getTRCAssertion(soapHeaderElement).getID() + "__" + DateUtil.getCurrentTimeGMT());
            } catch (final Exception e) {
                logger.error("createEvidenceREMNRO: '{}'", ExceptionUtils.getStackTrace(e), e);
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
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, e.getOpenncpErrorCode(), codeContext, e,
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                failure = true;
                break processLabel;
            }

            if (epsosDoc == null) {

                //  Evidence for response from NI in case of failure
                //  This should be NRR of NCPA receiving from NI. This was throwing errors because we were not passing a XML document.
                //  We're passing data like: "SearchCriteria: {patientId = 12445ASD}"
                //  So we provided a XML representation of such data. Still, evidence is generated based on request data, not response.
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
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, IheErrorCode.XDSMissingDocument,
                        OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING.getCode() + " : " +
                                OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING.getDescription(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            }

            // Evidence for response from NI in case of success
            /* Joao: This should be NRR of NCPA receiving from NI.
                    This was throwing errors because we were not passing a XML document.
                    We're passing data like:
                    "SearchCriteria: {patientId = 12445ASD}"
                    So we provided a XML representation of such data. Still, evidence is generated based on request data, not response.
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

            try {
                saml2Validator.validateXCAHeader(soapHeaderElement, classCodeValue);
            } catch (final InvalidFieldException e) {
                logger.error(e.getMessage(), e);
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, OpenNCPErrorCode.ERROR_PS_INCORRECT_FORMATTING, e.getMessage(), e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            } catch (final MissingFieldException e) {
                logger.error(e.getMessage(), e);
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, OpenNCPErrorCode.ERROR_PS_MISSING_REQUIRED_FIELDS, e.getMessage(), e, RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            } catch (final OpenNCPErrorCodeException e) {
                logger.error("OpenncpErrorCodeException: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, e.getErrorCode(), e.getMessage(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            } catch (final SMgrException e) {
                logger.error("SMgrException: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, OpenNCPErrorCode.ERROR_SEC_GENERIC, e.getMessage(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            }

            logger.info("XCA Retrieve Request is valid.");
            final var homeCommunityId = omFactory.createOMElement("HomeCommunityId", ns2);
            homeCommunityId.setText(request.getDocumentRequest().get(0).getHomeCommunityId());
            documentResponse.addChild(homeCommunityId);

            final var repositoryUniqueId = omFactory.createOMElement("RepositoryUniqueId", ns2);
            repositoryUniqueId.setText(request.getDocumentRequest().get(0).getRepositoryUniqueId());
            documentResponse.addChild(repositoryUniqueId);

            final var documentUniqueId = omFactory.createOMElement("DocumentUniqueId", ns2);
            documentUniqueId.setText(documentId);
            documentResponse.addChild(documentUniqueId);

            final var mimeType = omFactory.createOMElement("mimeType", ns2);
            mimeType.setText(MediaType.TEXT_XML_VALUE);
            documentResponse.addChild(mimeType);

            final var document = omFactory.createOMElement("Document", omFactory.createOMNamespace("urn:ihe:iti:xds-b:2007", ""));
            logger.info("XCA Retrieve Response has been created.");
            try {
                Document doc = epsosDoc.getDocument();
                logger.info("Client userID: '{}'", eventLog.getSC_UserID());

                if (doc != null) {
                    logger.info("[National Infrastructure] CDA Document:\n'{}'", epsosDoc.getClassCode().getCode());
                    /* Validate CDA eHDSI Friendly */
                    if (OpenNCPValidation.isValidationEnable()) {

                        OpenNCPValidation.validateCdaDocument(XMLUtil.documentToString(epsosDoc.getDocument()), NcpSide.NCP_A,
                                epsosDoc.getClassCode(), false);
                    }
                    // Transcode to eHDSI Pivot
                    if (!getClassCodesOrCD().contains(classCodeValue)) {
                        doc = transformDocument(doc, registryErrorList, registryResponse, true, eventLog);
                    }
                    if (!checkIfOnlyWarnings(registryErrorList)) {

                        // If the transformation process has raised at least one FATAL Error, we should determine which
                        // XCAError code has to be provided according the corresponding TM Error Code
                        final Iterator<OMElement> errors = registryErrorList.getChildElements();
                        while (errors.hasNext()) {

                            final OMElement errorCode = errors.next();
                            logger.error("Error: '{}'-'{}'", errorCode.getText(), errorCode.getAttributeValue(QName.valueOf("errorCode")));
                            logger.error("TRANSCODING ERROR: '{}'-'{}'", TMError.ERROR_REQUIRED_CODED_ELEMENT_NOT_TRANSCODED.getCode(),
                                    errorCode.getAttributeValue(QName.valueOf("errorCode")));

                            if (StringUtils.startsWith(errorCode.getAttributeValue(QName.valueOf("errorCode")), "45")) {

                                OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.ERROR_TRANSCODING_ERROR;
                                String openNcpErrorCodeDescription = openncpErrorCode.getDescription();
                                final String errorCodeContext = errorCode.getAttributeValue(QName.valueOf("codeContext"));

                                if (Objects.requireNonNull(classCodeValue) == EP_CLASSCODE) {
                                    openncpErrorCode = OpenNCPErrorCode.ERROR_EP_MISSING_EXPECTED_MAPPING;
                                } else if (classCodeValue == PS_CLASSCODE) {
                                    openncpErrorCode = OpenNCPErrorCode.ERROR_PS_MISSING_EXPECTED_MAPPING;
                                }
                                if (StringUtils.isNotBlank(errorCodeContext)) {
                                    openNcpErrorCodeDescription = openncpErrorCode.getDescription() + " [" + errorCodeContext + "]";
                                }

                                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, openncpErrorCode, openNcpErrorCodeDescription,
                                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                                // If the error is FATAL flag failure has been set to true
                                failure = true;
                                break;
                            }
                        }
                    } else {
                        /* Validate CDA eHDSI Pivot if no error during the transformation */
                        if (OpenNCPValidation.isValidationEnable()) {
                            OpenNCPValidation.validateCdaDocument(XMLUtils.toOM(doc.getDocumentElement()).toString(), NcpSide.NCP_A,
                                    epsosDoc.getClassCode(), true);
                        }
                    }
                }

                // If there is no failure during the process, the CDA document has been attached to the response
                logger.info("Error Registry: Failure '{}'", failure);
                if (!failure) {
                    ByteArrayDataSource dataSource = null;
                    if (doc != null) {
                        dataSource = new ByteArrayDataSource(XMLUtils.toOM(doc.getDocumentElement()).toString().getBytes(), "text/xml;charset=UTF-8");
                    }
                    final var dataHandler = new DataHandler(dataSource);
                    final var textData = omFactory.createOMText(dataHandler, true);
                    textData.setOptimize(true);
                    document.addChild(textData);

                    logger.debug("Returning document '{}'", documentId);
                    documentResponse.addChild(document);
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
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList, code, e.getMessage(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            }
        }

        // If the registryErrorList is empty or contains only Warning, the status of the request is SUCCESS
        if (!registryErrorList.getChildElements().hasNext()) {
            logger.info("XCA Retrieve Document - Transformation Status: '{}'\nDefault Case", AdhocQueryResponseStatus.SUCCESS);
            registryResponse.addAttribute(omFactory.createOMAttribute("status", null, AdhocQueryResponseStatus.SUCCESS));
        } else {
            if (checkIfOnlyWarnings(registryErrorList)) {
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nCheck Warning", AdhocQueryResponseStatus.SUCCESS);
                registryResponse.addAttribute(omFactory.createOMAttribute("status", null, AdhocQueryResponseStatus.SUCCESS));
            } else if (failure) {
                // If there is a failure during the request process, the status is FAILURE
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nCheck Warning Failure: '{}'", AdhocQueryResponseStatus.FAILURE,
                        failure);
                registryResponse.addAttribute(omFactory.createOMAttribute("status", null, AdhocQueryResponseStatus.FAILURE));
            } else {
                //  Otherwise the status is PARTIAL SUCCESS
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nOtherwise...", AdhocQueryResponseStatus.PARTIAL_SUCCESS);
                registryResponse.addAttribute(omFactory.createOMAttribute("status", null, AdhocQueryResponseStatus.PARTIAL_SUCCESS));
            }
        }

        logger.info("Preparing Event Log of the Response:");
        try {
            final boolean errorsDiscovered = registryErrorList.getChildElements().hasNext();
            if (errorsDiscovered) {
                registryResponse.addChild(registryErrorList);
            }
            omElement.addChild(registryResponse);
            if (documentReturned) {
                omElement.addChild(documentResponse);
            }
            prepareEventLogForRetrieve(eventLog, request, errorsDiscovered, documentReturned, registryErrorList, soapHeaderElement, classCodeValue);
        } catch (final Exception ex) {
            logger.error("Prepare Audit log failed. '{}'", ex.getMessage(), ex);
            // TODO: TWG to decide if this is this fatal
        }
    }

    /**
     * This method will check if the Registry error list only contains Warnings.
     *
     * @return boolean value, indicating if the list only contains warnings.
     */
    private boolean checkIfOnlyWarnings(final OMElement registryErrorList) {

        var onlyWarnings = true;
        OMElement element;
        final Iterator<OMElement> it = registryErrorList.getChildElements();

        while (it.hasNext()) {
            element = it.next();
            if (StringUtils.equals(element.getAttribute(QName.valueOf("severity")).getAttributeValue(),
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR.getText())) {
                logger.debug("Error has been detected for Element: '{}'", element.getText());
                onlyWarnings = false;
            }
        }
        return onlyWarnings;
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

        final var adhocQueryResponse = ofQuery.createAdhocQueryResponse();
        final var registryErrorList = ofRs.createRegistryErrorList();

        // Create Registry Object List
        adhocQueryResponse.setRegistryObjectList(ofRim.createRegistryObjectListType());

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

    /**
     * XCA list operation implementation, returns the list of patient summaries or ePrescriptions, depending on the query.
     */
    @Override
    public AdhocQueryResponse queryDocument(final AdhocQueryRequest adhocQueryRequest, final SOAPHeader soapHeader, final EventLog eventLog) throws Exception {

        var adhocQueryResponse = ofQuery.createAdhocQueryResponse();
        try {
            adhocQueryResponseBuilder(adhocQueryRequest, adhocQueryResponse, soapHeader, eventLog);
        } catch (final UnsupportedOperationException uoe) {
            adhocQueryResponse = handleUnsupportedOperationException(adhocQueryRequest, uoe);
        }
        return adhocQueryResponse;
    }

    /**
     * XCA retrieve operation implementation, returns the particular document requested by the caller.
     * The response is placed in the OMElement
     */
    @Override
    public void retrieveDocument(final RetrieveDocumentSetRequestType request, final SOAPHeader soapHeader, final EventLog eventLog, final OMElement response)
            throws Exception {

        retrieveDocumentSetBuilder(request, soapHeader, eventLog, response);
    }
}

