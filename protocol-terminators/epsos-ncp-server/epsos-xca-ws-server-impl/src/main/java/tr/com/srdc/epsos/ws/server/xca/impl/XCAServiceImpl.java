package tr.com.srdc.epsos.ws.server.xca.impl;

import epsos.ccd.gnomon.auditmanager.*;
import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import epsos.ccd.posam.tm.response.TMResponseStructure;
import epsos.ccd.posam.tm.service.ITransformationService;
import eu.epsos.protocolterminators.ws.server.exception.NIException;
import eu.epsos.protocolterminators.ws.server.xca.DocumentSearchInterface;
import eu.epsos.protocolterminators.ws.server.xca.XCAServiceInterface;
import eu.epsos.util.EvidenceUtils;
import eu.epsos.util.xca.XCAConstants;
import eu.epsos.util.xdr.XDRConstants;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.constant.ClassCode;
import eu.europa.ec.sante.ehdsi.constant.error.ITMTSAMError;
import eu.europa.ec.sante.ehdsi.constant.error.IheErrorCode;
import eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode;
import eu.europa.ec.sante.ehdsi.constant.error.TMError;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.Helper;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.exceptions.OpenNCPErrorCodeException;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.saml.SAML2Validator;
import eu.europa.ec.sante.ehdsi.openncp.pt.common.AdhocQueryResponseStatus;
import eu.europa.ec.sante.ehdsi.openncp.pt.common.RegistryErrorSeverity;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import eu.europa.ec.sante.ehdsi.openncp.util.UUIDHelper;
import fi.kela.se.epsos.data.model.*;
import fi.kela.se.epsos.data.model.SearchCriteria.Criteria;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tr.com.srdc.epsos.data.model.FilterParams;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.DateUtil;
import tr.com.srdc.epsos.util.XMLUtil;
import tr.com.srdc.epsos.util.http.HTTPUtil;
import tr.com.srdc.epsos.ws.server.xca.impl.errorlist.RegistryErrorUtils;
import tr.com.srdc.epsos.ws.server.xca.impl.extrinsicobjectbuilder.ep.EPExtrinsicObjectBuilder;
import tr.com.srdc.epsos.ws.server.xca.impl.extrinsicobjectbuilder.orcd.OrCDExtrinsicObjectBuilder;
import tr.com.srdc.epsos.ws.server.xca.impl.extrinsicobjectbuilder.ps.PSExtrinsicObjectBuilder;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.*;

import static eu.europa.ec.sante.ehdsi.constant.ClassCode.*;

public class XCAServiceImpl implements XCAServiceInterface {

