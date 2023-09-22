package eu.epsos.pt.ws.client.xca;

import ee.affecto.epsos.util.EventLogClientUtil;
import eu.epsos.dts.xds.AdhocQueryRequestCreator;
import eu.epsos.dts.xds.AdhocQueryResponseConverter;
import eu.epsos.exceptions.DocumentTransformationException;
import eu.epsos.exceptions.XCAException;
import eu.epsos.pt.transformation.DomUtils;
import eu.epsos.pt.transformation.TranslationsAndMappingsClient;
import eu.europa.ec.sante.ehdsi.constant.ClassCode;
import eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode;
import eu.europa.ec.sante.ehdsi.constant.error.TMError;
import eu.europa.ec.sante.ehdsi.openncp.pt.common.RegistryErrorSeverity;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.RegisteredService;
import eu.europa.ec.sante.ehdsi.openncp.pt.common.DynamicDiscoveryService;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TMResponseStructure;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import eu.europa.ec.sante.ehdsi.constant.assertion.AssertionEnum;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType.DocumentResponse;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.util.XMLUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import tr.com.srdc.epsos.data.model.FilterParams;
import tr.com.srdc.epsos.data.model.GenericDocumentCode;
import tr.com.srdc.epsos.data.model.PatientId;
import tr.com.srdc.epsos.data.model.xds.QueryResponse;
import tr.com.srdc.epsos.data.model.xds.XDSDocument;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.ws.xca.client.RespondingGateway_ServiceStub;
import tr.com.srdc.epsos.ws.xca.client.retrieve.RetrieveDocumentSetRequestTypeCreator;

import java.io.IOException;
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
 * @author Luís Pinto<code> - luis.pinto@iuz.pt</code>
 * @author Marcelo Fonseca<code> - marcelo.fonseca@iuz.pt</code>
 */
