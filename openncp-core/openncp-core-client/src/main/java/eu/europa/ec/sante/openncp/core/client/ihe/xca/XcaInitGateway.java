package eu.europa.ec.sante.openncp.core.client.ihe.xca;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.client.ihe.datamodel.AdhocQueryRequestCreator;
import eu.europa.ec.sante.openncp.core.client.ihe.datamodel.AdhocQueryResponseConverter;
import eu.europa.ec.sante.openncp.core.client.transformation.DomUtils;
import eu.europa.ec.sante.openncp.core.common.ihe.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.QueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryError;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryErrorList;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCAException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.Base64Util;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogClientUtil;
import eu.europa.ec.sante.openncp.core.common.tsam.error.TMError;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * XCA Initiating Gateway
 * <p>
 * This is an implementation of a IHE XCA Initiation Gateway.
 * This class provides the necessary operations to query and retrieve documents.
 *
 */
@Service
public class XcaInitGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(XcaInitGateway.class);
    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final List<String> TM_ERROR_CODES = Arrays.stream(TMError.values()).map(TMError::getCode).collect(Collectors.toList());

    private static final String ERROR_SEVERITY_ERROR = "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error";

    private final CDATransformationService cdaTransformationService;

    public XcaInitGateway(final CDATransformationService cdaTransformationService) {
        this.cdaTransformationService = Validate.notNull(cdaTransformationService, "CDATransformationService cannot be null");
    }

    public QueryResponse crossGatewayQuery(final PatientId pid, final String countryCode,
                                                  final List<GenericDocumentCode> documentCodes,
                                                  final FilterParams filterParams,
                                                  final Map<AssertionType, Assertion> assertionMap,
                                                  final String service) throws XCAException {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("[");
            documentCodes.forEach(s -> {
                builder.append(s.getValue()).append(",");
            });
            builder.replace(builder.length() - 1, builder.length(), "]");
            final String classCodes = builder.toString();
            LOGGER_CLINICAL.info("QueryResponse crossGatewayQuery('{}','{}','{}','{}','{}','{}')", pid.getExtension(), countryCode,
                    classCodes, assertionMap.get(AssertionType.HCP).getID(), assertionMap.get(AssertionType.TRC).getID(), service);
            if (filterParams != null) {
                LOGGER_CLINICAL.info("FilterParams created Before: " + filterParams.getCreatedBefore());
                LOGGER_CLINICAL.info("FilterParams created After: " + filterParams.getCreatedAfter());
                LOGGER_CLINICAL.info("FilterParams size : " + filterParams.getMaximumSize());
            }
        }
        QueryResponse result = null;

        try {

            /* queryRequest */
            final AdhocQueryRequest queryRequest = AdhocQueryRequestCreator.createAdhocQueryRequest(pid.getExtension(), pid.getRoot(), documentCodes, filterParams);

            /* Stub */
            final var respondingGatewayStub = new RespondingGateway_ServiceStub();
            final var dynamicDiscoveryService = new DynamicDiscoveryService();
            final String epr = dynamicDiscoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.fromName(service));
            respondingGatewayStub.setAddr(epr);
            respondingGatewayStub._getServiceClient().getOptions().setTo(new EndpointReference(epr));
            EventLogClientUtil.createDummyMustUnderstandHandler(respondingGatewayStub);
            respondingGatewayStub.setCountryCode(countryCode);

            /* queryResponse */
            final List<ClassCode> documentClassCodes = new ArrayList<>();
            for (final GenericDocumentCode genericDocumentCode : documentCodes) {
                documentClassCodes.add(ClassCode.getByCode(genericDocumentCode.getValue()));
            }
            final AdhocQueryResponse queryResponse = respondingGatewayStub.respondingGateway_CrossGatewayQuery(queryRequest, assertionMap, documentClassCodes);
            processRegistryErrors(queryResponse.getRegistryErrorList());

            if (queryResponse.getRegistryObjectList() != null) {
                result = AdhocQueryResponseConverter.convertAdhocQueryResponse(queryResponse);
            }
        } catch (final RemoteException | RuntimeException ex) {
            throw new RuntimeException(ex);
        }

        return result;
    }

    public RetrieveDocumentSetResponseType.DocumentResponse crossGatewayRetrieve(final XDSDocument document, final String homeCommunityId,
                                                                                 final String countryCode, final String targetLanguage,
                                                                                 final Map<AssertionType, Assertion> assertionMap,
                                                                                 final String service) throws XCAException {

        LOGGER.info("QueryResponse crossGatewayQuery('{}','{}','{}','{}','{}', '{}')", homeCommunityId, countryCode,
                targetLanguage, assertionMap.get(AssertionType.HCP).getID(),
                assertionMap.get(AssertionType.TRC).getID(), service);
        RetrieveDocumentSetResponseType.DocumentResponse result = null;
        final RetrieveDocumentSetResponseType queryResponse;
        ClassCode classCode = null;

        try {

            final RetrieveDocumentSetRequestType queryRequest = new RetrieveDocumentSetRequestTypeCreator().createRetrieveDocumentSetRequestType(
                    document.getDocumentUniqueId(), homeCommunityId, document.getRepositoryUniqueId());

            final RespondingGateway_ServiceStub stub = new RespondingGateway_ServiceStub();
            final DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
            final String endpointReference = dynamicDiscoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.fromName(service));
            stub.setAddr(endpointReference);
            stub._getServiceClient().getOptions().setTo(new EndpointReference(endpointReference));
            stub.setCountryCode(countryCode);
            EventLogClientUtil.createDummyMustUnderstandHandler(stub);
            // This is a rather dirty hack, but document.getClassCode() returns null for some reason.
            switch (service) {
                case Constants.OrderService:
                case Constants.PatientService:
                case Constants.OrCDService:
                    classCode = ClassCode.getByCode(document.getClassCode().getValue());
                    break;
                default:
                    LOGGER.error("Service Not Supported");
                    //TODO: Has to be managed as an error.
            }
            queryResponse = stub.respondingGateway_CrossGatewayRetrieve(queryRequest, assertionMap, classCode);

            if (queryResponse.getRegistryResponse() != null) {

                final var registryErrorList = queryResponse.getRegistryResponse().getRegistryErrorList();
                processRegistryErrors(registryErrorList);
            }
        } catch (final RemoteException ex) {
            throw new RuntimeException(ex);
        }

        if (!queryResponse.getDocumentResponse().isEmpty()) {
            if (queryResponse.getDocumentResponse().size() > 1) {
                LOGGER.error("More than one documents where retrieved for the current request with parameters document ID: '{}' " +
                        "- homeCommunityId: '{}' - registry: '{}'", document.getDocumentUniqueId(), homeCommunityId, document.getRepositoryUniqueId());
                //TODO: Shall be a fatal ERROR
            }
            // review this try - catch - finally mechanism and the transformation/translation mechanism.
            final byte[] pivotDocument = queryResponse.getDocumentResponse().get(0).getDocument();

            try {
                //  Validate CDA Pivot
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(new String(pivotDocument, StandardCharsets.UTF_8),
                            NcpSide.NCP_B, ClassCode.getByCode(document.getClassCode().getValue()), true);
                }
                if (service.equals(Constants.OrCDService)) {
                    queryResponse.getDocumentResponse().get(0).setDocument(pivotDocument);
                } else {
                    //  Sets the response document to a translated version.
                    final var tmResponseStructure = cdaTransformationService.translate(DomUtils.byteToDocument(pivotDocument), targetLanguage, NcpSide.NCP_B);
                    final var domDocument = tmResponseStructure.getResponseCDA();
                    final byte[] translatedCDA = XMLUtils.toOM(Base64Util.decode(domDocument).getDocumentElement()).toString().getBytes(StandardCharsets.UTF_8);
                    queryResponse.getDocumentResponse().get(0).setDocument(translatedCDA);

                }

            } catch (final Exception e) {
                LOGGER.warn("DocumentTransformationException: CDA cannot be translated: Please check the TM result");
            } finally {
                LOGGER.debug("[XCA Init Gateway] Returns Original Document");
                //  Validate CDA Friendly-B
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(
                            new String(queryResponse.getDocumentResponse().get(0).getDocument(), StandardCharsets.UTF_8),
                            NcpSide.NCP_B, ClassCode.getByCode(document.getClassCode().getValue()), false);
                }
                //  Returns the original document, even if the translation process fails.
                result = queryResponse.getDocumentResponse().get(0);
            }
        }
        return result;
    }

    /**
     * Processes registry errors from the {@link AdhocQueryResponse} message, by reporting them to the logging system.
     *
     * @param registryErrorList the list of errors from the {@link AdhocQueryResponse} message.
     * @throws XCAException thrown when an error has a severity of type "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error".
     */
    private static void processRegistryErrors(final RegistryErrorList registryErrorList) throws XCAException {
        // A.R. ++ Error processing. For retrieve. Is it needed?
        // We don't want to break on TSAM errors anyway...

        if (registryErrorList != null) {
            final List<RegistryError> errorList = registryErrorList.getRegistryError();

            if (errorList != null) {
                final StringBuilder msg = new StringBuilder();
                boolean hasError = false;
                for (final RegistryError error : errorList) {
                    final String errorCode = error.getErrorCode();
                    final String value = error.getValue();
                    final String location = error.getLocation();
                    final String severity = error.getSeverity();
                    final String codeContext = error.getCodeContext();
                    LOGGER.debug("\nerrorCode='{}'\ncodeContext='{}'\nlocation='{}'\nseverity='{}'\n'{}'\n",
                            errorCode, codeContext, location, severity, value);

                    // Marcelo Fonseca: Added error situation where no document is found or registered, 1101/1102.
                    // (Needs to be revised according to new error communication strategy to the portal).
                    if (StringUtils.equals(ERROR_SEVERITY_ERROR,severity)
                            || errorCode.equals(OpenNCPErrorCode.ERROR_EP_NOT_FOUND.getCode())
                            || errorCode.equals(OpenNCPErrorCode.ERROR_PS_NOT_FOUND.getCode())
                                || errorCode.equals(OpenNCPErrorCode.ERROR_EP_REGISTRY_NOT_ACCESSIBLE.getCode())) {
                            msg.append(errorCode).append(" ").append(codeContext).append(" ").append(value);
                            hasError = true;
                    }

                    // Avoid the transformation errors to abort process - this way they are only logged in the upper instructions
                    if (checkTransformationErrors(errorCode)) {
                        continue;
                    }

                    final OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.getErrorCode(errorCode);
                    if(openncpErrorCode == null){
                        LOGGER.warn("No EHDSI error code found in the XCA response for : " + errorCode);
                    }

                    //Throw all the remaining errors
                    if (hasError) {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("Registry Errors: '{}'", msg);
                        }
                        throw new XCAException(openncpErrorCode, codeContext, location);
                    }
                }
            }
        }
    }

    /**
     * This method will check if a given code is related to the document transformation errors
     *
     * @param errorCode Error Code associated to the action performed.
     * @return True | false according the Error Codes List.
     */
    private static boolean checkTransformationErrors(final String errorCode) {
        return TM_ERROR_CODES.contains(errorCode);
    }
}