    private static final DatatypeFactory DATATYPE_FACTORY;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(XCAServiceImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private final ITransformationService transformationService;
    private final OMFactory omFactory;
    private final oasis.names.tc.ebxml_regrep.xsd.query._3.ObjectFactory ofQuery;
    private final oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory ofRim;
    private final oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory ofRs;
    private final DocumentSearchInterface documentSearchService;

    /**
     * Public Constructor for IHE XCA Profile implementation, the default constructor will handle the loading of
     * the National Connector implementation by using the <class>ServiceLoader</class>
     *
     * @see ServiceLoader
     */
    public XCAServiceImpl() {

        ServiceLoader<DocumentSearchInterface> serviceLoader = ServiceLoader.load(DocumentSearchInterface.class);
        try {
            logger.info("Loading National implementation of DocumentSearchInterface...");
            documentSearchService = serviceLoader.iterator().next();
            logger.info("Successfully loaded documentSearchService");
        } catch (Exception e) {
            logger.error("Failed to load implementation of DocumentSearchInterface: " + e.getMessage(), e);
            throw e;
        }

        ofQuery = new oasis.names.tc.ebxml_regrep.xsd.query._3.ObjectFactory();
        ofRs = new oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory();
        ofRim = new oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory();

        omFactory = OMAbstractFactory.getOMFactory();

        var applicationContext = new ClassPathXmlApplicationContext("ctx_tm.xml");
        transformationService = (ITransformationService) applicationContext.getBean(ITransformationService.class.getName());
    }

    private static String trimDocumentEntryPatientId(String patientId) {

        if (patientId.contains("^^^")) {
            return patientId.substring(0, patientId.indexOf("^^^"));
        }
        return patientId;
    }

    public static List<ClassCode> getClassCodesOrCD() {
        List<ClassCode> list = new ArrayList<>();
        list.add(ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE);
        list.add(ORCD_LABORATORY_RESULTS_CLASSCODE);
        list.add(ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE);
        list.add(ORCD_MEDICAL_IMAGES_CLASSCODE);
        return list;
    }

    private void prepareEventLogForQuery(EventLog eventLog, AdhocQueryRequest request, AdhocQueryResponse response,
                                         Element sh, ClassCode classCode) {

        logger.info("method prepareEventLogForQuery(Request: '{}', ClassCode: '{}')", request.getId(), classCode);

        switch (classCode) {
            case EP_CLASSCODE:
                eventLog.setEventType(EventType.ORDER_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.ORDER_SERVICE_LIST);
                break;
            case PS_CLASSCODE:
                eventLog.setEventType(EventType.PATIENT_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.PATIENT_SERVICE_LIST);
                break;
            case MRO_CLASSCODE:
                eventLog.setEventType(EventType.MRO_LIST);
                eventLog.setEI_TransactionName(TransactionName.MRO_SERVICE_LIST);
                break;
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
                eventLog.setEventType(EventType.ORCD_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.ORCD_SERVICE_LIST);
                break;
            default:
                logger.warn("No event identification information found!");
                //  TODO: Analyzing if some specific codes are needed in this situation
                break;
        }
        eventLog.setEI_EventActionCode(EventActionCode.READ);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        eventLog.setPS_ParticipantObjectID(getDocumentEntryPatientId(request));

        if (response.getRegistryObjectList() != null) {
            List<String> documentIds = new ArrayList<>();
            for (var i = 0; i < response.getRegistryObjectList().getIdentifiable().size(); i++) {
                if (!(response.getRegistryObjectList().getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                ExtrinsicObjectType eot = (ExtrinsicObjectType) response.getRegistryObjectList().getIdentifiable().get(i).getValue();
                for (ExternalIdentifierType externalIdentifierType : eot.getExternalIdentifier()) {
                    if (externalIdentifierType.getIdentificationScheme().equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME)) {
                        documentIds.add(externalIdentifierType.getValue());
                    }
                }
            }
            eventLog.setEventTargetParticipantObjectIds(documentIds);
        }

        // Set the operation status to the response
        handleEventLogStatus(eventLog, response, request);

        String userIdAlias = Helper.getAssertionsSPProvidedId(sh);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "" + "<" + Helper.getUserID(sh)
                + "@" + Helper.getAssertionsIssuer(sh) + ">");
        eventLog.setHR_AlternativeUserID(Helper.getAlternateUserID(sh));
        eventLog.setHR_RoleID(Helper.getRoleID(sh));
        eventLog.setSP_UserID(HTTPUtil.getSubjectDN(true));
        eventLog.setPT_ParticipantObjectID(getDocumentEntryPatientId(request));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (response.getRegistryErrorList() != null) {
            RegistryError registryError = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_ParticipantObjectID(registryError.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(registryError.getCodeContext().getBytes());
        }
    }

    private void handleEventLogStatus(EventLog eventLog, AdhocQueryResponse queryResponse, AdhocQueryRequest queryRequest) {

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

    private void addDocumentClassCodeToEventLog(EventLog eventLog, AdhocQueryRequest queryRequest) {

        for (SlotType1 slotType1 : queryRequest.getAdhocQuery().getSlot()) {
            if (StringUtils.equals(slotType1.getName(), "$XDSDocumentEntryClassCode")) {
                String documentType = slotType1.getValueList().getValue().get(0);
                documentType = StringUtils.remove(documentType, "('");
                documentType = StringUtils.remove(documentType, "')");
                eventLog.getEventTargetParticipantObjectIds().add(documentType);
            }
        }
    }

    private void prepareEventLogForRetrieve(EventLog eventLog, RetrieveDocumentSetRequestType request, boolean errorsDiscovered,
                                            boolean documentReturned, OMElement registryErrorList, Element sh, ClassCode classCode) {

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
                    break;
                case PS_CLASSCODE:
                    eventLog.setEventType(EventType.PATIENT_SERVICE_RETRIEVE);
                    eventLog.setEI_TransactionName(TransactionName.PATIENT_SERVICE_RETRIEVE);
                    break;
                case MRO_CLASSCODE:
                    eventLog.setEventType(EventType.MRO_RETRIEVE);
                    eventLog.setEI_TransactionName(TransactionName.MRO_SERVICE_RETRIEVE);
                    break;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    eventLog.setEventType(EventType.ORCD_SERVICE_RETRIEVE);
                    eventLog.setEI_TransactionName(TransactionName.ORCD_SERVICE_RETRIEVE);
                    break;
                default:
                    logger.warn("No event identification information found!");
                    //  TODO: Analyzing if some specific codes are needed in this situation
                    break;
            }
        }
        eventLog.setEI_EventActionCode(EventActionCode.READ);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        eventLog.getEventTargetParticipantObjectIds().add(request.getDocumentRequest().get(0).getDocumentUniqueId());

        if (!documentReturned) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
        } else if (!errorsDiscovered) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
        }

