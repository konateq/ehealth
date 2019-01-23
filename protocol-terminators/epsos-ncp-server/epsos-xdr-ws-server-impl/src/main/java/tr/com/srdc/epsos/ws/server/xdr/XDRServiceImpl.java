package tr.com.srdc.epsos.ws.server.xdr;

import epsos.ccd.gnomon.auditmanager.*;
import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import eu.epsos.exceptions.DocumentTransformationException;
import eu.epsos.protocolterminators.ws.server.exception.NIException;
import eu.epsos.protocolterminators.ws.server.xdr.DocumentProcessingException;
import eu.epsos.protocolterminators.ws.server.xdr.DocumentSubmitInterface;
import eu.epsos.protocolterminators.ws.server.xdr.XDRServiceInterface;
import eu.epsos.pt.transformation.TMServices;
import eu.epsos.util.EvidenceUtils;
import eu.epsos.util.xdr.XDRConstants;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import eu.europa.ec.sante.ehdsi.openncp.pt.common.AdhocQueryResponseStatus;
import fi.kela.se.epsos.data.model.DocumentFactory;
import fi.kela.se.epsos.data.model.EPSOSDocument;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import tr.com.srdc.epsos.securityman.SAML2Validator;
import tr.com.srdc.epsos.securityman.exceptions.AssertionValidationException;
import tr.com.srdc.epsos.securityman.exceptions.InsufficientRightsException;
import tr.com.srdc.epsos.securityman.exceptions.InvalidFieldException;
import tr.com.srdc.epsos.securityman.exceptions.MissingFieldException;
import tr.com.srdc.epsos.securityman.helper.Helper;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.DateUtil;
import tr.com.srdc.epsos.util.XMLUtil;
import tr.com.srdc.epsos.util.http.HTTPUtil;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.ServiceLoader;

