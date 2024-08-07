package tr.com.srdc.epsos.ws.server.xdr;

import epsos.ccd.gnomon.auditmanager.*;
import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import eu.epsos.exceptions.DocumentTransformationException;
import eu.epsos.protocolterminators.ws.server.common.RegistryErrorSeverity;
import eu.epsos.protocolterminators.ws.server.exception.NIException;
import eu.epsos.protocolterminators.ws.server.exception.NoConsentException;
import eu.epsos.protocolterminators.ws.server.utils.RegistryErrorUtils;
import eu.epsos.protocolterminators.ws.server.xdr.DocumentProcessingException;
import eu.epsos.protocolterminators.ws.server.xdr.DocumentSubmitInterface;
import eu.epsos.protocolterminators.ws.server.xdr.XDRServiceInterface;
import eu.epsos.pt.transformation.DomUtils;
import eu.epsos.pt.transformation.TranslationsAndMappingsClient;
import eu.epsos.util.EvidenceUtils;
import eu.epsos.util.IheConstants;
import eu.epsos.util.xdr.XDRConstants;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.constant.ClassCode;
import eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.Helper;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.exceptions.OpenNCPErrorCodeException;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.saml.SAML2Validator;
import eu.europa.ec.sante.ehdsi.openncp.model.DiscardDispenseDetails;
import eu.europa.ec.sante.ehdsi.openncp.pt.common.AdhocQueryResponseStatus;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TMResponseStructure;
import eu.europa.ec.sante.ehdsi.openncp.tm.util.Base64Util;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import fi.kela.se.epsos.data.model.DocumentFactory;
import fi.kela.se.epsos.data.model.EPSOSDocument;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.DateUtil;
import tr.com.srdc.epsos.util.XMLUtil;
import tr.com.srdc.epsos.util.http.HTTPUtil;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class XDRServiceImpl implements XDRServiceInterface {

    private static final DatatypeFactory DATATYPE_FACTORY;
    private static final String HL7_NAMESPACE = "urn:hl7-org:v3";

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (final DatatypeConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(XDRServiceImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private final oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory ofRs;
    private final DocumentSubmitInterface documentSubmitService;

    public XDRServiceImpl() {

        final ServiceLoader<DocumentSubmitInterface> serviceLoader = ServiceLoader.load(DocumentSubmitInterface.class);
        try {
            logger.info("Loading National implementation of DocumentSubmitInterface...");
            documentSubmitService = serviceLoader.iterator().next();
            logger.info("Successfully loaded documentSubmitService");
        } catch (final Exception e) {
            logger.error("Failed to load implementation of documentSubmitService: " + e.getMessage(), e);
            throw e;
        }
        ofRs = new oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory();
    }

    protected XDRServiceImpl(final DocumentSubmitInterface dsi, final oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory ofRs) {
        this.documentSubmitService = dsi;
        this.ofRs = ofRs;
    }

    private RegistryError createErrorMessage(final OpenNCPErrorCode openncpErrorCode, final String codeContext, final String value, final String location, final RegistryErrorSeverity severity) {
        final RegistryError registryError = ofRs.createRegistryError();
        registryError.setErrorCode(openncpErrorCode.getCode());
        registryError.setLocation(location);
        registryError.setSeverity(severity.getText());
        registryError.setCodeContext(codeContext);
        registryError.setValue(value);
        return registryError;
    }

    private void prepareEventLogForDiscardMedication(final EventLog eventLog, final String discardId, final ProvideAndRegisterDocumentSetRequestType request,
                                                     final RegistryResponseType response, final Element soapHeader) {

        eventLog.setEventType(EventType.DISPENSATION_SERVICE_DISCARD);
        eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_DISCARD);
        eventLog.setEI_EventActionCode(EventActionCode.CREATE);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        eventLog.getEventTargetParticipantObjectIds().add(discardId);
        if (request.getSubmitObjectsRequest().getRegistryObjectList() != null) {

            for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().size(); i++) {
                if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                final ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiable().get(i).getValue();
                String documentId = "";
                for (final ExternalIdentifierType eit : eot.getExternalIdentifier()) {
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
        final String userIdAlias = Helper.getAssertionsSPProvidedId(soapHeader);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : ""
                + "<" + Helper.getUserID(soapHeader) + "@" + Helper.getAssertionsIssuer(soapHeader) + ">");
        eventLog.setHR_AlternativeUserID(Helper.getAlternateUserID(soapHeader));
        eventLog.setHR_RoleID(Helper.getRoleID(soapHeader));
        eventLog.setSP_UserID(HTTPUtil.getSubjectDN(true));
        eventLog.setPT_ParticipantObjectIDs(Collections.singletonList(getDocumentEntryPatientId(request)));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (response.getRegistryErrorList() != null) {
            final RegistryError re = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_ParticipantObjectID(re.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(re.getCodeContext().getBytes());
        }
    }

    /**
     * Prepare audit log for the dispensation service, initialize() operation, i.e. dispensation submit operation
     *
     * @author konstantin.hypponen@kela.fi
     */
    public void prepareEventLogForDispensationInitialize(final EventLog eventLog, final ProvideAndRegisterDocumentSetRequestType request,
                                                         final RegistryResponseType response, final Element sh) {

        eventLog.setEventType(EventType.DISPENSATION_SERVICE_INITIALIZE);
        eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_INITIALIZE);
        eventLog.setEI_EventActionCode(EventActionCode.CREATE);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));

        if (request.getSubmitObjectsRequest().getRegistryObjectList() != null) {

            for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().size(); i++) {
                if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                final ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiable().get(i).getValue();
                String documentId = "";
                for (final ExternalIdentifierType eit : eot.getExternalIdentifier()) {
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

        final String userIdAlias = Helper.getAssertionsSPProvidedId(sh);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : ""
                + "<" + Helper.getUserID(sh) + "@" + Helper.getAssertionsIssuer(sh) + ">");
        eventLog.setHR_AlternativeUserID(Helper.getAlternateUserID(sh));
        eventLog.setHR_RoleID(Helper.getRoleID(sh));
        eventLog.setSP_UserID(HTTPUtil.getSubjectDN(true));
        eventLog.setPT_ParticipantObjectIDs(Collections.singletonList(getDocumentEntryPatientId(request)));
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);

        if (response.getRegistryErrorList() != null) {
            final RegistryError re = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_ParticipantObjectID(re.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(re.getCodeContext().getBytes());
        }
    }


    private String getDocumentEntryPatientId(final ProvideAndRegisterDocumentSetRequestType request) {

        String patientId = "";
        // Traverse all ExtrinsicObjects
        for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().size(); i++) {

            if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue();
            // Traverse all Classification blocks in the ExtrinsicObject selected
            for (int j = 0; j < eot.getSlot().size(); j++) {
                // Search for the slot with the name "sourcePatientId"
                if (StringUtils.equals(eot.getSlot().get(j).getName(), "sourcePatientId")) {
                    patientId = eot.getSlot().get(j).getValueList().getValue().get(0);
                    return patientId;
                }
            }
        }
        logger.error("Could not locate the patient id of the XDR request.");
        return patientId;
    }

    private String getPatientId(final ProvideAndRegisterDocumentSetRequestType request) {

        final String patientId = getDocumentEntryPatientId(request);
        return patientId.substring(0, patientId.indexOf("^^^"));
    }

    /**
     * @param request    - XDR submit request.
     * @param soapHeader - SOAP Header from XDR message.
     * @param eventLog   - Discard Medication event log.
     * @return XDR Discard Medication object.
     * @throws Exception - Generic Exception in case of error, should be finalized using specific Exception.
     */
    public RegistryResponseType discardMedicationDispensed(final ProvideAndRegisterDocumentSetRequestType request,
                                                           final SOAPHeader soapHeader, final EventLog eventLog) throws Exception {

        logger.info("Processing Discard Dispense Medication");
        final RegistryErrorList registryErrorList = ofRs.createRegistryErrorList();
        final Element soapHeaderElement = XMLUtils.toDOM(soapHeader);
        documentSubmitService.setSOAPHeader(soapHeaderElement);

        //  Validate HCP SAML token according de Medication Discard Dispense rule:
        String sealCountryCode = null;

        try {
            sealCountryCode = SAML2Validator.validateXDRHeader(soapHeaderElement, ClassCode.EDD_CLASSCODE);

        } catch (final OpenNCPErrorCodeException | SMgrException e) {
            logger.error("'{}': '{}'", e.getClass().getName(), e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    OpenNCPErrorCode.ERROR_SEC_GENERIC,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        final String fullPatientId = getDocumentEntryPatientId(request);
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.info("Received a Discard eDispense document for patient: '{}'", fullPatientId);
        }
        String countryCode = "";
        final String distinguishedName = eventLog.getSC_UserID();
        final int cIndex = distinguishedName.indexOf("C=");

        if (cIndex > 0) {
            countryCode = distinguishedName.substring(cIndex + 2, cIndex + 4);
        } else {
            logger.info("Could not get client country code from the service consumer certificate. " +
                    "The reason can be that the call was not via HTTPS. Will check the country code from the signature certificate now.");
            if (sealCountryCode != null) {
                logger.info("Found the client country code via the signature certificate.");
                countryCode = sealCountryCode;
            }
        }
        logger.info("The client country code to be used by the PDP: '{}'", countryCode);
        if (!SAML2Validator.isConsentGiven(fullPatientId, countryCode)) {
            logger.debug("No consent given, throwing InsufficientRightsException");
            final NoConsentException e = new NoConsentException(null);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    e.getOpenncpErrorCode(),
                    e.getMessage(),
                    null,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        final RegistryResponseType response = new RegistryResponseType();
        String documentId = "";
        String discardId = "";
        String discardDate = "";

        try {
            final org.w3c.dom.Document domDocument = DomUtils.byteToDocument(request.getDocument().get(0).getValue());
            final EPSOSDocument epsosDocument = DocumentFactory.createEPSOSDocument(fullPatientId, ClassCode.ED_CLASSCODE, domDocument);
            documentId = getDocumentId(epsosDocument.getDocument());
            // Evidence for call to NI for XDR submit (dispensation)
            // Joao: here we have a Document, so we can generate the mandatory NRO
            try {
                EvidenceUtils.createEvidenceREMNRO(epsosDocument.getDocument(), Constants.NCP_SIG_KEYSTORE_PATH,
                        Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                        Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD,
                        Constants.SP_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH,
                        Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                        EventType.DISPENSATION_SERVICE_INITIALIZE.getIheCode(), new DateTime(),
                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XDR_DISP_REQ",
                        Objects.requireNonNull(Helper.getTRCAssertion(soapHeaderElement)).getID() + "__" + DateUtil.getCurrentTimeGMT());
            } catch (final Exception e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }

            final List<JAXBElement<? extends IdentifiableType>> registryObjectList = request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable();
            if (registryObjectList != null) {
                for (final JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {

                    if (identifiable.getValue() instanceof ExtrinsicObjectType) {
                        final List<SlotType1> slotType1List = identifiable.getValue().getSlot();
                        for (final SlotType1 slotType1 : slotType1List) {
                            if (StringUtils.equals(slotType1.getName(), "creationTime")) {
                                discardDate = slotType1.getValueList().getValue().get(0);
                            }
                        }
                    } else if (identifiable.getValue() instanceof RegistryPackageType) {
                        final RegistryPackageType registryPackageType = (RegistryPackageType) identifiable.getValue();
                        for (final ExternalIdentifierType externalIdentifier : registryPackageType.getExternalIdentifier()) {
                            if (StringUtils.equals(externalIdentifier.getIdentificationScheme(), "urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8")) {
                                discardId = externalIdentifier.getValue();
                            }
                        }
                    }
                }
            }
            //  Call to National Connector
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(XDRConstants.REGISTRY_PACKAGE.SUBMISSION_TIME_FORMAT);
            final DiscardDispenseDetails discardDetails = new DiscardDispenseDetails();
            discardDetails.setDispenseId(documentId);
            discardDetails.setDiscardId(discardId);
            discardDetails.setDiscardDate(simpleDateFormat.parse(discardDate));
            discardDetails.setPatientId(fullPatientId);
            discardDetails.setHealthCareProvider(Helper.getAlternateUserID(soapHeaderElement));
            discardDetails.setHealthCareProviderId(Helper.getAssertionsSPProvidedId(soapHeaderElement));
            discardDetails.setHealthCareProviderFacility(Helper.getXSPALocality(soapHeaderElement));
            discardDetails.setHealthCareProviderOrganization(Helper.getOrganization(soapHeaderElement));
            discardDetails.setHealthCareProviderOrganizationId(Helper.getOrganizationId(soapHeaderElement));
            //  TODO: EHNCP-2055 Inconsistency in handling patient id
            documentSubmitService.cancelDispensation(discardDetails, epsosDocument);

        } catch (final NIException e) {
            logger.error("NIException: '[{}]'-'[{}]'", e.getOpenncpErrorCode(), e.getMessage());
            registryErrorList.getRegistryError().add(createErrorMessage(e.getOpenncpErrorCode(), e.getOpenncpErrorCode().getDescription() + "^" + e.getMessage(), "", e.getMessage(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR));
        } catch (final Exception e) {
            logger.error("Generic Exception: '[{}]'", e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    OpenNCPErrorCode.ERROR_ED_GENERIC,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        if (registryErrorList.getRegistryError().isEmpty()) {
            response.setStatus(AdhocQueryResponseStatus.SUCCESS);
        } else {
            response.setRegistryErrorList(registryErrorList);
            response.setStatus(AdhocQueryResponseStatus.FAILURE);
        }
        prepareEventLogForDiscardMedication(eventLog, discardId, request, response, soapHeaderElement);

        return response;
    }

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @return
     * @throws Exception
     */
    public RegistryResponseType saveDispensation(final ProvideAndRegisterDocumentSetRequestType request, final SOAPHeader soapHeader,
                                                 final EventLog eventLog) throws Exception {

        logger.info("Processing Dispense Medication");
        final RegistryResponseType response = new RegistryResponseType();
        String sealCountryCode = null;

        Element shElement;
        try {
            shElement = XMLUtils.toDOM(soapHeader);
        } catch (final Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
        documentSubmitService.setSOAPHeader(shElement);

        final RegistryErrorList registryErrorList = ofRs.createRegistryErrorList();
        try {
            sealCountryCode = SAML2Validator.validateXDRHeader(shElement, ClassCode.ED_CLASSCODE);

        } catch (final OpenNCPErrorCodeException e) {
            logger.error("OpenncpErrorCodeException: '{}'", e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    e.getErrorCode(),
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (final SMgrException e) {
            logger.error("SMgrException: '{}'", e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    OpenNCPErrorCode.ERROR_SEC_GENERIC,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        final String fullPatientId = getDocumentEntryPatientId(request);
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.info("Received a eDispense document for patient: '{}'", fullPatientId);
        }
        String countryCode = "";
        final String distinguishedName = eventLog.getSC_UserID();
        final int cIndex = distinguishedName.indexOf("C=");

        if (cIndex > 0) {
            countryCode = distinguishedName.substring(cIndex + 2, cIndex + 4);
        }
        // Mustafa: This part is added for handling consents when the call is not https.
        // In this case, we check the country code of the signature certificate that ships within the HCP assertion.
        // TODO: Might be necessary to remove later, although it does no harm in reality!
        else {
            logger.info("Could not get client country code from the service consumer certificate. " +
                    "The reason can be that the call was not via HTTPS. Will check the country code from the signature certificate now.");
            if (sealCountryCode != null) {
                logger.info("Found the client country code via the signature certificate.");
                countryCode = sealCountryCode;
            }
        }
        logger.info("The client country code to be used by the PDP: '{}'", countryCode);
        if (!SAML2Validator.isConsentGiven(fullPatientId, countryCode)) {
            logger.debug("No consent given, throwing InsufficientRightsException");
            final NoConsentException e = new NoConsentException(null);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    e.getOpenncpErrorCode(),
                    e.getMessage(),
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }
        if (!registryErrorList.getRegistryError().isEmpty()) {
            response.setRegistryErrorList(registryErrorList);
            response.setStatus(AdhocQueryResponseStatus.FAILURE);
        } else {
            try {
                shElement = XMLUtils.toDOM(soapHeader);
            } catch (final Exception e) {
                logger.error(e.getMessage());
                throw e;
            }

            for (int i = 0; i < request.getDocument().size(); i++) {

                final ProvideAndRegisterDocumentSetRequestType.Document doc = request.getDocument().get(i);
                String documentId = StringUtils.EMPTY;
                byte[] docBytes = doc.getValue();
                org.w3c.dom.Document domDocument = null;
                try {

                    //  Validate CDA epSOS Pivot.
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCdaDocument(new String(doc.getValue(), StandardCharsets.UTF_8),
                                NcpSide.NCP_A, obtainClassCode(request), true);
                    }

                    //  Reset the response document to a translated version.
                    final TMResponseStructure tmResponseStructure = TranslationsAndMappingsClient.translate(DomUtils.byteToDocument(docBytes), Constants.LANGUAGE_CODE);
                    domDocument = Base64Util.decode(tmResponseStructure.getResponseCDA());
                    docBytes = XMLUtils.toOM(domDocument.getDocumentElement()).toString().getBytes(StandardCharsets.UTF_8);


                    // Validate CDA epSOS Pivot
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCdaDocument(new String(docBytes, StandardCharsets.UTF_8), NcpSide.NCP_A,
                                obtainClassCode(request), false);
                    }
                } catch (final DocumentTransformationException ex) {
                    logger.error(ex.getLocalizedMessage(), ex);
                }

                try {
                    final EPSOSDocument epsosDocument = DocumentFactory.createEPSOSDocument(fullPatientId, ClassCode.ED_CLASSCODE, domDocument);
                    // Evidence for call to NI for XDR submit (dispensation)
                    // Joao: here we have a Document, so we can generate the mandatory NRO
                    try {
                        EvidenceUtils.createEvidenceREMNRO(epsosDocument.getDocument(), Constants.NCP_SIG_KEYSTORE_PATH,
                                Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                                Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD,
                                Constants.SP_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH,
                                Constants.NCP_SIG_KEYSTORE_PASSWORD, Constants.NCP_SIG_PRIVATEKEY_ALIAS,
                                EventType.DISPENSATION_SERVICE_INITIALIZE.getIheCode(), new DateTime(),
                                EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XDR_DISP_REQ",
                                Objects.requireNonNull(Helper.getTRCAssertion(shElement)).getID() + "__" + DateUtil.getCurrentTimeGMT());
                    } catch (final Exception e) {
                        logger.error(ExceptionUtils.getStackTrace(e));
                    }
                    // Call to National Connector
                    documentId = getDocumentId(epsosDocument.getDocument());
                    documentSubmitService.submitDispensation(epsosDocument);

                    // Evidence for response from NI for XDR submit (dispensation)
                    /* Joao: the NRR is being generated based on the request message (submitted document). The interface for document submission does not return
                    any response for the submit service. This NRR is optional as per the CP. Left commented for now. */
//                    try {
//                        EvidenceUtils.createEvidenceREMNRR(epsosDocument.toString(),
//                                tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
//                                tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                                tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                                IHEEventType.epsosDispensationServiceInitialize.getCode(),
//                                new DateTime(),
//                                EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                                "NI_XDR_DISP_RES",
//                                Helper.getDocumentEntryPatientIdFromTRCAssertion(shElement) + "__" + DateUtil.getCurrentTimeGMT());
//                    } catch (Exception e) {
//                        logger.error(ExceptionUtils.getStackTrace(e));
//                    }
                } catch (final NIException e) {
                    logger.error("NIException: [{}] - [{}]", e.getOpenncpErrorCode(), e.getMessage());
                    registryErrorList.getRegistryError().add(createErrorMessage(e.getOpenncpErrorCode(), e.getOpenncpErrorCode().getDescription() + " ( " + documentId + " )", "", e.getMessage(), RegistryErrorSeverity.ERROR_SEVERITY_ERROR));
                } catch (final Exception e) {
                    logger.error("Generic Exception: '{}'", e.getMessage(), e);
                    RegistryErrorUtils.addErrorMessage(
                            registryErrorList,
                            OpenNCPErrorCode.ERROR_ED_GENERIC,
                            e.getMessage(),
                            e,
                            RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
                }
            }
            if (!registryErrorList.getRegistryError().isEmpty()) {
                response.setRegistryErrorList(registryErrorList);
                response.setStatus(AdhocQueryResponseStatus.FAILURE);
            } else {
                response.setStatus(AdhocQueryResponseStatus.SUCCESS);
            }
        }
        prepareEventLogForDispensationInitialize(eventLog, request, response, shElement);

        return response;
    }

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @return
     * @throws Exception
     */
    public RegistryResponseType saveDocument(final ProvideAndRegisterDocumentSetRequestType request, final SOAPHeader soapHeader,
                                             final EventLog eventLog) throws Exception {

        logger.info("[WS] XDR Service: Save Document");
        // Traverse all ExtrinsicObjects
        for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().size(); i++) {

            if (!(request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().get(i).getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType extrinsicObject = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue();

            // Traverse all Classification blocks in the ExtrinsicObject selected
            for (final ClassificationType classification : extrinsicObject.getClassification()) {

                logger.debug("[WS] XDR Service: Classification: '{}'-'{}'", classification.getClassificationScheme(), classification.getNodeRepresentation());
                if (StringUtils.equals(classification.getClassificationScheme(), IheConstants.FORMAT_CODE_SCHEME)) {

                    // TODO: check the right LOINC code, currently coded as in example 3.4.2 ver. 2.2 p. 82
                    if (StringUtils.equals(classification.getNodeRepresentation(), "urn:epSOS:ep:dis:2010")) {
                        //  urn:epSOS:ep:dis:2010
                        return saveDispensation(request, soapHeader, eventLog);

                    } else if (StringUtils.equals(classification.getNodeRepresentation(), "urn:eHDSI:ed:discard:2020")) {
                        //  "urn:eHDSI:ed:discard:2020"
                        return discardMedicationDispensed(request, soapHeader, eventLog);
                    }
                }
            }
            break;
        }

        return reportDocumentTypeError(request);
    }

    protected String validateXDRHeader(final Element sh, final ClassCode classCode) throws MissingFieldException, InvalidFieldException,
            SMgrException, InsufficientRightsException {

        return SAML2Validator.validateXDRHeader(sh, classCode);
    }

    /**
     * @param request
     * @param sh
     * @param eventLog
     * @return
     */
    public RegistryResponseType saveHCER(final ProvideAndRegisterDocumentSetRequestType request, final SOAPHeader sh, final EventLog
            eventLog) {

        final RegistryResponseType response = new RegistryResponseType();
        String sigCountryCode = null;

        Element shElement = null;
        try {
            shElement = XMLUtils.toDOM(sh);
        } catch (final Exception e) {
            logger.error("Exception: '{}'", e.getMessage(), e);
        }
        documentSubmitService.setSOAPHeader(shElement);

        final RegistryErrorList rel = ofRs.createRegistryErrorList();
        try {
            sigCountryCode = validateXDRHeader(shElement, ClassCode.HCER_CLASSCODE);
        } catch (final OpenNCPErrorCodeException e) {
            logger.error("OpenncpErrorCodeException: '{}'", e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    rel,
                    e.getErrorCode(),
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (final SMgrException e) {
            logger.error("SMgrException: '{}'", e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    rel,
                    OpenNCPErrorCode.ERROR_SEC_GENERIC,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        final String fullPatientId = getDocumentEntryPatientId(request);
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.info("Received a HCER document for patient: '{}'", fullPatientId);
        }
        /*
         * Here PDP checks and related calls are skipped, necessary checks to be performed in the NI while processing
         * the consent document.
         */
        for (int i = 0; i < request.getDocument().size(); i++) {
            final Document doc = request.getDocument().get(i);

            try {
                /* Validate CDA epSOS Pivot */
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(new String(doc.getValue(), StandardCharsets.UTF_8),
                            NcpSide.NCP_A, obtainClassCode(request), true);
                }

                final TMResponseStructure tmResponseStructure = TranslationsAndMappingsClient.translate(DomUtils.byteToDocument(doc.getValue()), Constants.LANGUAGE_CODE);
                final org.w3c.dom.Document cdaDocument = Base64Util.decode(tmResponseStructure.getResponseCDA());
                final byte[] docBytes = XMLUtils.toOM(cdaDocument.getDocumentElement()).toString().getBytes(StandardCharsets.UTF_8);
                final String documentString = new String(docBytes, StandardCharsets.UTF_8);


                /* Validate CDA epSOS Pivot */
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(documentString, NcpSide.NCP_A, obtainClassCode(request), true);
                }

                final org.w3c.dom.Document domDocument = XMLUtil.parseContent(documentString);
                final EPSOSDocument epsosDocument = DocumentFactory.createEPSOSDocument(fullPatientId, ClassCode.HCER_CLASSCODE, domDocument);
                documentSubmitService.submitHCER(epsosDocument);
            } catch (final DocumentProcessingException e) {
                logger.error("DocumentProcessingException: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorMessage(
                        rel,
                        e.getOpenncpErrorCode(),
                        e.getMessage(),
                        e,
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            } catch (final DocumentTransformationException e) {
                logger.error("DocumentTransformationException: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorMessage(
                        rel,
                        e.getErrorCode(),
                        e.getMessage(),
                        e,
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
            } catch (final Exception e) {
                logger.error("Exception: '{}'", e.getMessage(), e);
                RegistryErrorUtils.addErrorMessage(
                        rel,
                        OpenNCPErrorCode.ERROR_ED_GENERIC,
                        e.getMessage(),
                        e,
                        RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
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

    public RegistryResponseType reportDocumentTypeError(final ProvideAndRegisterDocumentSetRequestType request) {

        final RegistryResponseType response = new RegistryResponseType();

        response.setRegistryErrorList(new RegistryErrorList());

        final RegistryError error = new RegistryError();
        error.setErrorCode(OpenNCPErrorCode.ERROR_UNKNOWN_SIGNIFIER.getCode());
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
    private ClassCode obtainClassCode(final ProvideAndRegisterDocumentSetRequestType request) {

        if (request == null) {
            logger.error("The provided request message in order to extract the classCode is null.");
            return null;
        }

        final String CLASS_SCHEME = XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_SCHEME;
        String result = "";

        for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().size(); i++) {

            if (!(request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable().get(i).getValue()
                    instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiable().get(i).getValue();

            for (int j = 0; j < eot.getClassification().size(); j++) {

                if (eot.getClassification().get(j).getClassificationScheme().equals(CLASS_SCHEME)) {
                    result = eot.getClassification().get(j).getNodeRepresentation();
                    break;
                }
            }
        }

        if (result.isEmpty()) {
            logger.warn("No class code was found in request object.");
        }
        return ClassCode.getByCode(result);
    }

    private String getDocumentId(final org.w3c.dom.Document document) {

        String uid = "";
        if (document != null && document.getElementsByTagNameNS(HL7_NAMESPACE, "id").getLength() > 0) {
            final Node id = document.getElementsByTagNameNS(HL7_NAMESPACE, "id").item(0);
            if (id.getAttributes().getNamedItem("root") != null) {
                uid = uid + id.getAttributes().getNamedItem("root").getTextContent();
            }
            if (id.getAttributes().getNamedItem("extension") != null) {
                uid = uid + "^" + id.getAttributes().getNamedItem("extension").getTextContent();
            }
        }
        return uid;
    }
}
