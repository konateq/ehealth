package eu.europa.ec.sante.openncp.core.server.ihe.xdr;

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
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import eu.europa.ec.sante.openncp.core.common.assertion.PolicyAssertionManager;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.OpenNCPErrorCodeException;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidationResult;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidator;
import eu.europa.ec.sante.openncp.core.common.ihe.IHEEventType;
import eu.europa.ec.sante.openncp.core.common.ihe.RegistryErrorSeverity;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.IheConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.DiscardDispenseDetails;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.DocumentFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPSOSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.evidence.EvidenceUtils;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.DocumentTransformationException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoConsentException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.Base64Util;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.DomUtils;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.*;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xdr.DocumentSubmitInterface;
import eu.europa.ec.sante.openncp.core.server.ihe.AdhocQueryResponseStatus;
import eu.europa.ec.sante.openncp.core.server.ihe.NationalConnectorFactory;
import eu.europa.ec.sante.openncp.core.server.ihe.RegistryErrorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.soap.SOAPHeader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class XdrServiceServerSideImpl implements XdrServiceServerSide {

    private static final DatatypeFactory DATATYPE_FACTORY;
    private static final String HL7_NAMESPACE = "urn:hl7-org:v3";

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (final DatatypeConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(XdrServiceServerSideImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final NationalConnectorFactory nationalConnectorFactory;
    private final AssertionValidator assertionValidator;
    private final CDATransformationService cdaTransformationService;
    private final PolicyAssertionManager policyAssertionManager;

    public XdrServiceServerSideImpl(final NationalConnectorFactory nationalConnectorFactory, final AssertionValidator assertionValidator, final PolicyAssertionManager policyAssertionManager, final CDATransformationService cdaTransformationService) {
        this.nationalConnectorFactory = Validate.notNull(nationalConnectorFactory, "nationalConnectorFactory cannot be null");
        this.assertionValidator = Validate.notNull(assertionValidator, "assertionValidator cannot be null");
        this.policyAssertionManager = Validate.notNull(policyAssertionManager, "policyAssertionManager must not be null");
        this.cdaTransformationService = Validate.notNull(cdaTransformationService, "CDATransformationService cannot be null");
    }

    private RegistryError createErrorMessage(final OpenNCPErrorCode openncpErrorCode, final String codeContext, final String value, final String location, final RegistryErrorSeverity severity) {
        final RegistryError registryError = objectFactory.createRegistryError();
        registryError.setErrorCode(openncpErrorCode.getCode());
        registryError.setLocation(location);
        registryError.setSeverity(severity.getText());
        registryError.setCodeContext(codeContext);
        registryError.setValue(value);
        return registryError;
    }

    private void prepareEventLogForDiscardMedication(final EventLog eventLog, final String discardId, final ProvideAndRegisterDocumentSetRequest request,
                                                     final RegistryResponseType response, final Element soapHeader) {

        eventLog.setEventType(EventType.XDR_SERVICE_NCP_A);
        eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_DISCARD);
        eventLog.setEI_EventActionCode(EventActionCode.CREATE);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        eventLog.getEventTargetParticipantObjectIds().add(discardId);
        if (request.getSubmitObjectsRequest().getRegistryObjectList() != null) {

            for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables().size(); i++) {
                if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiables().get(i).getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                final ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiables().get(i).getValue();
                String documentId = "";
                for (final ExternalIdentifierType eit : eot.getExternalIdentifiers()) {
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
            final RegistryError re = response.getRegistryErrorList().getRegistryErrors().get(0);
            eventLog.setEM_ParticipantObjectID(re.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(re.getCodeContext().getBytes());
        }
    }

    /**
     * Prepare audit log for the dispensation service, initialize() operation, i.e. dispensation submit operation
     *
     * @author konstantin.hypponen@kela.fi
     */
    public void prepareEventLogForDispensationInitialize(final EventLog eventLog, final ProvideAndRegisterDocumentSetRequest request,
                                                         final RegistryResponseType response, final Element sh) {
        eventLog.setEventType(EventType.XDR_SERVICE_NCP_A);
        eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_INITIALIZE);
        eventLog.setEI_EventActionCode(EventActionCode.CREATE);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));

        if (request.getSubmitObjectsRequest().getRegistryObjectList() != null) {
            for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables().size(); i++) {
                if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiables().get(i).getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                final ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                        .getIdentifiables().get(i).getValue();
                String documentId = "";
                for (final ExternalIdentifierType eit : eot.getExternalIdentifiers()) {
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
            final RegistryError re = response.getRegistryErrorList().getRegistryErrors().get(0);
            eventLog.setEM_ParticipantObjectID(re.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(re.getCodeContext().getBytes());
        }
    }

    private String getDocumentEntryPatientId(final ProvideAndRegisterDocumentSetRequest request) {

        String patientId = "";
        // Traverse all ExtrinsicObjects
        for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables().size(); i++) {

            if (!(request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiables().get(i).getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiables().get(i).getValue();
            // Traverse all Classification blocks in the ExtrinsicObject selected
            for (int j = 0; j < eot.getSlots().size(); j++) {
                // Search for the slot with the name "sourcePatientId"
                if (StringUtils.equals(eot.getSlots().get(j).getName(), "sourcePatientId")) {
                    patientId = eot.getSlots().get(j).getValueList().getValues().get(0);
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
    public RegistryResponseType discardMedicationDispensed(final DocumentSubmitInterface documentSubmitService, final ProvideAndRegisterDocumentSetRequest request,
                                                           final SOAPHeader soapHeader, final EventLog eventLog) throws Exception {
        logger.info("Processing Discard Dispense Medication");
        final RegistryErrorList registryErrorList = objectFactory.createRegistryErrorList();
        documentSubmitService.setSOAPHeader(soapHeader);

        //  Validate assertions according de Medication Discard Dispense rule:
        String sealCountryCode = null;
        try {
            final AssertionDetails hcpAssertion = validateAssertionsAndGetHCPAssertion(soapHeader);
            policyAssertionManager.xdrPermissionValidatorSubmitDocument(hcpAssertion.getAssertion());
            sealCountryCode = hcpAssertion.getCountryCode().orElse(null);
        } catch (final OpenNCPErrorCodeException e) {
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
        if (!policyAssertionManager.isConsentGiven(fullPatientId, countryCode)) {
            logger.debug("No consent given, throwing InsufficientRightsException");
            final NoConsentException e = new NoConsentException(null);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    e.getOpenncpErrorCode(),
                    e.getMessage(),
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        final RegistryResponseType response = new RegistryResponseType();
        String documentId = "";
        String discardId = "";
        String discardDate = "";

        try {
            final Document domDocument = DomUtils.byteToDocument(request.getDocuments().get(0).getValue());
            final EPSOSDocument epsosDocument = DocumentFactory.createEPSOSDocument(fullPatientId, ClassCode.ED_CLASSCODE, domDocument);
            documentId = getDocumentId(epsosDocument.getDocument());
            // Evidence for call to NI for XDR submit (dispensation)
            // Joao: here we have a Document, so we can generate the mandatory NRO

            final X509Certificate issuerCert = EvidenceUtils.getCertificate(Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                    Constants.NCP_SIG_PRIVATEKEY_ALIAS);
            final X509Certificate recipientCert = EvidenceUtils.getCertificate(Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD, Constants.SC_PRIVATEKEY_ALIAS);
            final X509Certificate senderCert = EvidenceUtils.getCertificate(Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD, Constants.SP_PRIVATEKEY_ALIAS);

            final PrivateKey key = EvidenceUtils.getSigningKey(Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                    Constants.NCP_SIG_PRIVATEKEY_ALIAS);

            try {
                EvidenceUtils.createEvidenceREMNRO(epsosDocument.getDocument(), issuerCert,
                        senderCert, recipientCert, key,
                        IHEEventType.DISPENSATION_SERVICE_INITIALIZE.getCode(), new DateTime(),
                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XDR_DISP_REQ",
                        Objects.requireNonNull(SoapElementHelper.getTRCAssertion(soapHeader)).getID() + "__" + DateUtil.getCurrentTimeGMT());
            } catch (final Exception e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }

            final List<JAXBElement<? extends IdentifiableType>> registryObjectList = request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables();
            if (registryObjectList != null) {
                for (final JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {

                    if (identifiable.getValue() instanceof ExtrinsicObjectType) {
                        final List<Slot> slotType1List = identifiable.getValue().getSlots();
                        for (final Slot slotType1 : slotType1List) {
                            if (StringUtils.equals(slotType1.getName(), "creationTime")) {
                                discardDate = slotType1.getValueList().getValues().get(0);
                            }
                        }
                    } else if (identifiable.getValue() instanceof RegistryPackageType) {
                        final RegistryPackageType registryPackageType = (RegistryPackageType) identifiable.getValue();
                        for (final ExternalIdentifierType externalIdentifier : registryPackageType.getExternalIdentifiers()) {
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
            discardDetails.setHealthCareProvider(SoapElementHelper.getAlternateUserID(soapHeader));
            discardDetails.setHealthCareProviderId(SoapElementHelper.getAssertionsSPProvidedId(soapHeader));
            discardDetails.setHealthCareProviderFacility(SoapElementHelper.getXSPALocality(soapHeader));
            discardDetails.setHealthCareProviderOrganization(SoapElementHelper.getOrganization(soapHeader));
            discardDetails.setHealthCareProviderOrganizationId(SoapElementHelper.getOrganizationId(soapHeader));
            documentSubmitService.cancelDispensation(discardDetails, epsosDocument);

        } catch (final NIException e) {
            logger.error("NIException: [{}] - [{}]", e.getOpenncpErrorCode(), e.getMessage());
            registryErrorList.getRegistryErrors().add(createErrorMessage(e.getOpenncpErrorCode(), e.getOpenncpErrorCode().getDescription() + "^" + e.getMessage(), "", Arrays.stream(ExceptionUtils.getRootCauseStackTrace(e)).findFirst().orElse(StringUtils.EMPTY), RegistryErrorSeverity.ERROR_SEVERITY_ERROR));
        } catch (final Exception e) {
            logger.error("Generic Exception: '{}'", e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    OpenNCPErrorCode.ERROR_ED_GENERIC,
                    e.getMessage(),
                    e,
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }

        if (registryErrorList.getRegistryErrors().isEmpty()) {
            response.setStatus(AdhocQueryResponseStatus.SUCCESS);
        } else {
            response.setRegistryErrorList(registryErrorList);
            response.setStatus(AdhocQueryResponseStatus.FAILURE);
        }
        prepareEventLogForDiscardMedication(eventLog, discardId, request, response, soapHeader);

        return response;
    }

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @return
     * @throws Exception
     */
    public RegistryResponseType saveDispensation(final DocumentSubmitInterface documentSubmitInterface, final ProvideAndRegisterDocumentSetRequest request, final SOAPHeader soapHeader,
                                                 final EventLog eventLog) throws Exception {
        logger.info("Processing Dispense Medication");
        final RegistryResponseType response = new RegistryResponseType();
        String sealCountryCode = null;

        documentSubmitInterface.setSOAPHeader(soapHeader);

        final RegistryErrorList registryErrorList = objectFactory.createRegistryErrorList();
        documentSubmitInterface.setSOAPHeader(soapHeader);

        try {
            final AssertionDetails hcpAssertion = validateAssertionsAndGetHCPAssertion(soapHeader);
            policyAssertionManager.xdrPermissionValidatorSubmitDocument(hcpAssertion.getAssertion());
            sealCountryCode = hcpAssertion.getCountryCode().orElse(null);
        } catch (final OpenNCPErrorCodeException e) {
            logger.error("OpenncpErrorCodeException: '{}'", e.getMessage(), e);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    e.getErrorCode(),
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
        if (!policyAssertionManager.isConsentGiven(fullPatientId, countryCode)) {
            logger.debug("No consent given, throwing InsufficientRightsException");
            final NoConsentException e = new NoConsentException(null);
            RegistryErrorUtils.addErrorMessage(
                    registryErrorList,
                    e.getOpenncpErrorCode(),
                    e.getMessage(),
                    RegistryErrorSeverity.ERROR_SEVERITY_ERROR);
        }
        if (!registryErrorList.getRegistryErrors().isEmpty()) {
            response.setRegistryErrorList(registryErrorList);
            response.setStatus(AdhocQueryResponseStatus.FAILURE);
        } else {
            for (int i = 0; i < request.getDocuments().size(); i++) {

                final ProvideAndRegisterDocumentSetRequest.Document doc = request.getDocuments().get(i);
                byte[] docBytes = doc.getValue();
                org.w3c.dom.Document domDocument = null;
                try {

                    //  Validate CDA epSOS Pivot.
                    if (GazelleValidation.isValidationEnable()) {
                        GazelleValidation.validateCdaDocument(new String(doc.getValue(), StandardCharsets.UTF_8),
                                NcpSide.NCP_A, obtainClassCode(request), true);
                    }

                    //  Reset the response document to a translated version.
                    final TMResponseStructure tmResponseStructure = cdaTransformationService.translate(DomUtils.byteToDocument(docBytes), Constants.LANGUAGE_CODE);
                    domDocument = Base64Util.decode(tmResponseStructure.getResponseCDA());
                    docBytes = XMLUtil.documentToByteArray(domDocument);


                    // Validate CDA epSOS Pivot
                    if (GazelleValidation.isValidationEnable()) {
                        GazelleValidation.validateCdaDocument(new String(docBytes, StandardCharsets.UTF_8), NcpSide.NCP_A,
                                obtainClassCode(request), false);
                    }
                } catch (final DocumentTransformationException ex) {
                    logger.error(ex.getLocalizedMessage(), ex);
                }

                try {
                    final EPSOSDocument epsosDocument = DocumentFactory.createEPSOSDocument(fullPatientId, ClassCode.ED_CLASSCODE, domDocument);

                    // Call to National Connector
                    final String documentId = getDocumentId(epsosDocument.getDocument());
                    documentSubmitInterface.submitDispensation(epsosDocument);
                } catch (final NIException e) {
                    logger.error("NIException: [{}] - [{}]", e.getOpenncpErrorCode(), e.getMessage());
                    registryErrorList.getRegistryErrors().add(createErrorMessage(e.getOpenncpErrorCode(), e.getOpenncpErrorCode().getDescription() + "^" + e.getMessage(), "", Arrays.stream(ExceptionUtils.getRootCauseStackTrace(e)).findFirst().orElse(StringUtils.EMPTY), RegistryErrorSeverity.ERROR_SEVERITY_ERROR));
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
            if (!registryErrorList.getRegistryErrors().isEmpty()) {
                response.setRegistryErrorList(registryErrorList);
                response.setStatus(AdhocQueryResponseStatus.FAILURE);
            } else {
                response.setStatus(AdhocQueryResponseStatus.SUCCESS);
            }
        }
        prepareEventLogForDispensationInitialize(eventLog, request, response, soapHeader);

        return response;
    }

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @return
     * @throws Exception
     */
    @Override
    public RegistryResponseType saveDocument(final ProvideAndRegisterDocumentSetRequest request, final SOAPHeader soapHeader,
                                             final EventLog eventLog) throws Exception {
        logger.info("[WS] XDR Service: Save Document");
        final DocumentSubmitInterface documentSubmitService = nationalConnectorFactory.createDocumentSubmitInstance();
        // Traverse all ExtrinsicObjects
        for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables().size(); i++) {

            if (!(request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables().get(i).getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType extrinsicObject = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiables().get(i).getValue();

            // Traverse all Classification blocks in the ExtrinsicObject selected
            for (final ClassificationType classification : extrinsicObject.getClassifications()) {

                logger.debug("[WS] XDR Service: Classification: '{}'-'{}'", classification.getClassificationScheme(), classification.getNodeRepresentation());
                if (StringUtils.equals(classification.getClassificationScheme(), IheConstants.FORMAT_CODE_SCHEME)) {

                    // Check the right LOINC code, currently coded as in example 3.4.2 ver. 2.2 p. 82
                    if (StringUtils.equals(classification.getNodeRepresentation(), "urn:epSOS:ep:dis:2010")) {
                        //  urn:epSOS:ep:dis:2010
                        return saveDispensation(documentSubmitService, request, soapHeader, eventLog);

                    } else if (StringUtils.equals(classification.getNodeRepresentation(), "urn:eHDSI:ed:discard:2020")) {
                        //  "urn:eHDSI:ed:discard:2020"
                        return discardMedicationDispensed(documentSubmitService, request, soapHeader, eventLog);
                    }
                }
            }
            break;
        }

        return reportDocumentTypeError(request);
    }

    public static RegistryResponseType reportDocumentTypeError(final ProvideAndRegisterDocumentSetRequest request) {

        final RegistryResponseType response = new RegistryResponseType();

        response.setRegistryErrorList(new RegistryErrorList());

        final RegistryError error = new RegistryError();
        error.setErrorCode(OpenNCPErrorCode.ERROR_UNKNOWN_SIGNIFIER.getCode());
        error.setCodeContext("Unknown document");
        response.getRegistryErrorList().getRegistryErrors().add(error);
        response.setStatus(AdhocQueryResponseStatus.FAILURE);

        return response;
    }

    /**
     * This method will extract the document class code from a given ProvideAndRegisterDocumentSetRequestType message.
     *
     * @param request the request containing the class code.
     * @return the class code.
     */
    private ClassCode obtainClassCode(final ProvideAndRegisterDocumentSetRequest request) {

        if (request == null) {
            logger.error("The provided request message in order to extract the classCode is null.");
            return null;
        }

        final String CLASS_SCHEME = XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_SCHEME;
        String result = "";

        for (int i = 0; i < request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables().size(); i++) {

            if (!(request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables().get(i).getValue()
                    instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType eot = (ExtrinsicObjectType) request.getSubmitObjectsRequest().getRegistryObjectList()
                    .getIdentifiables().get(i).getValue();

            for (int j = 0; j < eot.getClassifications().size(); j++) {

                if (eot.getClassifications().get(j).getClassificationScheme().equals(CLASS_SCHEME)) {
                    result = eot.getClassifications().get(j).getNodeRepresentation();
                    break;
                }
            }
        }

        if (result.isEmpty()) {
            logger.warn("No class code was found in request object.");
        }
        return ClassCode.getByCode(result);
    }

    private static String getDocumentId(final org.w3c.dom.Document document) {

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

}