public class XcaInitGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(XcaInitGateway.class);
    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final List<String> TM_ERROR_CODES = Arrays.stream(TMError.values()).map(TMError::getCode).collect(Collectors.toList());

    /**
     * Private constructor to disable class instantiation.
     */
    private XcaInitGateway() {
    }

    public static QueryResponse crossGatewayQuery(final PatientId pid, final String countryCode,
                                                  final List<GenericDocumentCode> documentCodes,
                                                  final FilterParams filterParams,
                                                  final Map<AssertionEnum, Assertion> assertionMap,
                                                  final String service) throws XCAException {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("[");
            documentCodes.forEach(s -> {
                builder.append(s.getValue()).append(",");
            });
            builder.replace(builder.length() - 1, builder.length(), "]");
            String classCodes = builder.toString();
            LOGGER_CLINICAL.info("QueryResponse crossGatewayQuery('{}','{}','{}','{}','{}','{}')", pid.getExtension(), countryCode,
                    classCodes, assertionMap.get(AssertionEnum.CLINICIAN).getID(), assertionMap.get(AssertionEnum.TREATMENT).getID(), service);
            if (filterParams != null) {
                LOGGER_CLINICAL.info("FilterParams created Before: " + filterParams.getCreatedBefore());
                LOGGER_CLINICAL.info("FilterParams created After: " + filterParams.getCreatedAfter());
                LOGGER_CLINICAL.info("FilterParams size : " + filterParams.getMaximumSize());
            }
        }
        QueryResponse result = null;

        try {

            /* queryRequest */
            AdhocQueryRequest queryRequest = AdhocQueryRequestCreator.createAdhocQueryRequest(pid.getExtension(), pid.getRoot(), documentCodes, filterParams);

            /* Stub */
            var respondingGatewayStub = new RespondingGateway_ServiceStub();
            var dynamicDiscoveryService = new DynamicDiscoveryService();
            String epr = dynamicDiscoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.fromName(service));
            respondingGatewayStub.setAddr(epr);
            respondingGatewayStub._getServiceClient().getOptions().setTo(new EndpointReference(epr));
            EventLogClientUtil.createDummyMustUnderstandHandler(respondingGatewayStub);
            respondingGatewayStub.setCountryCode(countryCode);

            /* queryResponse */
            List<ClassCode> documentClassCodes = new ArrayList<>();
            for (GenericDocumentCode genericDocumentCode : documentCodes) {
                documentClassCodes.add(ClassCode.getByCode(genericDocumentCode.getValue()));
            }
            AdhocQueryResponse queryResponse = respondingGatewayStub.respondingGateway_CrossGatewayQuery(queryRequest, assertionMap, documentClassCodes);
            processRegistryErrors(queryResponse.getRegistryErrorList());

            if (queryResponse.getRegistryObjectList() != null) {
                result = AdhocQueryResponseConverter.convertAdhocQueryResponse(queryResponse);
            }
        } catch (RemoteException | RuntimeException ex) {
            throw new RuntimeException(ex);
        }

        return result;
    }

    public static DocumentResponse crossGatewayRetrieve(final XDSDocument document, final String homeCommunityId,
                                                        final String countryCode, final String targetLanguage,
                                                        final Map<AssertionEnum, Assertion> assertionMap,
                                                        String service) throws XCAException {

        LOGGER.info("QueryResponse crossGatewayQuery('{}','{}','{}','{}','{}', '{}')", homeCommunityId, countryCode,
                targetLanguage, assertionMap.get(AssertionEnum.CLINICIAN).getID(),
                assertionMap.get(AssertionEnum.TREATMENT).getID(), service);
        DocumentResponse result = null;
        RetrieveDocumentSetResponseType queryResponse;
        ClassCode classCode = null;

        try {

            RetrieveDocumentSetRequestType queryRequest = new RetrieveDocumentSetRequestTypeCreator().createRetrieveDocumentSetRequestType(
                    document.getDocumentUniqueId(), homeCommunityId, document.getRepositoryUniqueId());

            RespondingGateway_ServiceStub stub = new RespondingGateway_ServiceStub();
            DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
            String endpointReference;
            if (service.equals(Constants.MroService)) {

                endpointReference = dynamicDiscoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.PATIENT_SERVICE);
            } else {

                endpointReference = dynamicDiscoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.fromName(service));
            }
            stub.setAddr(endpointReference);
            stub._getServiceClient().getOptions().setTo(new EndpointReference(endpointReference));
            stub.setCountryCode(countryCode);
            EventLogClientUtil.createDummyMustUnderstandHandler(stub);
            // This is a rather dirty hack, but document.getClassCode() returns null for some reason.
            switch (service) {
                case Constants.OrderService:
                case Constants.PatientService:
                case Constants.MroService:
                case Constants.OrCDService:
                    classCode = ClassCode.getByCode(document.getClassCode().getValue());
                    break;
                default:
                    LOGGER.error("Service Not Supported");
                    //TODO: Has to be managed as an error.
            }
            queryResponse = stub.respondingGateway_CrossGatewayRetrieve(queryRequest, assertionMap, classCode);

            if (queryResponse.getRegistryResponse() != null) {

                var registryErrorList = queryResponse.getRegistryResponse().getRegistryErrorList();
                processRegistryErrors(registryErrorList);
            }
        } catch (RemoteException ex) {
            throw new RuntimeException(ex);
        }

        if (!queryResponse.getDocumentResponse().isEmpty()) {
            if (queryResponse.getDocumentResponse().size() > 1) {
                LOGGER.error("More than one documents where retrieved for the current request with parameters document ID: '{}' " +
                        "- homeCommunityId: '{}' - registry: '{}'", document.getDocumentUniqueId(), homeCommunityId, document.getRepositoryUniqueId());
                //TODO: Shall be a fatal ERROR
            }
            //TODO: review this try - catch - finally mechanism and the transformation/translation mechanism.
            byte[] pivotDocument = queryResponse.getDocumentResponse().get(0).getDocument();
            byte[] friendlyDocument;

            try {
                //  Validate CDA Pivot
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateCdaDocument(new String(pivotDocument, StandardCharsets.UTF_8),
                            NcpSide.NCP_B, ClassCode.getByCode(document.getClassCode().getValue()), true);
                }
                if (service.equals(Constants.OrCDService)) {
                    queryResponse.getDocumentResponse().get(0).setDocument(pivotDocument);
                } else {
                    //  Resets the response document to a translated version.

                    var tmResponseStructure = TranslationsAndMappingsClient.translate(DomUtils.byteToDocument(pivotDocument), targetLanguage);
                    var domDocument = tmResponseStructure.getResponseCDA();
                    byte[] translatedCDA = XMLUtils.toOM(domDocument.getDocumentElement()).toString().getBytes(StandardCharsets.UTF_8);
                    queryResponse.getDocumentResponse().get(0).setDocument(translatedCDA);
                }

            } catch (Exception e) {
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
    private static void processRegistryErrors(RegistryErrorList registryErrorList) throws XCAException {
        // A.R. ++ Error processing. For retrieve. Is it needed?
        // We don't want to break on TSAM errors anyway...

        if (registryErrorList != null) {
            List<RegistryError> errorList = registryErrorList.getRegistryError();

            if (errorList != null) {
                StringBuilder msg = new StringBuilder();
                boolean hasError = false;
                for (RegistryError error : errorList) {
                    String errorCode = error.getErrorCode();
                    String value = error.getValue();
                    String location = error.getLocation();
                    String severity = error.getSeverity();
                    String codeContext = error.getCodeContext();
                    LOGGER.debug("\nerrorCode='{}'\ncodeContext='{}'\nlocation='{}'\nseverity='{}'\n'{}'\n",
                            errorCode, codeContext, location, severity, value);

                    // Marcelo Fonseca: Added error situation where no document is found or registered, 1101/1102.
                    // (Needs to be revised according to new error communication strategy to the portal).
                    if (RegistryErrorSeverity.ERROR_SEVERITY_ERROR.getText().equals(severity)
                            || errorCode.equals(OpenNCPErrorCode.ERROR_EP_NOT_FOUND.getCode())
                            || errorCode.equals(OpenNCPErrorCode.ERROR_PS_NOT_FOUND.getCode())) {
                        msg.append(errorCode).append(" ").append(codeContext).append(" ").append(value);
                        hasError = true;
                    }

                    // Avoid the transformation errors to abort process - this way they are only logged in the upper instructions
                    if (checkTransformationErrors(errorCode)) {
                        continue;
                    }

                    OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.getErrorCode(errorCode);
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
    private static boolean checkTransformationErrors(String errorCode) {
        return TM_ERROR_CODES.contains(errorCode);
    }
}