public class XDRServiceImpl implements XDRServiceInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(XDRServiceImpl.class);
    private oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory ofRs;
    private ServiceLoader<DocumentSubmitInterface> serviceLoader;
    private DocumentSubmitInterface documentSubmitService;

    public XDRServiceImpl() {

        serviceLoader = ServiceLoader.load(DocumentSubmitInterface.class);
        try {
            LOGGER.info("Loading National implementation of DocumentSubmitInterface...");
            documentSubmitService = serviceLoader.iterator().next();
            LOGGER.info("Successfully loaded documentSubmitService");
        } catch (Exception e) {
            LOGGER.error("Failed to load implementation of documentSubmitService: " + e.getMessage(), e);
            throw e;
        }

        ofRs = new oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory();
    }

    protected XDRServiceImpl(DocumentSubmitInterface dsi, oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory ofRs) {
        this.documentSubmitService = dsi;
        this.ofRs = ofRs;
    }

    private RegistryError createErrorMessage(String errorCode, String codeContext, String value, boolean isWarning) {

        RegistryError re = ofRs.createRegistryError();
        re.setErrorCode(errorCode);
        re.setLocation(getLocation());
        re.setSeverity("urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:" + (isWarning ? "Warning" : "Error"));
        re.setCodeContext(codeContext);
        re.setValue(value);

        return re;
    }

    protected String getLocation() {

        //  Returning HOME COMMUNITY ID insteand of RegisteredService.CONSENT_SERVICE
        return "urn:oid:" + Constants.HOME_COMM_ID;
    }

    /**
     * Prepare audit log for the dispensation service, initialize() operation, i.e. dispensation submit operation
     *
     * @author konstantin.hypponen@kela.fi
     */
    public void prepareEventLogForDispensationInitialize(EventLog eventLog, ProvideAndRegisterDocumentSetRequestType request,
                                                         RegistryResponseType response, Element sh) {

        eventLog.setEventType(EventType.epsosDispensationServiceInitialize);
        eventLog.setEI_TransactionName(TransactionName.epsosDispensationServiceInitialize);
        eventLog.setEI_EventActionCode(EventActionCode.UPDATE);
        try {
            eventLog.setEI_EventDateTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("DatatypeConfigurationException: {}", e.getMessage(), e);
        }
        if (request.getSubmitObjectsRequest().getRegistryObjectList() != null) {

            for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().size(); i++) {
                if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiable().get(i).getValue();
                String documentId = "";
                for (ExternalIdentifierType eit : eot.getExternalIdentifier()) {
                    if (StringUtils.equals(eit.getIdentificationScheme(), XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME)) {
                        documentId = eit.getValue();
                    }
                }
                eventLog.getEventTargetParticipantObjectIds().add(documentId);
                break;
            }
        }

        if (response.getRegistryErrorList() != null) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        }

        String userIdAlias = Helper.getAssertionsSPProvidedId(sh);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : ""
                + "<" + Helper.getUserID(sh) + "@" + Helper.getAssertionsIssuer(sh) + ">");
        eventLog.setHR_AlternativeUserID(Helper.getAlternateUserID(sh));
        eventLog.setHR_RoleID(Helper.getRoleID(sh));
        eventLog.setSP_UserID(HTTPUtil.getSubjectDN(true));
        eventLog.setPT_PatricipantObjectID(getDocumentEntryPatientId(request));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (response.getRegistryErrorList() != null) {
            RegistryError re = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_PatricipantObjectID(re.getErrorCode());
            eventLog.setEM_PatricipantObjectDetail(re.getCodeContext().getBytes());
        }
        LOGGER.debug("Event log prepared");
    }

    /**
     * Prepare audit log for the consent service, put() operation, i.e. consent submission
     *
     * @author konstantin.hypponen@kela.fi TODO: check the audit logs in
     * Gazelle, fix if needed
     */
    public void prepareEventLogForConsentPut(EventLog eventLog, ProvideAndRegisterDocumentSetRequestType request, RegistryResponseType response, Element sh) {

        eventLog.setEventType(EventType.epsosConsentServicePut);
        eventLog.setEI_TransactionName(TransactionName.epsosConsentServicePut);
        eventLog.setEI_EventActionCode(EventActionCode.UPDATE);
        try {
            eventLog.setEI_EventDateTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("DatatypeConfigurationException: {}", e.getMessage(), e);
        }

        if (request.getSubmitObjectsRequest().getRegistryObjectList() != null) {

            for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().size(); i++) {

                if (!(request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().get(i).getValue()
                        instanceof ExtrinsicObjectType)) {
                    continue;
                }
                ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiable().get(i).getValue();
                String documentId = "";
                for (ExternalIdentifierType eit : eot.getExternalIdentifier()) {
                    if (StringUtils.equals(eit.getIdentificationScheme(), XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME)) {
                        documentId = eit.getValue();
                    }
                }
                eventLog.getEventTargetParticipantObjectIds().add(documentId);
                break;
            }
        }

        if (response.getRegistryErrorList() != null) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        }
        String userIdAlias = Helper.getAssertionsSPProvidedId(sh);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : ""
                + "<" + Helper.getUserID(sh) + "@" + Helper.getAssertionsIssuer(sh) + ">");
        eventLog.setHR_AlternativeUserID(Helper.getAlternateUserID(sh));
        eventLog.setHR_RoleID(Helper.getRoleID(sh));

        eventLog.setSP_UserID(HTTPUtil.getSubjectDN(true));

        eventLog.setPT_PatricipantObjectID(getDocumentEntryPatientId(request));

        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (response.getRegistryErrorList() != null) {
            RegistryError re = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_PatricipantObjectID(re.getErrorCode());
            eventLog.setEM_PatricipantObjectDetail(re.getCodeContext().getBytes());
        }

        LOGGER.debug("Event log prepared");
    }

    private String getDocumentEntryPatientId(ProvideAndRegisterDocumentSetRequestType request) {

        String patientId = "";
        // Traverse all ExtrinsicObjects
        for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().size(); i++) {

            if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue();
            // Traverse all Classification blocks in the ExtrinsicObject
            // selected
            for (int j = 0; j < eot.getSlot().size(); j++) {
                // Search for the slot with the name "sourcePatientId"
                if (StringUtils.equals(eot.getSlot().get(j).getName(), "sourcePatientId")) {
                    patientId = eot.getSlot().get(j).getValueList().getValue().get(0);
                    return patientId;
                }
            }
        }
        LOGGER.error("Could not locate the patient id of the XDR request.");
        return patientId;
    }

    private String getPatientId(ProvideAndRegisterDocumentSetRequestType request) {

        String patientId = getDocumentEntryPatientId(request);
        return patientId.substring(0, patientId.indexOf("^^^"));
    }

    /**
     * @param request
     * @param sh
     * @param eventLog
     * @return
     * @throws Exception
     */
    public RegistryResponseType saveDispensation(ProvideAndRegisterDocumentSetRequestType request, SOAPHeader sh,
                                                 EventLog eventLog) throws Exception {

        RegistryResponseType response = new RegistryResponseType();
        String sigCountryCode = null;

        Element shElement;
        try {
            shElement = XMLUtils.toDOM(sh);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
        documentSubmitService.setSOAPHeader(shElement);

        RegistryErrorList rel = ofRs.createRegistryErrorList();
        try {
            sigCountryCode = SAML2Validator.validateXDRHeader(shElement, Constants.ED_CLASSCODE);

        } catch (InsufficientRightsException e) {
            LOGGER.error("InsufficientRightsException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
        } catch (AssertionValidationException e) {
            LOGGER.error("AssertionValidationException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
        } catch (SMgrException e) {
            LOGGER.error("SMgrException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage("", e.getMessage(), "", false));
        }

        //  String documentId = request.getDocument().get(0).getId();
        String patientId = getPatientId(request);
        LOGGER.info("Received an eDispensation document for patient: '{}'", patientId);
        String countryCode = "";
        String DN = eventLog.getSC_UserID();
        int cIndex = DN.indexOf("C=");

        if (cIndex > 0) {
            countryCode = DN.substring(cIndex + 2, cIndex + 4);
        }
        // Mustafa: This part is added for handling consents when the call is not https.
        // In this case, we check the country code of the signature certificate that ships within the HCP assertion.
        // TODO: Might be necessary to remove later, although it does no harm in reality!
        else {
            LOGGER.info("Could not get client country code from the service consumer certificate. " +
                    "The reason can be that the call was not via HTTPS. Will check the country code from the signature certificate now.");
            if (sigCountryCode != null) {
                LOGGER.info("Found the client country code via the signature certificate.");
                countryCode = sigCountryCode;
            }
        }
        LOGGER.info("The client country code to be used by the PDP: '{}'", countryCode);
        if (!SAML2Validator.isConsentGiven(patientId, countryCode)) {
            LOGGER.debug("No consent given, throwing InsufficientRightsException");
            InsufficientRightsException e = new InsufficientRightsException(4701);
            rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
        }
        if (!rel.getRegistryError().isEmpty()) {
            response.setRegistryErrorList(rel);
            response.setStatus(AdhocQueryResponseStatus.FAILURE);
        } else {
            try {
                shElement = XMLUtils.toDOM(sh);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                throw e;
            }

            for (int i = 0; i < request.getDocument().size(); i++) {
                Document doc = request.getDocument().get(i);
                byte[] docBytes = doc.getValue();
                try {

                    /* Validate CDA epSOS Pivot */
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCdaDocument(new String(doc.getValue(), StandardCharsets.UTF_8),
                                NcpSide.NCP_A, obtainClassCode(request), true);
                    }

                    //Resets the response document to a translated version.
                    docBytes = TMServices.transformDocument(docBytes, Constants.LANGUAGE_CODE);

                    /* Validate CDA epSOS Pivot */
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCdaDocument(new String(docBytes, StandardCharsets.UTF_8), NcpSide.NCP_A,
                                obtainClassCode(request), false);
                    }

                } catch (DocumentTransformationException ex) {
                    LOGGER.error(ex.getLocalizedMessage(), ex);
                }
                try {
                    org.w3c.dom.Document domDocument = TMServices.byteToDocument(docBytes);
                    EPSOSDocument epsosDocument = DocumentFactory.createEPSOSDocument(patientId, Constants.ED_CLASSCODE, domDocument);
                    // Evidence for call to NI for XDR submit (dispensation)
                    // Joao: here we have a Document so we can generate the mandatory NRO
                    try {
                        EvidenceUtils.createEvidenceREMNRO(epsosDocument.getDocument(),
                                Constants.NCP_SIG_KEYSTORE_PATH,
                                Constants.NCP_SIG_KEYSTORE_PASSWORD,
                                Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                                Constants.SP_KEYSTORE_PATH,
                                Constants.SP_KEYSTORE_PASSWORD,
                                Constants.SP_PRIVATEKEY_ALIAS,
                                Constants.NCP_SIG_KEYSTORE_PATH,
                                Constants.NCP_SIG_KEYSTORE_PASSWORD,
                                Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                                IHEEventType.epsosDispensationServiceInitialize.getCode(),
                                new DateTime(),
                                EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                                "NI_XDR_DISP_REQ",
                                Objects.requireNonNull(Helper.getTRCAssertion(shElement)).getID() + "__" + DateUtil.getCurrentTimeGMT());
                    } catch (Exception e) {
                        LOGGER.error(ExceptionUtils.getStackTrace(e));
                    }
                    // Call to National Connector
                    documentSubmitService.submitDispensation(epsosDocument);

                    // Evidence for response from NI for XDR submit (dispensation)
                    /* Joao: the NRR is being generated based on the request message (submitted document). The interface for document submission does not return
                    any response for the submit service. This NRR is optional as per the CP. Left commented for now. */
//                    try {
//                        EvidenceUtils.createEvidenceREMNRR(epsosDocument.toString(),
//                                Constants.NCP_SIG_KEYSTORE_PATH,
//                                Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                                Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                                IHEEventType.epsosDispensationServiceInitialize.getCode(),
//                                new DateTime(),
//                                EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                                "NI_XDR_DISP_RES",
//                                Helper.getDocumentEntryPatientIdFromTRCAssertion(shElement) + "__" + DateUtil.getCurrentTimeGMT());
//                    } catch (Exception e) {
//                        LOGGER.error(ExceptionUtils.getStackTrace(e));
//                    }
                } catch (NIException e) {
                    LOGGER.error("NIException: '{}'", e.getMessage(), e);
                    rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
                } catch (Exception e) {
                    LOGGER.error("Exception: '{}'", e.getMessage(), e);
                    rel.getRegistryError().add(createErrorMessage("", e.getMessage(), "", false));
                }
            }
            if (!rel.getRegistryError().isEmpty()) {
                response.setRegistryErrorList(rel);
                response.setStatus(AdhocQueryResponseStatus.FAILURE);
            } else {
                response.setStatus(AdhocQueryResponseStatus.SUCCESS);
            }
        }
        prepareEventLogForDispensationInitialize(eventLog, request, response, shElement);

        return response;
    }

    public RegistryResponseType saveDocument(ProvideAndRegisterDocumentSetRequestType request, SOAPHeader sh,
                                             EventLog eventLog) throws Exception {

        // Traverse all ExtrinsicObjects
        for (int i = 0; i < request.getSubmitObjectsRequest()
                .getRegistryObjectList().getIdentifiable().size(); i++) {
            if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            ExtrinsicObjectType eot = (ExtrinsicObjectType) request
                    .getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue();
            // Traverse all Classification blocks in the ExtrinsicObject
            // selected
            for (int j = 0; j < eot.getClassification().size(); j++) {
                // If classified as eDispensation, process as eDispensation
                if (eot.getClassification().get(j).getNodeRepresentation()
                        .compareTo(Constants.ED_CLASSCODE) == 0) {
                    LOGGER.info("Processing an eDispensation message");

                    return saveDispensation(request, sh, eventLog);

                } // TODO: check the right LOINC code, currently coded as in
                // example 3.4.2 ver. 2.2 p. 82
                else if (eot.getClassification().get(j).getNodeRepresentation().compareTo(Constants.CONSENT_CLASSCODE) == 0) {
                    LOGGER.info("Processing a consent");
                    return saveConsent(request, sh, eventLog);
                } else if (eot.getClassification().get(j).getNodeRepresentation()
                        .equals(Constants.HCER_CLASSCODE)) {
                    LOGGER.info("Processing HCER document");
                    return saveHCER(request, sh, eventLog);
                }
            }
            break;
        }

        return reportDocumentTypeError(request);
    }

    public RegistryResponseType saveConsent(ProvideAndRegisterDocumentSetRequestType request, SOAPHeader sh, EventLog eventLog) {

        RegistryResponseType response = new RegistryResponseType();
        String sigCountryCode = null;

        Element shElement = null;
        try {
            shElement = XMLUtils.toDOM(sh);
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }
        documentSubmitService.setSOAPHeader(shElement);

        RegistryErrorList rel = ofRs.createRegistryErrorList();
        try {
            sigCountryCode = SAML2Validator.validateXDRHeader(shElement, Constants.CONSENT_CLASSCODE);

        } catch (InsufficientRightsException e) {
            LOGGER.error("InsufficientRightsException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
        } catch (AssertionValidationException e) {
            LOGGER.error("AssertionValidationException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
        } catch (SMgrException e) {
            LOGGER.error("SMgrException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage("", e.getMessage(), "", false));
        }

        String patientId = getPatientId(request);
        LOGGER.info("Received a consent document for patient: '{}'", patientId);
        /*
         * Here PDP checks and related calls are skipped, necessary checks to be
         * performed in the NI while processing the consent document.
         */
        try {
            shElement = XMLUtils.toDOM(sh);
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }

        for (int i = 0; i < request.getDocument().size(); i++) {
            Document doc = request.getDocument().get(i);
            byte[] docBytes = doc.getValue();
            try {

                /* Validate CDA epSOS Pivot */
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(new String(doc.getValue(), StandardCharsets.UTF_8),
                            NcpSide.NCP_A, obtainClassCode(request), true);
                }

                //Resets the response document to a translated version.
                docBytes = TMServices.transformDocument(docBytes, Constants.LANGUAGE_CODE);

                /* Validate CDA epSOS Pivot */
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(new String(docBytes, StandardCharsets.UTF_8),
                            NcpSide.NCP_A, obtainClassCode(request), true);
                }

            } catch (DocumentTransformationException ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            } catch (Exception ex) {
                LOGGER.error(null, ex);
            }
            try {
                org.w3c.dom.Document domDocument = TMServices.byteToDocument(docBytes);
                EPSOSDocument epsosDocument = DocumentFactory.createEPSOSDocument(patientId, Constants.CONSENT_CLASSCODE, domDocument);

                // Evidence for call to NI for XDR submit (patient consent)
                // Joao: here we have a Document so we can generate the mandatory NRO
                try {
                    EvidenceUtils.createEvidenceREMNRO(epsosDocument.getDocument(),
                            Constants.NCP_SIG_KEYSTORE_PATH,
                            Constants.NCP_SIG_KEYSTORE_PASSWORD,
                            Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                            Constants.SP_KEYSTORE_PATH,
                            Constants.SP_KEYSTORE_PASSWORD,
                            Constants.SP_PRIVATEKEY_ALIAS,
                            Constants.NCP_SIG_KEYSTORE_PATH,
                            Constants.NCP_SIG_KEYSTORE_PASSWORD,
                            Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                            IHEEventType.epsosConsentServicePut.getCode(),
                            new DateTime(),
                            EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                            "NI_XDR_CONSENT_REQ",
                            Objects.requireNonNull(Helper.getTRCAssertion(shElement)).getID() + "__" + DateUtil.getCurrentTimeGMT());

                } catch (Exception e) {
                    LOGGER.error(ExceptionUtils.getStackTrace(e));
                }

                documentSubmitService.submitPatientConsent(epsosDocument);
                // Evidence for response from NI for XDR submit (patient consent)
                /* Joao: the NRR is being generated based on the request message (submitted document). The interface for document submission does not return
                    any response for the submit service. This NRR is optional as per the CP. Left commented for now. */
//                try {
//                    EvidenceUtils.createEvidenceREMNRR(epsosDocument.toString(),
//                            Constants.NCP_SIG_KEYSTORE_PATH,
//                            Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                            Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                            IHEEventType.epsosConsentServicePut.getCode(),
//                            new DateTime(),
//                            EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                            "NI_XDR_CONSENT_RES",
//                            Helper.getDocumentEntryPatientIdFromTRCAssertion(shElement) + "__" + DateUtil.getCurrentTimeGMT());
//                } catch (Exception e) {
//                    LOGGER.error(ExceptionUtils.getStackTrace(e));
//                }

            } catch (DocumentProcessingException e) {
                LOGGER.error("DocumentProcessingException: '{}'", e.getMessage(), e);
                rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
            } catch (Exception e) {
                LOGGER.error("Exception: '{}'", e.getMessage(), e);
                rel.getRegistryError().add(createErrorMessage("", e.getMessage(), "", false));
            }
        }
        if (!rel.getRegistryError().isEmpty()) {
            response.setRegistryErrorList(rel);
            response.setStatus(AdhocQueryResponseStatus.FAILURE);
        } else {
            response.setStatus(AdhocQueryResponseStatus.SUCCESS);
        }

        try {
            prepareEventLogForConsentPut(eventLog, request, response, shElement);
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            // Is this fatal?
        }
        return response;
    }

    protected String validateXDRHeader(Element sh, String classCode) throws MissingFieldException, InvalidFieldException, SMgrException, InsufficientRightsException {
        return SAML2Validator.validateXDRHeader(sh, classCode);
    }

    public RegistryResponseType saveHCER(ProvideAndRegisterDocumentSetRequestType request, SOAPHeader sh, EventLog eventLog) {

        RegistryResponseType response = new RegistryResponseType();
        String sigCountryCode = null;

        Element shElement = null;
        try {
            shElement = XMLUtils.toDOM(sh);
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }
        documentSubmitService.setSOAPHeader(shElement);

        RegistryErrorList rel = ofRs.createRegistryErrorList();
        try {
            sigCountryCode = validateXDRHeader(shElement, Constants.HCER_CLASSCODE);
        } catch (InsufficientRightsException e) {
            LOGGER.error("InsufficientRightsException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
        } catch (AssertionValidationException e) {
            LOGGER.error("AssertionValidationException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
        } catch (SMgrException e) {
            LOGGER.error("SMgrException: '{}'", e.getMessage(), e);
            rel.getRegistryError().add(createErrorMessage("", e.getMessage(), "", false));
        }

        String patientId = getPatientId(request);
        LOGGER.info("Received a HCER document for patient: '{}' ", patientId);
        /*
         * Here PDP checks and related calls are skipped, necessary checks to be
         * performed in the NI while processing the consent document.
         */
        for (int i = 0; i < request.getDocument().size(); i++) {
            Document doc = request.getDocument().get(i);

            try {
                /* Validate CDA epSOS Pivot */
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(new String(doc.getValue(), StandardCharsets.UTF_8),
                            NcpSide.NCP_A, obtainClassCode(request), true);
                }

                String documentString = new String(TMServices.transformDocument(doc.getValue(), Constants.LANGUAGE_CODE), StandardCharsets.UTF_8);

                /* Validate CDA epSOS Pivot */
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(documentString, NcpSide.NCP_A, obtainClassCode(request), true);
                }

                org.w3c.dom.Document domDocument = XMLUtil.parseContent(documentString);
                EPSOSDocument epsosDocument = DocumentFactory.createEPSOSDocument(patientId, Constants.HCER_CLASSCODE, domDocument);
                documentSubmitService.submitHCER(epsosDocument);
            } catch (DocumentProcessingException e) {
                LOGGER.error("DocumentProcessingException: '{}'", e.getMessage(), e);
                rel.getRegistryError().add(createErrorMessage(e.getCode(), e.getMessage(), "", false));
            } catch (DocumentTransformationException e) {
                LOGGER.error("DocumentTransformationException: '{}'", e.getMessage(), e);
                rel.getRegistryError().add(createErrorMessage(e.getErrorCode(), e.getCodeContext(), e.getMessage(), false));
            } catch (Exception e) {
                LOGGER.error("Exception: '{}'", e.getMessage(), e);
                rel.getRegistryError().add(createErrorMessage("", e.getMessage(), "", false));
            }
        }
        if (!rel.getRegistryError().isEmpty()) {
            response.setRegistryErrorList(rel);
            response.setStatus(AdhocQueryResponseStatus.FAILURE);
        } else {
            response.setStatus(AdhocQueryResponseStatus.SUCCESS);
        }

        return response;
    }

    public RegistryResponseType reportDocumentTypeError(ProvideAndRegisterDocumentSetRequestType request) {

        RegistryResponseType response = new RegistryResponseType();

        response.setRegistryErrorList(new RegistryErrorList());

        RegistryError error = new RegistryError();
        error.setErrorCode("4202");
        error.setCodeContext("Unknown document");
        response.getRegistryErrorList().getRegistryError().add(error);
        response.setStatus(AdhocQueryResponseStatus.FAILURE);

        return response;
    }

    /**
     * This method will extract the document class code from a given ProvideAndRegisterDocumentSetRequestType message.
     *
     * @param request the request containing the class code.
     * @return the class code.
     */
    private String obtainClassCode(final ProvideAndRegisterDocumentSetRequestType request) {

        if (request == null) {
            LOGGER.error("The provided request message in order to extract the classcode is null.");
            return null;
        }

        final String CLASS_SCHEME = XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_SCHEME;
        String result = "";

        for (int i = 0; i < request.getSubmitObjectsRequest()
                .getRegistryObjectList().getIdentifiable().size(); i++) {
            if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            ExtrinsicObjectType eot = (ExtrinsicObjectType) request
                    .getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue();
            for (int j = 0; j < eot.getClassification().size(); j++) {
                if (eot.getClassification().get(j).getClassificationScheme().equals(CLASS_SCHEME)) {
                    result = eot.getClassification().get(j).getNodeRepresentation();
                    break;
                }
            }
        }

        if (result.isEmpty()) {
            LOGGER.warn("No class code was found in request object.");
        }
        return result;
    }

    /**
     * @param dPe
     * @return
     */
    private DocumentProcessingException normalizeDocProcException(DocumentProcessingException dPe) {

        final String REPOSITORY_INTERNAL_ERROR = "XDSRepositoryError";
        DocumentProcessingException result = new DocumentProcessingException();

        if (StringUtils.equals(dPe.getMessage(), "DOCUMENT TRANSLATION FAILED.")) {
            result.setCode(REPOSITORY_INTERNAL_ERROR);
            result.setCodeSystem(null);
            result.setMessage("An error has occurred during the document translation.");
            result.setStackTrace(dPe.getStackTrace());
        }

        return result;
    }
}
