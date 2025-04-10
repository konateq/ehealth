package eu.europa.ec.sante.openncp.core.client.ihe.xca;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerException;
import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import eu.europa.ec.sante.openncp.core.client.ihe.IhePortTypeFactory;
import eu.europa.ec.sante.openncp.core.client.ihe.datamodel.AdhocQueryRequestCreator;
import eu.europa.ec.sante.openncp.core.client.ihe.datamodel.AdhocQueryResponseConverter;
import eu.europa.ec.sante.openncp.core.client.transformation.DomUtils;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.QueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.OpenNCPException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.Base64Util;
import eu.europa.ec.sante.openncp.core.common.tsam.error.TMError;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xca.RespondingGatewayPortType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * XCA Initiating Gateway
 * <p>
 * This is an implementation of a IHE XCA Initiation Gateway.
 * This class provides the necessary operations to query and retrieve documents.
 */
@Service
public class XcaGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(XcaGateway.class);
    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final List<String> TM_ERROR_CODES = Arrays.stream(TMError.values()).map(TMError::getCode).collect(Collectors.toList());

    private static final String ERROR_SEVERITY_ERROR = "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error";

    private final CDATransformationService cdaTransformationService;
    private final IhePortTypeFactory ihePortTypeFactory;
    private final DynamicDiscoveryService discoveryService;
    private final ConfigurationManager configurationManager;

    public XcaGateway(final CDATransformationService cdaTransformationService, final IhePortTypeFactory ihePortTypeFactory, final DynamicDiscoveryService discoveryService, final ConfigurationManager configurationManager) {
        this.cdaTransformationService = Validate.notNull(cdaTransformationService, "CDATransformationService must not be null");
        this.ihePortTypeFactory = Validate.notNull(ihePortTypeFactory, "ihePortTypeFactory must not be null");
        this.discoveryService = Validate.notNull(discoveryService, " discoveryService must not be null");
        this.configurationManager = Validate.notNull(configurationManager, "configurationManager must not be null");
    }

    public QueryResponse crossGatewayQuery(final PatientId pid, final String countryCode,
                                           final List<GenericDocumentCode> documentCodes,
                                           final FilterParams filterParams,
                                           final Map<AssertionType, Assertion> assertionMap,
                                           final String service) throws OpenNCPException {

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
            final RespondingGatewayPortType xcaPortType = getEndPointAndCreatePortType(countryCode, service);

            /* queryResponse */
            final List<ClassCode> documentClassCodes = new ArrayList<>();
            for (final GenericDocumentCode genericDocumentCode : documentCodes) {
                documentClassCodes.add(ClassCode.getByCode(genericDocumentCode.getValue()));
            }
            final AdhocQueryResponse queryResponse = xcaPortType.respondingGatewayCrossGatewayQuery(queryRequest);
            processRegistryErrors(queryResponse.getRegistryErrorList());

            if (queryResponse.getRegistryObjectList() != null) {
                result = AdhocQueryResponseConverter.convertAdhocQueryResponse(queryResponse);
            }
        } catch (final RuntimeException ex) {
            throw new RuntimeException(ex);
        }

        return result;
    }

    public RetrieveDocumentSetResponse.DocumentResponse crossGatewayRetrieve(final XDSDocument document, final String homeCommunityId,
                                                                             final String countryCode, final String targetLanguage,
                                                                             final Map<AssertionType, Assertion> assertionMap,
                                                                             final String service) throws OpenNCPException {

        LOGGER.info("QueryResponse crossGatewayQuery('{}','{}','{}','{}','{}', '{}')", homeCommunityId, countryCode,
                targetLanguage, assertionMap.get(AssertionType.HCP).getID(),
                assertionMap.get(AssertionType.TRC).getID(), service);
        RetrieveDocumentSetResponse.DocumentResponse result = null;
        final RetrieveDocumentSetResponse queryResponse;
        ClassCode classCode = null;

        try {

            final RetrieveDocumentSetRequest queryRequest = new RetrieveDocumentSetRequestCreator().createRetrieveDocumentSetRequestType(
                    document.getDocumentUniqueId(), homeCommunityId, document.getRepositoryUniqueId());
            final RespondingGatewayPortType xcaPortType = getEndPointAndCreatePortType(countryCode, service);

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
            queryResponse = xcaPortType.respondingGatewayCrossGatewayRetrieve(queryRequest);

            if (queryResponse.getRegistryResponse() != null) {

                final var registryErrorList = queryResponse.getRegistryResponse().getRegistryErrorList();
                processRegistryErrors(registryErrorList);
            }
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }

        if (!queryResponse.getDocumentResponses().isEmpty()) {
            if (queryResponse.getDocumentResponses().size() > 1) {
                LOGGER.error("More than one documents where retrieved for the current request with parameters document ID: '{}' " +
                        "- homeCommunityId: '{}' - registry: '{}'", document.getDocumentUniqueId(), homeCommunityId, document.getRepositoryUniqueId());
                //TODO: Shall be a fatal ERROR
            }
            // review this try - catch - finally mechanism and the transformation/translation mechanism.
            final byte[] pivotDocument = queryResponse.getDocumentResponses().get(0).getDocument();

            try {
                //  Validate CDA Pivot
                if (GazelleValidation.isValidationEnable()) {
                    GazelleValidation.validateCdaDocument(new String(pivotDocument, StandardCharsets.UTF_8),
                            NcpSide.NCP_B, ClassCode.getByCode(document.getClassCode().getValue()), true);
                }
                if (service.equals(Constants.OrCDService)) {
                    queryResponse.getDocumentResponses().get(0).setDocument(pivotDocument);
                } else {
                    //  Sets the response document to a translated version.
                    final var tmResponseStructure = cdaTransformationService.translate(DomUtils.byteToDocument(pivotDocument), targetLanguage, NcpSide.NCP_B);
                    final var domDocument = tmResponseStructure.getResponseCDA();
                    final byte[] translatedCDA = XMLUtil.documentToByteArray(Base64Util.decode(domDocument));
                    queryResponse.getDocumentResponses().get(0).setDocument(translatedCDA);
                }

            } catch (final Exception e) {
                LOGGER.warn("DocumentTransformationException: CDA cannot be translated: Please check the TM result");
            } finally {
                LOGGER.debug("[XCA Init Gateway] Returns Original Document");
                //  Validate CDA Friendly-B
                if (GazelleValidation.isValidationEnable()) {
                    GazelleValidation.validateCdaDocument(
                            new String(queryResponse.getDocumentResponses().get(0).getDocument(), StandardCharsets.UTF_8),
                            NcpSide.NCP_B, ClassCode.getByCode(document.getClassCode().getValue()), false);
                }
                //  Returns the original document, even if the translation process fails.
                result = queryResponse.getDocumentResponses().get(0);
            }
        }
        return result;
    }

    private RespondingGatewayPortType getEndPointAndCreatePortType(final String countryCode, final String service) {
        String endpointUrl = null;
        try {
            endpointUrl = discoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.fromName(service));
        } catch (final ConfigurationManagerException e) {
            throw new RuntimeException(e);
        }

        return ihePortTypeFactory.createXCAPort(configurationManager, endpointUrl);
    }

    /**
     * Processes registry errors from the {@link AdhocQueryResponse} message, by reporting them to the logging system.
     *
     * @param registryErrorList the list of errors from the {@link AdhocQueryResponse} message.
     * @throws OpenNCPException thrown when an error has a severity of type "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error".
     */
    private static void processRegistryErrors(final RegistryErrorList registryErrorList) throws OpenNCPException {
        // A.R. ++ Error processing. For retrieve. Is it needed?
        // We don't want to break on TSAM errors anyway...

        if (registryErrorList != null) {
            final List<RegistryError> errorList = registryErrorList.getRegistryErrors();

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
                    if (StringUtils.equals(ERROR_SEVERITY_ERROR, severity)
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
                    if (openncpErrorCode == null) {
                        LOGGER.warn("No EHDSI error code found in the XCA response for : " + errorCode);
                    }

                    //Throw all the remaining errors
                    if (hasError) {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("Registry Errors: '{}'", msg);
                        }
                        throw new OpenNCPException(openncpErrorCode, codeContext, location);
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