        String userIdAlias = Helper.getAssertionsSPProvidedId(sh);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "" + "<" + Helper.getUserID(sh)
                + "@" + Helper.getAssertionsIssuer(sh) + ">");
        eventLog.setHR_AlternativeUserID(Helper.getAlternateUserID(sh));
        eventLog.setHR_RoleID(Helper.getRoleID(sh));
        eventLog.setSP_UserID(HTTPUtil.getSubjectDN(true));
        eventLog.setPT_ParticipantObjectID(Helper.getDocumentEntryPatientIdFromTRCAssertion(sh));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (errorsDiscovered) {
            Iterator<OMElement> re = registryErrorList.getChildElements();
            //Include only the first error in the audit log.
            if (re.hasNext()) {
                OMElement error = re.next();
                if (logger.isDebugEnabled()) {
                    try {
                        logger.debug("Error to be included in audit: '{}'", XMLUtil.prettyPrint(XMLUtils.toDOM(error)));
                    } catch (Exception e) {
                        logger.debug("Exception: '{}'", e.getMessage(), e);
                    }
                }
                eventLog.setEM_ParticipantObjectID(error.getAttributeValue(new QName("", "errorCode")));
                eventLog.setEM_ParticipantObjectDetail(error.getAttributeValue(new QName("", "codeContext")).getBytes());
            }
        }
    }

    private List<ClassCode> getDocumentEntryClassCodes(AdhocQueryRequest request) {
        List<ClassCode> classCodes = new ArrayList<>();
        for (SlotType1 slotType1 : request.getAdhocQuery().getSlot()) {
            if (slotType1.getName().equals("$XDSDocumentEntryClassCode")) {
                var fullClassCodeString = slotType1.getValueList().getValue().get(0);
                var pattern = "\\(?\\)?\\'?";
                fullClassCodeString = fullClassCodeString.replaceAll(pattern, "");
                String[] classCodeString = fullClassCodeString.split(",");
                for (String classCode : classCodeString) {
                    classCode = classCode.substring(0, classCode.indexOf("^^"));
                    classCodes.add(ClassCode.getByCode(classCode));
                }
            }
        }
        return classCodes;
    }

    private ClassCode getClassCode(List<ClassCode> classCodeList) {

        for (ClassCode classCode : classCodeList) {
            for (ClassCode classCodeValue : ClassCode.values()) {
                if (classCode.equals(classCodeValue)) {
                    return classCode;
                }
            }
        }
        return null;
    }

    /**
     * Util method extracting the XDS Patient Identifier from the XCA query.
     *
     * @return HL7v2 Patient Identifier formatted String.
     */
    private String getDocumentEntryPatientId(AdhocQueryRequest request) {

        for (SlotType1 slot : request.getAdhocQuery().getSlot()) {
            if (slot.getName().equals("$XDSDocumentEntryPatientId")) {
                String patientId = slot.getValueList().getValue().get(0);
                patientId = patientId.substring(1, patientId.length() - 1);
                return patientId;
            }
        }
        return "$XDSDocumentEntryPatientId Not Found!";
    }

    private FilterParams getFilterParams(AdhocQueryRequest request) {

        var filterParams = new FilterParams();

        for (SlotType1 slotType : request.getAdhocQuery().getSlot()) {
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
    private String getRepositoryUniqueId(RetrieveDocumentSetRequestType request) {

        return request.getDocumentRequest().get(0).getRepositoryUniqueId();
    }

    private ExternalIdentifierType makeExternalIdentifier(String identificationScheme, String registryObject, String value, String name) {

        String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        var externalIdentifierType = ofRim.createExternalIdentifierType();
        externalIdentifierType.setId(uuid);
        externalIdentifierType.setIdentificationScheme(identificationScheme);
        externalIdentifierType.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:ExternalIdentifier");
        externalIdentifierType.setRegistryObject(registryObject);
        externalIdentifierType.setValue(value);

        externalIdentifierType.setName(ofRim.createInternationalStringType());
        externalIdentifierType.getName().getLocalizedString().add(ofRim.createLocalizedStringType());
        externalIdentifierType.getName().getLocalizedString().get(0).setValue(name);
        return externalIdentifierType;
    }

    private AssociationType1 makeAssociation(String source, String target) {

        String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        var association = ofRim.createAssociationType1();
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
    private void adhocQueryResponseBuilder(AdhocQueryRequest request, AdhocQueryResponse response, SOAPHeader soapHeader,
                                           EventLog eventLog) throws Exception {

        String sigCountryCode = null;
        Element shElement = null;
        String responseStatus = AdhocQueryResponseStatus.FAILURE;
        // What's being requested: eP or PS?
        List<ClassCode> classCodeValues = getDocumentEntryClassCodes(request);
        var registryErrorList = ofRs.createRegistryErrorList();
        // Create Registry Object List
        response.setRegistryObjectList(ofRim.createRegistryObjectListType());

        try {
            shElement = XMLUtils.toDOM(soapHeader);
            documentSearchService.setSOAPHeader(shElement);
            sigCountryCode = SAML2Validator.validateXCAHeader(shElement, getClassCode(classCodeValues));
        } catch (OpenNCPErrorCodeException e) {
            logger.error(e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(registryErrorList, e.getErrorCode(), e.getMessage(), "", RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (Exception e) {
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
            RegistryErrorUtils.addErrorMessage(registryErrorList, code, e.getMessage(), "", RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            throw e;
        }

        String fullPatientId = Helper.getDocumentEntryPatientIdFromTRCAssertion(shElement);
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
            RegistryErrorUtils.addErrorMessage(registryErrorList, code, code.getDescription(), "", RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
        }

        var countryCode = "";
        String distinguishedName = eventLog.getSC_UserID();
        int cIndex = distinguishedName.indexOf("C=");

        if (cIndex > 0) {
            countryCode = distinguishedName.substring(cIndex + 2, cIndex + 4);
        }
        // This part is added for handling consents when the call is not https.
        // In this case, we check the country code of the signature certificate that ships within the HCP assertion
        // TODO: Might be necessary to remove later, although it does no harm in reality!
        else {
            logger.info("Could not get client country code from the service consumer certificate. " +
                    "The reason can be that the call was not via HTTPS. " +
                    "Will check the country code from the signature certificate now.");
            if (sigCountryCode != null) {
                logger.info("Found the client country code via the signature certificate.");
                countryCode = sigCountryCode;
            }
        }
        logger.info("The client country code to be used by the PDP: '{}'", countryCode);

        // Then, it is the Policy Decision Point (PDP) that decides according to the consent of the patient
        if (!SAML2Validator.isConsentGiven(fullPatientId, countryCode)) {
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_NO_CONSENT,
                    OpenNCPErrorCode.ERROR_NO_CONSENT.getDescription(), "", RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        if (classCodeValues.isEmpty()) {
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN,
                    OpenNCPErrorCode.ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN.getDescription(), "", RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        // Evidence for call to NI for XCA List
        try {
            //  e-Sens: we MUST generate NRO when NCPA sends to NI. This was throwing errors because we were not
            //  passing an XML document. We're passing data like:"SearchCriteria: {patientId = 12445ASD}".
            //  So we provided a XML representation of such data.
            Assertion assertionTRC = Helper.getTRCAssertion(shElement);
            String messageUUID = UUIDHelper.encodeAsURN(assertionTRC.getID()) + "_" + assertionTRC.getIssueInstant();

            EvidenceUtils.createEvidenceREMNRO(DocumentFactory.createSearchCriteria().addPatientId(fullPatientId).asXml(),
                    Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                    Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD, Constants.SP_PRIVATEKEY_ALIAS,
                    Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                    IHEEventType.PATIENT_SERVICE_LIST.getCode(), new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                    "NI_XCA_LIST_REQ", messageUUID);
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_SEC_GENERIC,
                    OpenNCPErrorCode.ERROR_SEC_GENERIC.getDescription(), "", RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        for (ClassCode classCodeValue : classCodeValues) {
            try {
                switch (classCodeValue) {
                    case EP_CLASSCODE:
                        List<DocumentAssociation<EPDocumentMetaData>> prescriptions = documentSearchService.getEPDocumentList(
                                DocumentFactory.createSearchCriteria().addPatientId(fullPatientId));

                        if (prescriptions == null) {
                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_EP_REGISTRY_NOT_ACCESSIBLE,
                                    OpenNCPErrorCode.ERROR_EP_REGISTRY_NOT_ACCESSIBLE.getDescription(), "", RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.FAILURE;
                        } else if (prescriptions.isEmpty()) {
                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_EP_NOT_FOUND,
                                    OpenNCPErrorCode.ERROR_EP_NOT_FOUND.getDescription(), "", RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.FAILURE;
                        } else {
                            // Multiple prescriptions mean multiple PDF and XML files, multiple ExtrinsicObjects and associations
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                            for (DocumentAssociation<EPDocumentMetaData> prescription : prescriptions) {

                                logger.debug("Prescription Repository ID: '{}'", prescription.getXMLDocumentMetaData().getRepositoryId());
                                String xmlUUID;
                                var eotXML = ofRim.createExtrinsicObjectType();
                                xmlUUID = EPExtrinsicObjectBuilder.build(request, eotXML, prescription.getXMLDocumentMetaData());
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotXML));

                                String pdfUUID;
                                var eotPDF = ofRim.createExtrinsicObjectType();
                                pdfUUID = EPExtrinsicObjectBuilder.build(request, eotPDF, prescription.getPDFDocumentMetaData());
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotPDF));

                                if (StringUtils.isNotBlank(xmlUUID) && StringUtils.isNotBlank(pdfUUID)) {
                                    response.getRegistryObjectList().getIdentifiable().add(ofRim.createAssociation(makeAssociation(pdfUUID, xmlUUID)));
                                }
                            }
                        }
                        break;
                    case PS_CLASSCODE:
                        DocumentAssociation<PSDocumentMetaData> psDoc = documentSearchService.getPSDocumentList(
                                DocumentFactory.createSearchCriteria().addPatientId(fullPatientId));

                        if (psDoc == null || (psDoc.getPDFDocumentMetaData() == null && psDoc.getXMLDocumentMetaData() == null)) {

                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_PS_NOT_FOUND, "No patient summary is registered for the given patient.", "", RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                        } else {

                            PSDocumentMetaData docPdf = psDoc.getPDFDocumentMetaData();
                            PSDocumentMetaData docXml = psDoc.getXMLDocumentMetaData();
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;

                            var xmlUUID = "";
                            if (docXml != null) {
                                var eotXML = ofRim.createExtrinsicObjectType();
                                xmlUUID = PSExtrinsicObjectBuilder.build(request, eotXML, docXml, false);
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotXML));
                            }
                            var pdfUUID = "";
                            if (docPdf != null) {
                                var eotPDF = ofRim.createExtrinsicObjectType();
                                pdfUUID = PSExtrinsicObjectBuilder.build(request, eotPDF, docPdf, true);
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotPDF));
                            }
                            if (!xmlUUID.equals("") && !pdfUUID.equals("")) {
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createAssociation(makeAssociation(pdfUUID, xmlUUID)));
                            }
                        }
                        break;
                    case MRO_CLASSCODE:
                        DocumentAssociation<MroDocumentMetaData> mro = documentSearchService.getMroDocumentList(
                                DocumentFactory.createSearchCriteria().addPatientId(fullPatientId));

                        if (mro == null || (mro.getPDFDocumentMetaData() == null && mro.getXMLDocumentMetaData() == null)) {

                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_MRO_NO_DATA,
                                    OpenNCPErrorCode.ERROR_MRO_NO_DATA.getDescription(), "", RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                        } else {

                            MroDocumentMetaData docPdf = mro.getPDFDocumentMetaData();
                            MroDocumentMetaData docXml = mro.getXMLDocumentMetaData();

                            responseStatus = AdhocQueryResponseStatus.SUCCESS;

                            var xmlUUID = "";
                            if (docXml != null) {
                                var eotXML = ofRim.createExtrinsicObjectType();
                                xmlUUID = PSExtrinsicObjectBuilder.build(request, eotXML, docXml, false);
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotXML));
                            }
                            var pdfUUID = "";
                            if (docPdf != null) {
                                var eotPDF = ofRim.createExtrinsicObjectType();
                                pdfUUID = PSExtrinsicObjectBuilder.build(request, eotPDF, docPdf, true);
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotPDF));
                            }
                            if (!xmlUUID.equals("") && !pdfUUID.equals("")) {
                                response.getRegistryObjectList().getIdentifiable().add(ofRim.createAssociation(makeAssociation(pdfUUID, xmlUUID)));
                            }
                        }
                        break;
                    case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                    case ORCD_LABORATORY_RESULTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGES_CLASSCODE:
                        var searchCriteria = DocumentFactory.createSearchCriteria().addPatientId(fullPatientId);
                        var filterParams = getFilterParams(request);
                        if (filterParams.getMaximumSize() != null) {
                            searchCriteria.add(Criteria.MAXIMUM_SIZE, filterParams.getMaximumSize().toString());
                        }
                        if (filterParams.getCreatedBefore() != null) {
                            searchCriteria.add(Criteria.CREATED_BEFORE, filterParams.getCreatedBefore().toString());
                        }
                        if (filterParams.getCreatedAfter() != null) {
                            searchCriteria.add(Criteria.CREATED_AFTER, filterParams.getCreatedAfter().toString());
                        }

                        List<OrCDDocumentMetaData> orCDDocumentMetaDataList = getOrCDDocumentMetaDataList(classCodeValue, searchCriteria);

                        if (orCDDocumentMetaDataList == null) {
                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_ORCD_GENERIC,
                                    "orCD registry could not be accessed.", "", RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                            responseStatus = AdhocQueryResponseStatus.FAILURE;
                        } else if (orCDDocumentMetaDataList.isEmpty()) {
                            RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_ORCD_NOT_FOUND,
                                    "There is no original clinical data of the requested type registered for the given patient.", "", RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                        } else {

                            responseStatus = AdhocQueryResponseStatus.SUCCESS;
                            for (OrCDDocumentMetaData orCDDocumentMetaData : orCDDocumentMetaDataList) {
                                logger.debug("OrCD Document Repository ID: '{}'", orCDDocumentMetaData.getRepositoryId());
                                buildOrCDExtrinsicObject(request, response, orCDDocumentMetaData);
                            }
                        }
                        break;

                    default:
                        RegistryErrorUtils.addErrorMessage(registryErrorList, OpenNCPErrorCode.ERROR_GENERIC_SERVICE_SIGNIFIER_UNKNOWN,
                                "Class code not supported for XCA query(" + classCodeValue + ").", "", RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                        responseStatus = AdhocQueryResponseStatus.SUCCESS;
                        break;
                }

                try {
                    prepareEventLogForQuery(eventLog, request, response, shElement, classCodeValue);
                } catch (Exception e) {
                    logger.error("Prepare Audit log failed: '{}'", e.getMessage(), e);
                    // Is this fatal?
                }
            } catch (NIException e) {
                var stackTraceLines = e.getStackTrace();
                RegistryErrorUtils.addErrorMessage(registryErrorList, e.getOpenncpErrorCode(), e.getOpenncpErrorCode().getDescription(),
                        String.valueOf(stackTraceLines[0]), RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                responseStatus = AdhocQueryResponseStatus.FAILURE;
            }
        }

        if (!registryErrorList.getRegistryError().isEmpty()) {
            response.setRegistryErrorList(registryErrorList);
        }
        response.setStatus(responseStatus);
    }

    private List<OrCDDocumentMetaData> getOrCDDocumentMetaDataList(ClassCode classCode, SearchCriteria searchCriteria)
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

    private void buildOrCDExtrinsicObject(AdhocQueryRequest request, AdhocQueryResponse response, OrCDDocumentMetaData orCDDocumentMetaData) {

        var eotXML = ofRim.createExtrinsicObjectType();
        String xmlUUID = OrCDExtrinsicObjectBuilder.build(request, eotXML, orCDDocumentMetaData);
        response.getRegistryObjectList().getIdentifiable().add(ofRim.createExtrinsicObject(eotXML));
        if (!StringUtils.isEmpty(xmlUUID)) {
            response.getRegistryObjectList().getIdentifiable().add(ofRim.createAssociation(makeAssociation(xmlUUID, xmlUUID)));
        }
    }

    private Document transformDocument(Document doc, OMElement registryErrorList, OMElement registryResponseElement,
                                       boolean isTranscode, EventLog eventLog) {

        logger.debug("Transforming document, isTranscode: '{}' - Event Type: '{}'", isTranscode, eventLog.getEventType());
        if (eventLog.getReqM_ParticipantObjectDetail() != null) {
            var requester = new String(eventLog.getReqM_ParticipantObjectDetail());
            if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                loggerClinical.debug("Participant Requester: '{}'", requester);
            }
        }
        if (eventLog.getResM_ParticipantObjectDetail() != null) {
            var responder = new String(eventLog.getResM_ParticipantObjectDetail());
            if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                loggerClinical.debug("Participant Responder: '{}'", responder);
            }
        }

        Document returnDoc;
        try {
            TMResponseStructure tmResponse;
            String operationType;
            if (isTranscode) {
                operationType = "toEpSOSPivot";
                logger.debug("Transforming document to epSOS pivot...");
                tmResponse = transformationService.toEpSOSPivot(doc);
            } else {
                operationType = "translate";
                logger.debug("Translating document to '{}'", Constants.LANGUAGE_CODE);
                tmResponse = transformationService.translate(doc, Constants.LANGUAGE_CODE);
            }

            OMNamespace ns = registryResponseElement.getNamespace();
            var ons = omFactory.createOMNamespace(ns.getNamespaceURI(), "a");

            for (var i = 0; i < tmResponse.getErrors().size(); i++) {
                ITMTSAMError error = tmResponse.getErrors().get(i);
                RegistryErrorUtils.addErrorOMMessage(ons, registryErrorList,
                        error, operationType,
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            }

            for (var i = 0; i < tmResponse.getWarnings().size(); i++) {
                ITMTSAMError error = tmResponse.getWarnings().get(i);
                RegistryErrorUtils.addErrorOMMessage(ons, registryErrorList,
                        error, operationType,
                        RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
            }

            returnDoc = tmResponse.getResponseCDA();
            if (registryErrorList.getChildElements().hasNext()) {
                registryResponseElement.addChild(registryErrorList);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
        return returnDoc;
    }

    private void retrieveDocumentSetBuilder(RetrieveDocumentSetRequestType request, SOAPHeader soapHeader,
                                            EventLog eventLog, OMElement omElement) throws Exception {

        var omNamespace = omFactory.createOMNamespace("urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", "");
        var registryResponse = omFactory.createOMElement("RegistryResponse", omNamespace);
        var registryErrorList = omFactory.createOMElement("RegistryErrorList", omNamespace);
        OMNamespace ns2 = omElement.getNamespace();
        var documentResponse = omFactory.createOMElement("DocumentResponse", ns2);

        var documentReturned = false;
        var failure = false;

        Element soapHeaderElement;
        ClassCode classCodeValue = null;

        // Start processing within a labeled block, break on certain errors
        processLabel:
        {
            try {
                soapHeaderElement = XMLUtils.toDOM(soapHeader);
            } catch (Exception e) {
                logger.error(null, e);
                throw e;
            }

            documentSearchService.setSOAPHeader(soapHeaderElement);

            String documentId = request.getDocumentRequest().get(0).getDocumentUniqueId();
            String fullPatientId = Helper.getDocumentEntryPatientIdFromTRCAssertion(soapHeaderElement);
            String repositoryId = getRepositoryUniqueId(request);
            if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                loggerClinical.debug("Retrieving clinical document by criteria:\nPatient ID: '{}'\nDocument ID: '{}'\nRepository ID: '{}'",
                        fullPatientId, documentId, repositoryId);
            }
            //try getting country code from the certificate
            String countryCode = null;
            String distinguishedName = eventLog.getSC_UserID();
            logger.info("[Certificate] Distinguished Name: '{}'", distinguishedName);
            int cIndex = distinguishedName.indexOf("C=");
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
                countryCode = SAML2Validator.getCountryCodeFromHCPAssertion(soapHeaderElement);
                if (countryCode != null) {
                    logger.info("Found the client country code via the signature certificate.");
                } else {
                    failure = true;
                    RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList,
                            OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS,
                            OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS.getDescription(),
                            RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                    break processLabel;
                }
            }

            logger.info("The client country code to be used by the PDP '{}' ", countryCode);

            // Then, it is the Policy Decision Point (PDP) that decides according to the consent of the patient
            if (!SAML2Validator.isConsentGiven(fullPatientId, countryCode)) {
                failure = true;
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList,
                        OpenNCPErrorCode.ERROR_NO_CONSENT,
                        OpenNCPErrorCode.ERROR_NO_CONSENT.getDescription(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            }

            // Evidence for call to NI for XCA Retrieve
            /* Joao: we MUST generate NRO when NCPA sends to NI.This was throwing errors because we were not passing a XML document.
                We're passing data like:
                "SearchCriteria: {patientId = 12445ASD}"
                So we provided an XML representation of such data */
            try {
                EvidenceUtils.createEvidenceREMNRO(DocumentFactory.createSearchCriteria().addPatientId(fullPatientId).asXml(),
                        Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                        Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD, Constants.SP_PRIVATEKEY_ALIAS,
                        Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                        IHEEventType.PATIENT_SERVICE_RETRIEVE.getCode(), new DateTime(),
                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XCA_RETRIEVE_REQ",
                        Helper.getTRCAssertion(soapHeaderElement).getID() + "__" + DateUtil.getCurrentTimeGMT());
            } catch (Exception e) {
                logger.error("createEvidenceREMNRO: '{}'", ExceptionUtils.getStackTrace(e), e);
            }

            //TODO: EHNCP-1271 - Shall we indicate a specific ERROR Code???
            EPSOSDocument epsosDoc;
            try {
                epsosDoc = documentSearchService.getDocument(DocumentFactory.createSearchCriteria()
                        .add(Criteria.DOCUMENT_ID, documentId)
                        .addPatientId(fullPatientId)
                        .add(Criteria.REPOSITORY_ID, repositoryId));
            } catch (NIException e) {
                logger.error("NIException: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList,
                        e.getOpenncpErrorCode(),
                        e.getOpenncpErrorCode().getDescription(),
                        e.getMessage(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
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
//                    EvidenceUtils.createEvidenceREMNRR(DocumentFactory.createSearchCriteria().add(Criteria.PatientId, patientId).asXml(),
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
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList,
                        IheErrorCode.XDSMissingDocument,
                        OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING.getCode() + " : " + OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING.getDescription(),
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
                SAML2Validator.validateXCAHeader(soapHeaderElement, classCodeValue);
            } catch (OpenNCPErrorCodeException e) {
                logger.error("OpenncpErrorCodeException: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList,
                        e.getErrorCode(),
                        e.getMessage(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            } catch (SMgrException e) {
                logger.error("SMgrException: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList,
                        OpenNCPErrorCode.ERROR_SEC_GENERIC,
                        e.getMessage(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                break processLabel;
            }

            logger.info("XCA Retrieve Request is valid.");
            var homeCommunityId = omFactory.createOMElement("HomeCommunityId", ns2);
            homeCommunityId.setText(request.getDocumentRequest().get(0).getHomeCommunityId());
            documentResponse.addChild(homeCommunityId);

            var repositoryUniqueId = omFactory.createOMElement("RepositoryUniqueId", ns2);
            repositoryUniqueId.setText(request.getDocumentRequest().get(0).getRepositoryUniqueId());
            documentResponse.addChild(repositoryUniqueId);

            var documentUniqueId = omFactory.createOMElement("DocumentUniqueId", ns2);
            documentUniqueId.setText(documentId);
            documentResponse.addChild(documentUniqueId);

            var mimeType = omFactory.createOMElement("mimeType", ns2);
            mimeType.setText(MediaType.TEXT_XML_VALUE);
            documentResponse.addChild(mimeType);

            var document = omFactory.createOMElement("Document", omFactory.createOMNamespace("urn:ihe:iti:xds-b:2007", ""));
            logger.info("XCA Retrieve Response has been created.");
            try {
                Document doc = epsosDoc.getDocument();
                logger.info("Client userID: '{}'", eventLog.getSC_UserID());

                if (doc != null) {
                    logger.info("[National Infrastructure] CDA Document:\n'{}'", epsosDoc.getClassCode().getCode());
                    /* Validate CDA eHDSI Friendly */
                    if (OpenNCPValidation.isValidationEnable()) {

                        OpenNCPValidation.validateCdaDocument(XMLUtil.documentToString(epsosDoc.getDocument()),
                                NcpSide.NCP_A, epsosDoc.getClassCode(), false);
                    }
                    // Transcode to eHDSI Pivot
                    if (!getClassCodesOrCD().contains(classCodeValue)) {
                        doc = transformDocument(doc, registryErrorList, registryResponse, true, eventLog);
                    }
                    if (!checkIfOnlyWarnings(registryErrorList)) {

                        // If the transformation process has raised at least one FATAL Error, we should determine which
                        // XCAError code has to be provided according the corresponding TM Error Code
                        Iterator<OMElement> errors = registryErrorList.getChildElements();
                        while (errors.hasNext()) {

                            OMElement errorCode = errors.next();
                            logger.error("Error: '{}'-'{}'", errorCode.getText(), errorCode.getAttributeValue(QName.valueOf("errorCode")));
                            logger.error("TRANSCODING ERROR: '{}'-'{}'", TMError.ERROR_REQUIRED_CODED_ELEMENT_NOT_TRANSCODED.getCode(),
                                    errorCode.getAttributeValue(QName.valueOf("errorCode")));

                            if (StringUtils.startsWith(errorCode.getAttributeValue(QName.valueOf("errorCode")), "45")) {

                                OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.ERROR_TRANSCODING_ERROR;
                                String openNcpErrorCodeDescription = openncpErrorCode.getDescription();
                                String errorCodeContext = errorCode.getAttributeValue(QName.valueOf("codeContext"));

                                if (Objects.requireNonNull(classCodeValue) == EP_CLASSCODE) {
                                    openncpErrorCode = OpenNCPErrorCode.ERROR_EP_MISSING_EXPECTED_MAPPING;
                                } else if (classCodeValue == PS_CLASSCODE) {
                                    openncpErrorCode = OpenNCPErrorCode.ERROR_PS_MISSING_EXPECTED_MAPPING;
                                }
                                if (StringUtils.isNotBlank(errorCodeContext)) {
                                    openNcpErrorCodeDescription = openncpErrorCode.getDescription() + " [" + errorCodeContext + "]";
                                }

                                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList,
                                        openncpErrorCode,
                                        openNcpErrorCodeDescription,
                                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                                // If the error is FATAL flag failure has been set to true
                                failure = true;
                                break;
                            }
                        }
                    } else {
                        /* Validate CDA eHDSI Pivot if no error during the transformation */
                        if (OpenNCPValidation.isValidationEnable()) {
                            OpenNCPValidation.validateCdaDocument(XMLUtils.toOM(doc.getDocumentElement()).toString(),
                                    NcpSide.NCP_A, epsosDoc.getClassCode(), true);
                        }
                    }
                }

                // If there is no failure during the process, the CDA document has been attached to the response
                logger.info("Error Registry: Failure '{}'", failure);
                if (!failure) {
                    ByteArrayDataSource dataSource = null;
                    if (doc != null) {
                        dataSource = new ByteArrayDataSource(XMLUtils.toOM(doc.getDocumentElement()).toString().getBytes(),
                                "text/xml;charset=UTF-8");
                    }
                    var dataHandler = new DataHandler(dataSource);
                    var textData = omFactory.createOMText(dataHandler, true);
                    textData.setOptimize(true);
                    document.addChild(textData);

                    logger.debug("Returning document '{}'", documentId);
                    documentResponse.addChild(document);
                    documentReturned = true;
                }
            } catch (Exception e) {
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
                RegistryErrorUtils.addErrorOMMessage(omNamespace, registryErrorList,
                        code,
                        e.getMessage(),
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            }
        }

        // If the registryErrorList is empty or contains only Warning, the status of the request is SUCCESS
        if (!registryErrorList.getChildElements().hasNext()) {
            logger.info("XCA Retrieve Document - Transformation Status: '{}'\nDefault Case", AdhocQueryResponseStatus.SUCCESS);
            registryResponse.addAttribute(omFactory.createOMAttribute("status", null,
                    AdhocQueryResponseStatus.SUCCESS));
        } else {
            if (checkIfOnlyWarnings(registryErrorList)) {
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nCheck Warning", AdhocQueryResponseStatus.SUCCESS);
                registryResponse.addAttribute(omFactory.createOMAttribute("status", null,
                        AdhocQueryResponseStatus.SUCCESS));
            } else if (failure) {
                // If there is a failure during the request process, the status is FAILURE
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nCheck Warning Failure: '{}'", AdhocQueryResponseStatus.FAILURE, failure);
                registryResponse.addAttribute(omFactory.createOMAttribute("status", null,
                        AdhocQueryResponseStatus.FAILURE));
            } else {
                //  Otherwise the status is PARTIAL SUCCESS
                logger.info("XCA Retrieve Document - Transformation Status: '{}'\nOtherwise...", AdhocQueryResponseStatus.PARTIAL_SUCCESS);
                registryResponse.addAttribute(omFactory.createOMAttribute("status", null,
                        AdhocQueryResponseStatus.PARTIAL_SUCCESS));
            }
        }

        logger.info("Preparing Event Log of the Response:");
        try {
            boolean errorsDiscovered = registryErrorList.getChildElements().hasNext();
            if (errorsDiscovered) {
                registryResponse.addChild(registryErrorList);
            }
            omElement.addChild(registryResponse);
            if (documentReturned) {
                omElement.addChild(documentResponse);
            }
            prepareEventLogForRetrieve(eventLog, request, errorsDiscovered, documentReturned, registryErrorList,
                    soapHeaderElement, classCodeValue);
        } catch (Exception ex) {
            logger.error("Prepare Audit log failed. '{}'", ex.getMessage(), ex);
            // TODO: TWG to decide if this is this fatal
        }
    }

    /**
     * This method will check if the Registry error list only contains Warnings.
     *
     * @return boolean value, indicating if the list only contains warnings.
     */
    private boolean checkIfOnlyWarnings(OMElement registryErrorList) {

        var onlyWarnings = true;
        OMElement element;
        Iterator<OMElement> it = registryErrorList.getChildElements();

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
    private AdhocQueryResponse handleUnsupportedOperationException(AdhocQueryRequest request, UnsupportedOperationException e) {

        var adhocQueryResponse = ofQuery.createAdhocQueryResponse();
        var registryErrorList = ofRs.createRegistryErrorList();

        // Create Registry Object List
        adhocQueryResponse.setRegistryObjectList(ofRim.createRegistryObjectListType());

        List<ClassCode> classCodeValues = getDocumentEntryClassCodes(request);

        for (ClassCode classCodeValue : classCodeValues) {
            switch (classCodeValue) {
                case EP_CLASSCODE:
                    RegistryErrorUtils.addErrorMessage(registryErrorList,
                            OpenNCPErrorCode.ERROR_EP_NOT_FOUND,
                            "The XDS repository does not contain any ePrescription related to the current patient",
                            "No ePrescriptions are registered for the given patient.",
                            RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                    break;
                case PS_CLASSCODE:
                    RegistryErrorUtils.addErrorMessage(registryErrorList,
                            OpenNCPErrorCode.ERROR_PS_NOT_FOUND,
                            "The XDS repository does not contain any Patient Summary related to the current patient",
                            "No patient summary is registered for the given patient.",
                            RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                    break;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    RegistryErrorUtils.addErrorMessage(registryErrorList,
                            OpenNCPErrorCode.ERROR_ORCD_NOT_FOUND,
                            "The XDS repository does not contain any OrCD of the requested type related to the current patient",
                            "No original clinical document of the requested type is registered for the given patient.",
                            RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                    break;
                default:
                    RegistryErrorUtils.addErrorMessage(registryErrorList,
                            OpenNCPErrorCode.ERROR_DOCUMENT_NOT_FOUND,
                            "The XDS repository does not contain any documents related to the current patient",
                            "No documents are registered for the given patient.",
                            RegistryErrorSeverity.ERROR_SEVERITY_WARNING);
                    break;
            }
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
    public AdhocQueryResponse queryDocument(AdhocQueryRequest adhocQueryRequest, SOAPHeader soapHeader, EventLog eventLog)
            throws Exception {

        var adhocQueryResponse = ofQuery.createAdhocQueryResponse();
        try {
            adhocQueryResponseBuilder(adhocQueryRequest, adhocQueryResponse, soapHeader, eventLog);
        } catch (UnsupportedOperationException uoe) {
            adhocQueryResponse = handleUnsupportedOperationException(adhocQueryRequest, uoe);
        }
        return adhocQueryResponse;
    }

    /**
     * XCA retrieve operation implementation, returns the particular document requested by the caller.
     * The response is placed in the OMElement
     */
    @Override
    public void retrieveDocument(RetrieveDocumentSetRequestType request, SOAPHeader soapHeader, EventLog eventLog,
                                 OMElement response) throws Exception {

        retrieveDocumentSetBuilder(request, soapHeader, eventLog, response);
    }
}
