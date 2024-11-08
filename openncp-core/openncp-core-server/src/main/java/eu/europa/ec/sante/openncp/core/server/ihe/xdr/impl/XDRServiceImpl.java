package eu.europa.ec.sante.openncp.core.server.ihe.xdr.impl;

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
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.constants.ihe.IheConstants;
import eu.europa.ec.sante.openncp.core.common.constants.ihe.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.IHEEventType;
import eu.europa.ec.sante.openncp.core.common.ihe.RegistryErrorSeverity;
import eu.europa.ec.sante.openncp.core.common.ihe.XDRServiceInterface;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.OpenNCPErrorCodeException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML2Validator;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.DiscardDispenseDetails;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.DocumentFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPSOSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.*;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.ObjectFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryError;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryErrorList;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.evidence.EvidenceUtils;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.DocumentTransformationException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoConsentException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.Base64Util;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.DomUtils;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xdr.DocumentSubmitInterface;
import eu.europa.ec.sante.openncp.core.server.ihe.AdhocQueryResponseStatus;
import eu.europa.ec.sante.openncp.core.server.ihe.RegistryErrorUtils;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
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
    private final ObjectFactory ofRs = new ObjectFactory();
    private final DocumentSubmitInterface documentSubmitInterface;
    private final SAML2Validator saml2Validator;
    private final CDATransformationService cdaTransformationService;

    public XDRServiceImpl(final DocumentSubmitInterface documentSubmitInterface, final SAML2Validator saml2Validator, final CDATransformationService cdaTransformationService) {
        this.documentSubmitInterface = Validate.notNull(documentSubmitInterface, "DocumentSubmitInterface cannot be null");
        this.saml2Validator = Validate.notNull(saml2Validator, "SAML2Validator cannot be null");
        this.cdaTransformationService = Validate.notNull(cdaTransformationService, "CDATransformationService cannot be null");
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
                final eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
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
        final String userIdAlias = SoapElementHelper.getAssertionsSPProvidedId(soapHeader);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "<" + SoapElementHelper.getUserID(soapHeader) + "@" + SoapElementHelper.getAssertionsIssuer(soapHeader) + ">");
        eventLog.setHR_AlternativeUserID(SoapElementHelper.getAlternateUserID(soapHeader));
        eventLog.setHR_RoleID(SoapElementHelper.getRoleID(soapHeader));
        eventLog.setSP_UserID(HttpUtil.getSubjectDN(true));
        eventLog.setPT_ParticipantObjectIDs(List.of(getDocumentEntryPatientId(request)));
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
                for (final eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ExternalIdentifierType eit : eot.getExternalIdentifier()) {
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

        final String userIdAlias = SoapElementHelper.getAssertionsSPProvidedId(sh);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "<" + SoapElementHelper.getUserID(sh) + "@" + SoapElementHelper.getAssertionsIssuer(sh) + ">");
        eventLog.setHR_AlternativeUserID(SoapElementHelper.getAlternateUserID(sh));
        eventLog.setHR_RoleID(SoapElementHelper.getRoleID(sh));
        eventLog.setSP_UserID(HttpUtil.getSubjectDN(true));
        eventLog.setPT_ParticipantObjectIDs(List.of(getDocumentEntryPatientId(request)));
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
        documentSubmitInterface.setSOAPHeader(soapHeaderElement);

        //  Validate HCP SAML token according de Medication Discard Dispense rule:
        String sealCountryCode = null;

        try {
            sealCountryCode = saml2Validator.validateXDRHeader(soapHeaderElement, ClassCode.EDD_CLASSCODE);

        } catch (final MissingFieldException e) {
            logger.error("'{}': '{}'", e.getClass().getName(), e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    OpenNCPErrorCode.ERROR_ED_MISSING_REQUIRED_FIELDS,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (final InvalidFieldException e) {
            logger.error("'{}': '{}'", e.getClass().getName(), e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    OpenNCPErrorCode.ERROR_ED_INCORRECT_FORMATTING,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
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
        if (!saml2Validator.isConsentGiven(fullPatientId, countryCode)) {
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
                        IHEEventType.DISPENSATION_SERVICE_INITIALIZE.getCode(), new DateTime(),
                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XDR_DISP_REQ",
                        Objects.requireNonNull(SoapElementHelper.getTRCAssertion(soapHeaderElement)).getID() + "__" + DateUtil.getCurrentTimeGMT());
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
            discardDetails.setHealthCareProvider(SoapElementHelper.getAlternateUserID(soapHeaderElement));
            discardDetails.setHealthCareProviderId(SoapElementHelper.getAssertionsSPProvidedId(soapHeaderElement));
            discardDetails.setHealthCareProviderFacility(SoapElementHelper.getXSPALocality(soapHeaderElement));
            discardDetails.setHealthCareProviderOrganization(SoapElementHelper.getOrganization(soapHeaderElement));
            discardDetails.setHealthCareProviderOrganizationId(SoapElementHelper.getOrganizationId(soapHeaderElement));
            documentSubmitInterface.cancelDispensation(discardDetails, epsosDocument);

        } catch (final NIException e) {
            logger.error("NIException: [{}] - [{}]", e.getOpenncpErrorCode(), e.getMessage());
            registryErrorList.getRegistryError().add(createErrorMessage(e.getOpenncpErrorCode(), e.getOpenncpErrorCode().getDescription() + "^" + e.getMessage(), "", Arrays.stream(ExceptionUtils.getRootCauseStackTrace(e)).findFirst().orElse(StringUtils.EMPTY), RegistryErrorSeverity.ERROR_SEVERITY_ERROR));
        } catch (final Exception e) {
            logger.error("Generic Exception: '{}'", e.getMessage(), e);
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
        documentSubmitInterface.setSOAPHeader(shElement);

        final RegistryErrorList registryErrorList = ofRs.createRegistryErrorList();
        try {
            sealCountryCode = saml2Validator.validateXDRHeader(shElement, ClassCode.ED_CLASSCODE);

        } catch (final MissingFieldException e) {
            logger.error("'{}': '{}'", e.getClass().getName(), e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    OpenNCPErrorCode.ERROR_ED_MISSING_REQUIRED_FIELDS,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        } catch (final InvalidFieldException e) {
            logger.error("'{}': '{}'", e.getClass().getName(), e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    OpenNCPErrorCode.ERROR_ED_INCORRECT_FORMATTING,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
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
        // Might be necessary to remove later, although it does no harm in reality!
        else {
            logger.info("Could not get client country code from the service consumer certificate. " +
                    "The reason can be that the call was not via HTTPS. Will check the country code from the signature certificate now.");
            if (sealCountryCode != null) {
                logger.info("Found the client country code via the signature certificate.");
                countryCode = sealCountryCode;
            }
        }
        logger.info("The client country code to be used by the PDP: '{}'", countryCode);
        if (!saml2Validator.isConsentGiven(fullPatientId, countryCode)) {
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
                byte[] docBytes = doc.getValue();
                org.w3c.dom.Document domDocument = null;
                try {

                    //  Validate CDA epSOS Pivot.
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCdaDocument(new String(doc.getValue(), StandardCharsets.UTF_8),
                                NcpSide.NCP_A, obtainClassCode(request), true);
                    }

                    //  Reset the response document to a translated version.
                    final TMResponseStructure tmResponseStructure = cdaTransformationService.translate(DomUtils.byteToDocument(docBytes), Constants.LANGUAGE_CODE);
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
                                IHEEventType.DISPENSATION_SERVICE_INITIALIZE.getCode(), new DateTime(),
                                EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XDR_DISP_REQ",
                                Objects.requireNonNull(SoapElementHelper.getTRCAssertion(shElement)).getID() + "__" + DateUtil.getCurrentTimeGMT());
                    } catch (final Exception e) {
                        logger.error(ExceptionUtils.getStackTrace(e));
                    }
                    // Call to National Connector
                    final String documentId = getDocumentId(epsosDocument.getDocument());
                    documentSubmitInterface.submitDispensation(epsosDocument);

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
                    registryErrorList.getRegistryError().add(createErrorMessage(e.getOpenncpErrorCode(), e.getOpenncpErrorCode().getDescription() + "^" + e.getMessage(), "", Arrays.stream(ExceptionUtils.getRootCauseStackTrace(e)).findFirst().orElse(StringUtils.EMPTY), RegistryErrorSeverity.ERROR_SEVERITY_ERROR));
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

                    // Check the right LOINC code, currently coded as in example 3.4.2 ver. 2.2 p. 82
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
