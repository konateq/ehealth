package eu.europa.ec.sante.openncp.core.client.ihe.xca;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.HttpsClientConfiguration;
import eu.europa.ec.sante.openncp.core.common.constants.ihe.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcEntry;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtilWrapper;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.ServiceType;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCAException;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogClientUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogUtil;
import eu.europa.ec.sante.openncp.core.common.util.OidUtil;
import org.apache.axiom.om.*;
import org.apache.axiom.om.ds.AbstractOMDataSource;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.util.XMLUtils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;

/*
 *  RespondingGateway_ServiceStub java implementation
 */
public class RespondingGateway_ServiceStub extends Stub {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespondingGateway_ServiceStub.class);
    private static final JAXBContext wsContext;
    private static int counter = 0;

    static {

        LOGGER.debug("Loading the WS-Security init libraries in RespondingGateway_ServiceStub xca");
        org.apache.xml.security.Init.init();
    }

    static {

        JAXBContext jc;
        jc = null;
        try {
            jc = JAXBContext.newInstance(AdhocQueryRequest.class,
                    AdhocQueryResponse.class, RetrieveDocumentSetRequestType.class,
                    RetrieveDocumentSetResponseType.class);
        } catch (final JAXBException ex) {
            LOGGER.error(XCAConstants.EXCEPTIONS.UNABLE_CREATE_JAXB_CONTEXT + " " + ex.getMessage(), ex);
            Runtime.getRuntime().exit(-1);
        } finally {
            wsContext = jc;
        }
    }

    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    //hashmaps to keep the fault mapping
    private final HashMap faultExceptionNameMap = new HashMap();
    private final HashMap faultExceptionClassNameMap = new HashMap();
    private final HashMap faultMessageMap = new HashMap();
    private final QName[] opNameArray = null;
    protected AxisOperation[] _operations;
    private String addr;
    private String countryCode;
    private Date transactionStartTime;
    private Date transactionEndTime;

    /**
     * Constructor that takes in a configContext
     */
    public RespondingGateway_ServiceStub(final ConfigurationContext configurationContext, final String targetEndpoint) throws AxisFault {

        this(configurationContext, targetEndpoint, false);
    }

    /**
     * Constructor that takes in a configContext and use separate listener
     */
    public RespondingGateway_ServiceStub(final ConfigurationContext configurationContext, final String targetEndpoint, final boolean useSeparateListener) throws AxisFault {

        //To populate AxisService
        populateAxisService();
        populateFaults();

        _serviceClient = new ServiceClient(configurationContext, _service);
        _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(targetEndpoint));
        _serviceClient.getOptions().setUseSeparateListener(useSeparateListener);
        //  Wait time after which a client times out in a blocking scenario: 3 minutes
        _serviceClient.getOptions().setTimeOutInMilliSeconds(180000);
        //Set the soap version
        _serviceClient.getOptions().setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);

        // Enabling Axis2 - SSL 2 ways communication (not active by default).
        try {
            _serviceClient.getServiceContext().getConfigurationContext()
                    .setProperty(HTTPConstants.CACHED_HTTP_CLIENT, HttpsClientConfiguration.getDefaultSSLClient());
            _serviceClient.getServiceContext().getConfigurationContext()
                    .setProperty(HTTPConstants.REUSE_HTTP_CLIENT, false);
        } catch (final NoSuchAlgorithmException | KeyManagementException | IOException | CertificateException |
                       KeyStoreException | UnrecoverableKeyException e) {
            throw new RuntimeException("SSL Context cannot be initialized");
        }
    }

    /**
     * Default Constructor
     */
    public RespondingGateway_ServiceStub(final ConfigurationContext configurationContext) throws AxisFault {

        this(configurationContext, "http://195.142.27.167:8111/tr-xca/services/RespondingGateway_Service");
    }

    /**
     * Default Constructor
     */
    public RespondingGateway_ServiceStub() throws AxisFault {

        this("http://195.142.27.167:8111/tr-xca/services/RespondingGateway_Service");
    }

    /**
     * Constructor taking the target endpoint
     */
    public RespondingGateway_ServiceStub(final String targetEndpoint) throws AxisFault {
        this(null, targetEndpoint);
    }

    private static synchronized String getUniqueSuffix() {

        // reset the counter if it is greater than 99999
        if (counter > 99999) {
            counter = 0;
        }
        counter++;
        return System.currentTimeMillis() + "_" + counter;
    }

    public void setAddr(final String addr) {
        this.addr = addr;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    private void populateAxisService() {

        //creating the Service with a unique name
        _service = new AxisService(XCAConstants.RESPONDING_GATEWAY_SERVICE + getUniqueSuffix());
        addAnonymousOperations();
        //creating the operations
        AxisOperation __operation;
        _operations = new AxisOperation[2];
        __operation = new OutInAxisOperation();
        __operation.setName(new QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.QUERY.NAMESPACE_REQUEST_LOCAL_PART));
        _service.addOperation(__operation);
        _operations[0] = __operation;
        __operation = new OutInAxisOperation();
        __operation.setName(new QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.RETRIEVE.NAMESPACE_REQUEST_LOCAL_PART));
        _service.addOperation(__operation);
        _operations[1] = __operation;
    }

    /**
     * Populates the faults
     */
    private void populateFaults() {
        // Not implemented in eHDSI-OpenNCP.
    }

    /**
     * Auto generated method signature
     *
     * @param adhocQueryRequest
     */
    public AdhocQueryResponse respondingGateway_CrossGatewayQuery(final AdhocQueryRequest adhocQueryRequest,
                                                                  final Map<AssertionType, Assertion> assertionMap,
                                                                  final List<ClassCode> classCodes)
            throws java.rmi.RemoteException, XCAException {

        String eadcError = "";

        MessageContext _messageContext = null;
        MessageContext _returnMessageContext = null;
        try {
            // TMP
            // XCA list request start time
            long start = System.currentTimeMillis();

            OperationClient _operationClient = _serviceClient.createClient(_operations[0].getName());
            _operationClient.getOptions().setAction(XCAConstants.SOAP_HEADERS.QUERY.REQUEST_ACTION);
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            addPropertyToOperationClient(_operationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

            // create a message context
            _messageContext = new MessageContext();

            // create SOAP envelope with that payload
            SOAPEnvelope env;
            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    adhocQueryRequest,
                    optimizeContent(new QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.QUERY.NAMESPACE_REQUEST_LOCAL_PART)));

            /*
             * adding SOAP soap_headers
             */
            final SOAPFactory soapFactory = getFactory(_operationClient.getOptions().getSoapVersionURI());
            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final OMNamespace ns2 = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.QUERY.OM_NAMESPACE, "addressing");

            final SOAPHeaderBlock action = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("Action", ns2);
            final OMNode node = factory.createOMText(XCAConstants.SOAP_HEADERS.QUERY.REQUEST_ACTION);
            action.addChild(node);
            final OMAttribute att1 = factory.createOMAttribute(XCAConstants.SOAP_HEADERS.MUST_UNDERSTAND, env.getNamespace(), "1");
            action.addAttribute(att1);

            final SOAPHeaderBlock id = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("MessageID", ns2);
            final OMNode node2 = factory.createOMText(Constants.UUID_PREFIX + UUID.randomUUID());
            id.addChild(node2);

            SOAPHeaderBlock to = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("To", ns2);
            OMNode node3 = factory.createOMText(addr);
            to.addChild(node3);
            final OMAttribute att2 = factory.createOMAttribute(XCAConstants.SOAP_HEADERS.MUST_UNDERSTAND, env.getNamespace(), "1");
            to.addAttribute(att2);

            final SOAPHeaderBlock replyTo = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("ReplyTo", ns2);
            final OMElement address = OMAbstractFactory.getSOAP12Factory().createOMElement("Address", ns2);
            final OMNode node4 = factory.createOMText(XCAConstants.SOAP_HEADERS.ADDRESSING_NAMESPACE);
            address.addChild(node4);
            replyTo.addChild(address);

            /* We are manually adding all WSA headers (To, Action, MessageID). Other IHE service clients (XCPD and XDR) skip the addition of the To header
            and let the client-connector axis2 configurations take care of it (through the global engaging of WS-Addressing module in axis2.xml, which sets
            the To with the endpoint value from the transport information), but we cannot assume that these IHE Service clients will always be coupled with
            the client-connector (and that it'll always be based on Axis2). See issues EHNCP-1141 and EHNCP-1168.
            */
            _serviceClient.addHeader(to);
            _serviceClient.addHeader(action);
            _serviceClient.addHeader(id);
            _serviceClient.addHeader(replyTo);

            final var omNamespace = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.SECURITY_XSD, "wsse");
            final var headerSecurity = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("Security", omNamespace);

            try {
                if (assertionMap.containsKey(AssertionType.NOK)) {
                    final var assertionNextOfKin = assertionMap.get(AssertionType.NOK);
                    headerSecurity.addChild(XMLUtils.toOM(assertionNextOfKin.getDOM()));
                }
                final var assertionId = assertionMap.get(AssertionType.HCP);
                headerSecurity.addChild(XMLUtils.toOM(assertionId.getDOM()));
                final var assertionTreatment = assertionMap.get(AssertionType.TRC);
                headerSecurity.addChild(XMLUtils.toOM(assertionTreatment.getDOM()));

                _serviceClient.addHeader(headerSecurity);
            } catch (final Exception ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }

            _serviceClient.addHeadersToEnvelope(env);

            /*
             * Prepare request
             */
            _messageContext.setEnvelope(env);   // set the message context with that soap envelope
            _operationClient.addMessageContext(_messageContext);    // add the message contxt to the operation client

            /* Log soap request */
            final String logRequestBody;
            try {
                if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                    final String logRequestMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(env));
                    loggerClinical.debug("{}\n{}", XCAConstants.LOG.OUTGOING_XCA_QUERY_MESSAGE, logRequestMsg);
                }
                logRequestBody = XMLUtil.prettyPrint(XMLUtils.toDOM(env.getBody().getFirstElement()));
            } catch (final Exception ex) {
                throw new XCAException(OpenNCPErrorCode.ERROR_GENERIC, ex.getMessage(), null);
            }
            // NRO
//                try {
//                    EvidenceUtils.createEvidenceREMNRO(envCanonicalized,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                            EventType.epsosOrderServiceList.getCode(),
//                            new DateTime(),
//                            EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                            "NCPB_XCA_LIST_REQ");
//                } catch (Exception e) {
//                    LOGGER.error(ExceptionUtils.getStackTrace(e));
//                }

            // TMP
            // XCA List response end time
            long end = System.currentTimeMillis();
            LOGGER.info("XCA LIST REQUEST-RESPONSE TIME: '{}' ms", (end - start) / 1000.0);

            // TMP
            // Validation start time
            start = System.currentTimeMillis();

            /* Validate Request Message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCrossCommunityAccess(logRequestBody, NcpSide.NCP_B, classCodes);
            }

            // TMP
            // Transaction end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST VALIDATION REQ TIME: '{}' ms", (end - start) / 1000.0);

            // TMP
            // Transaction start time
            start = System.currentTimeMillis();

            /*
             * Execute Operation
             */
            transactionStartTime = new Date();
            try {
                _operationClient.execute(true);
            } catch (final AxisFault e) {
                LOGGER.error("Axis Fault error: '{}'", e.getMessage());
                LOGGER.error("Trying to automatically solve the problem by fetching configurations from the Central Services...");
                LOGGER.debug("ClassCode: '{}'", Arrays.toString(classCodes.toArray()));
                final DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
                final RegisteredService registeredService = getRegisteredService(classCodes);
                final String endpoint = dynamicDiscoveryService.getEndpointUrl(
                        this.countryCode.toLowerCase(Locale.ENGLISH), registeredService, true);

                if (StringUtils.isNotEmpty(endpoint)) {

                    /* if we get something from the Central Services, then we retry the request */
                    /* correctly sets the Transport information with the new endpoint */
                    LOGGER.debug("Retrying the request with the new configurations: [{}]", endpoint);
                    _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(endpoint));

                    /* we need a new OperationClient, otherwise we'll face the error "A message was added that is not valid. However, the operation context was complete." */
                    final OperationClient newOperationClient = _serviceClient.createClient(_operations[0].getName());
                    newOperationClient.getOptions().setAction(XCAConstants.SOAP_HEADERS.QUERY.REQUEST_ACTION);
                    newOperationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
                    addPropertyToOperationClient(newOperationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

                    final SOAPFactory newSoapFactory = getFactory(newOperationClient.getOptions().getSoapVersionURI());

                    /* As there's no simple way of replacing the previously set WSA To header (still containing the old endpoint) we need to create a new SOAP
                    payload so that the wsa:To header is correctly set. Here we have 2 situations. If we rely on the client-connector axis2.xml configurations,
                    then the new endpoint will be copied from the Transport information to the wsa:To during the running of the Addressing Phase,
                    as defined by the global engagement of the addressing module in axis2.xml. But we're not doing it since these IHE service clients should be
                    decoupled from client-connector, thus we manually add the new WSA To header to the new SOAP envelope. See issues EHNCP-1141 and EHNCP-1168. */
                    final SOAPEnvelope newEnv;
                    newEnv = toEnvelope(newSoapFactory,
                            adhocQueryRequest,
                            optimizeContent(new QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.QUERY.NAMESPACE_REQUEST_LOCAL_PART)));

                    /* Creating the new WSA To header with the new endpoint */
                    to = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("To", ns2);
                    node3 = newSoapFactory.createOMText(endpoint);
                    to.addChild(node3);
                    to.addAttribute(att2);

                    /* There's no way to remove a single header (and WS-Addressing specification forbids the existence of
                    more than 1 addressing:To header), so we need to remove all the old headers and add them again to _serviceClient
                    (including the new To header with the right endpoint), from which they'll be copied into the final SOAP envelope */
                    _serviceClient.removeHeaders();
                    _serviceClient.addHeader(to);
                    _serviceClient.addHeader(action);
                    _serviceClient.addHeader(id);
                    _serviceClient.addHeader(replyTo);
                    _serviceClient.addHeader(headerSecurity);
                    _serviceClient.addHeadersToEnvelope(newEnv);

                    /* we create a new Message Context with the new SOAP envelope */
                    final MessageContext newMessageContext = new MessageContext();
                    newMessageContext.setEnvelope(newEnv);

                    /* add the new message context to the new operation client */
                    newOperationClient.addMessageContext(newMessageContext);

                    /* we need to reset the previous variables with the new content, to be used later */
                    _operationClient = newOperationClient;
                    _messageContext = newMessageContext;
                    env = newEnv;

                    /* we retry the request */
                    newOperationClient.execute(true);

                    LOGGER.debug("Successfully retried the request! Proceeding with the normal workflow...");
                } else {
                    /* if we cannot solve this issue through the Central Services, then there's nothing we can do, so we let it be thrown */
                    eadcError = "Could not find configurations in the Central Services for [" + endpoint + "], the service will fail.";
                    LOGGER.error(eadcError);
                    throw new XCAException(OpenNCPErrorCode.ERROR_GENERIC, e.getMessage(), null);
                }
            }
            _returnMessageContext = _operationClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            final SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();
            transactionEndTime = new Date();

            // TMP
            // Transaction end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST TRANSACTION TIME: '{}'", (end - start) / 1000.0);

            // TMP
            // Transaction start time
            start = System.currentTimeMillis();

            /* Log soap response */
            final String logResponseBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                    final String logResponseMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(_returnEnv));
                    loggerClinical.debug("{}\n{}", XCAConstants.LOG.INCOMING_XCA_QUERY_MESSAGE, logResponseMsg);
                }
                logResponseBody = XMLUtil.prettyPrint(XMLUtils.toDOM(_returnEnv.getBody().getFirstElement()));
            } catch (final Exception ex) {
                throw new XCAException(OpenNCPErrorCode.ERROR_GENERIC, ex.getMessage(), null);
            }

            /* Validate Response Message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCrossCommunityAccess(logResponseBody, NcpSide.NCP_B, classCodes);
            }

            // TMP
            // Validation end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST VALIDATION RES TIME: '{}'", (end - start) / 1000);

            // eADC start time
            start = System.currentTimeMillis();

            // NRR
//            try {
//                EvidenceUtils.createEvidenceREMNRR(XMLUtil.prettyPrint(XMLUtils.toDOM(env)),
//                        tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
//                        tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                        tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                        EventType.epsosOrderServiceList.getCode(),
//                        new DateTime(),
//                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                        "NCPB_XCA_LIST_RES");
//            } catch (Exception e) {
//                LOGGER.error(ExceptionUtils.getStackTrace(e));
//            }

            //  Invoke eADC
            if(!EadcUtilWrapper.hasTransactionErrors(_returnEnv)) {
                EadcUtilWrapper.invokeEadc(_messageContext, _returnMessageContext, this._getServiceClient(), null,
                        transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.EADC,
                        EadcUtil.Direction.OUTBOUND, ServiceType.DOCUMENT_LIST_QUERY);
            } else {
                eadcError = EadcUtilWrapper.getTransactionErrorDescription(_returnEnv);
            }
            // eADC end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST eADC TIME: '{}' ms", (end - start) / 1000.0);

            // Audit start time
            start = System.currentTimeMillis();

            /*
             * Return
             */
            final Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    AdhocQueryResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            final AdhocQueryResponse adhocQueryResponse = (AdhocQueryResponse) object;

            final String dstHomeCommunityId = OidUtil.getHomeCommunityId(countryCode.toLowerCase(Locale.ENGLISH));

            for (final ClassCode classCode : classCodes) {
                createAndSendEventLogQuery(adhocQueryRequest, adhocQueryResponse,
                        _messageContext, _returnEnv, env, assertionMap.get(AssertionType.HCP), assertionMap.get(AssertionType.TRC),
                        this._getServiceClient().getOptions().getTo().getAddress(),
                        classCode, dstHomeCommunityId); // Audit
            }
            // TMP
            // Audit end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST AUDIT TIME: '{}' ms", (end - start) / 1000.0);

            return adhocQueryResponse;

        } catch (final AxisFault axisFault) {
            // TODO A.R. Audit log SOAP Fault is still missing
            final OMElement faultElt = axisFault.getDetail();

            if (faultElt != null) {

                if (faultExceptionNameMap.containsKey(faultElt.getQName())) {

                    //make the fault by reflection
                    try {
                        final String exceptionClassName = (String) faultExceptionClassNameMap.get(faultElt.getQName());
                        final Class exceptionClass = Class.forName(exceptionClassName);
                        final Exception ex = (Exception) exceptionClass.getDeclaredConstructor().newInstance();
                        //message class
                        final String messageClassName = (String) faultMessageMap.get(faultElt.getQName());
                        final Class messageClass = Class.forName(messageClassName);
                        final Object messageObject = fromOM(faultElt, messageClass, null);
                        final Method m = exceptionClass.getMethod("setFaultMessage", messageClass);
                        m.invoke(ex, messageObject);

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);

                        /* we cannot instantiate the class - throw the original Axis fault */
                    } catch (final Exception e) {
                        eadcError = e.getMessage();
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            }
            eadcError = OpenNCPErrorCode.ERROR_GENERIC_CONNECTION_NOT_POSSIBLE.getCode();
            throw new XCAException(OpenNCPErrorCode.ERROR_GENERIC_CONNECTION_NOT_POSSIBLE, "AxisFault", null);

        } finally {
            if (_messageContext != null && _messageContext.getTransportOut() != null && _messageContext.getTransportOut().getSender() != null) {
                _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            }
            if(!eadcError.isEmpty()) {
                EadcUtilWrapper.invokeEadcFailure(_messageContext, _returnMessageContext, this._getServiceClient(), null,
                        transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.EADC,
                        EadcUtil.Direction.OUTBOUND, ServiceType.DOCUMENT_LIST_QUERY, eadcError);
            }
        }
    }

    private RegisteredService getRegisteredService(final List<ClassCode> classCodes) {
        RegisteredService registeredService = null;
        for (final ClassCode classCode : classCodes) {
            switch (classCode) {
                case EP_CLASSCODE:
                    if (registeredService == null) {
                        registeredService = RegisteredService.ORDER_SERVICE;
                    } else {
                        LOGGER.error("It is not allowed to pass more than one classCode when the classCode '{}' is used.", ClassCode.EP_CLASSCODE.getCode());
                    }
                    break;
                case PS_CLASSCODE:
                    if (registeredService == null) {
                        registeredService = RegisteredService.PATIENT_SERVICE;
                    } else {
                        LOGGER.error("It is not allowed to pass more than one classCode when the classCode '{}' is used.", ClassCode.PS_CLASSCODE.getCode());
                    }
                    break;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    if (registeredService == null || registeredService == RegisteredService.ORCD_SERVICE) {
                        registeredService = RegisteredService.ORCD_SERVICE;
                    } else {
                        LOGGER.error("It is only allowed to pass OrCD classCodes when more than one classCode is passed.");
                    }
                    break;
                default:
                    break;
            }
        }
        return registeredService;
    }

    /**
     * Auto generated method signature
     *
     * @param retrieveDocumentSetRequest XCA request
     * @param assertionMap               HCP identity Assertion
     * @param classCode                  Class code of the document to be retrieved, needed for audit log preparation
     * @return RetrieveDocumentSetResponseType - Retrieve Document Response
     * @throws java.rmi.RemoteException
     */
    public RetrieveDocumentSetResponseType respondingGateway_CrossGatewayRetrieve(final RetrieveDocumentSetRequestType retrieveDocumentSetRequest,
                                                                                  final Map<AssertionType, Assertion> assertionMap,
                                                                                  final ClassCode classCode)
            throws java.rmi.RemoteException, XCAException {

        String eadcError = "";
        MessageContext _messageContext = null;
        MessageContext _returnMessageContext = null;
        Document cda = null;

        SOAPEnvelope env;
        try {
            OperationClient _operationClient = _serviceClient.createClient(_operations[1].getName());
            _operationClient.getOptions().setAction(XCAConstants.SOAP_HEADERS.RETRIEVE.REQUEST_ACTION);
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            addPropertyToOperationClient(_operationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");
            addPropertyToOperationClient(_operationClient, org.apache.axis2.Constants.Configuration.ENABLE_MTOM, org.apache.axis2.Constants.VALUE_TRUE);

            // create a message context
            _messageContext = new MessageContext();

            // create SOAP envelope with that payload
            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    retrieveDocumentSetRequest,
                    optimizeContent(new QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.RETRIEVE.NAMESPACE_REQUEST_LOCAL_PART)));

            /*
             * adding SOAP soap_headers
             */
            final SOAPFactory soapFactory = getFactory(_operationClient.getOptions().getSoapVersionURI());
            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final OMNamespace ns2 = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.ADDRESSING_NAMESPACE, "addressing");

            final OMElement action = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("Action", ns2);
            final OMNode node = factory.createOMText(XCAConstants.SOAP_HEADERS.RETRIEVE.REQUEST_ACTION);
            action.addChild(node);

            final OMElement id = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("MessageID", ns2);
            final OMNode node2 = factory.createOMText(Constants.UUID_PREFIX + UUID.randomUUID());
            id.addChild(node2);

            OMElement to = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("To", ns2);
            OMNode node3 = factory.createOMText(addr);
            to.addChild(node3);

            final OMElement replyTo = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("ReplyTo", ns2);
            final OMElement address = OMAbstractFactory.getSOAP12Factory().createOMElement("Address", ns2);
            final OMNode node4 = factory.createOMText(XCAConstants.SOAP_HEADERS.ADDRESSING_NAMESPACE);
            address.addChild(node4);
            replyTo.addChild(address);

            /* We are manually adding all WSA headers (To, Action, MessageID). Other IHE service clients (XCPD and XDR) skip the addition of the To header
            and let the client-connector axis2 configurations take care of it (through the global engaging of WS-Addressing module in axis2.xml, which sets
            the To with the endpoint value from the transport information), but we cannot assume that these IHE Service clients will always be coupled with
            the client-connector (and that it'll always be based on Axis2). See issues EHNCP-1141 and EHNCP-1168.
            */
            _serviceClient.addHeader(to);
            _serviceClient.addHeader(action);
            _serviceClient.addHeader(id);
            _serviceClient.addHeader(replyTo);


            final OMNamespace ns = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.SECURITY_XSD, "wsse");
            final OMElement security = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("Security", ns);

            try {

                if (assertionMap.containsKey(AssertionType.NOK)) {
                    final var assertionNextOfKin = assertionMap.get(AssertionType.NOK);
                    security.addChild(XMLUtils.toOM(assertionNextOfKin.getDOM()));
                }
                final var assertionId = assertionMap.get(AssertionType.HCP);
                security.addChild(XMLUtils.toOM(assertionId.getDOM()));
                final var assertionTreatment = assertionMap.get(AssertionType.TRC);
                security.addChild(XMLUtils.toOM(assertionTreatment.getDOM()));
                _serviceClient.addHeader(security);
            } catch (final Exception ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }

            _serviceClient.addHeadersToEnvelope(env);

            /*
             * Prepare request
             */
            _messageContext.setEnvelope(env);   // set the message context with that soap envelope
            _operationClient.addMessageContext(_messageContext);    // add the message context to the operation client

            /* Log soap request */
            final String logRequestBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                    final String logRequestMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(env));
                    loggerClinical.debug("{}\n{}", XCAConstants.LOG.OUTGOING_XCA_RETRIEVE_MESSAGE, logRequestMsg);
                }
                logRequestBody = XMLUtil.prettyPrint(XMLUtils.toDOM(env.getBody().getFirstElement()));
                // NRO
//                try {
//                    EvidenceUtils.createEvidenceREMNRO(envCanonicalized,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                            EventType.epsosOrderServiceRetrieve.getCode(),
//                            new DateTime(),
//                            EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                            "NCPB_XCA_RETRIEVE_REQ");
//                } catch (Exception e) {
//                    LOGGER.error(ExceptionUtils.getStackTrace(e));
//                }
            } catch (final Exception ex) {
                throw new XCAException(getErrorCode(classCode), ex.getMessage(), null);
            }

            /* Validate Request Message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCrossCommunityAccess(logRequestBody, NcpSide.NCP_B, List.of(classCode));
            }

            /*
             * Execute Operation
             */
            transactionStartTime = new Date();
            final SOAPEnvelope returnEnv;
            try {
                _operationClient.execute(true);
            } catch (final AxisFault e) {
                LOGGER.error("Axis Fault error: '{}'", e.getMessage());
                LOGGER.error("Trying to automatically solve the problem by fetching configurations from the Central Services...");

                String endpoint = null;
                LOGGER.debug("ClassCode: " + classCode);
                final DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
                switch (classCode) {
                    case PS_CLASSCODE:
                        endpoint = dynamicDiscoveryService.getEndpointUrl(
                                this.countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.PATIENT_SERVICE, true);
                        break;
                    case EP_CLASSCODE:
                        endpoint = dynamicDiscoveryService.getEndpointUrl(
                                this.countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.ORDER_SERVICE, true);
                        break;
                    default:
                        break;
                }
                if (StringUtils.isNotEmpty(endpoint)) {

                    /* if we get something from the Central Services, then we retry the request */
                    /* correctly sets the Transport information with the new endpoint */
                    LOGGER.debug("Retrying the request with the new configurations: [{}]", endpoint);
                    _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(endpoint));

                    /* we need a new OperationClient, otherwise we'll face the error "A message was added that is not valid. However, the operation context was complete." */
                    final OperationClient newOperationClient = _serviceClient.createClient(_operations[1].getName());
                    newOperationClient.getOptions().setAction(XCAConstants.SOAP_HEADERS.RETRIEVE.REQUEST_ACTION);
                    newOperationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
                    addPropertyToOperationClient(newOperationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");
                    addPropertyToOperationClient(newOperationClient, org.apache.axis2.Constants.Configuration.ENABLE_MTOM, org.apache.axis2.Constants.VALUE_TRUE);

                    final SOAPFactory newSoapFactory = getFactory(newOperationClient.getOptions().getSoapVersionURI());

                    /* As there's no simple way of replacing the previously set WSA To header (still containing the old endpoint) we need to create a new SOAP
                    payload so that the wsa:To header is correctly set. Here we have 2 situations. If we rely on the client-connector axis2.xml configurations,
                    then the new endpoint will be copied from the Transport information to the wsa:To during the running of the Addressing Phase,
                    as defined by the global engagement of the addressing module in axis2.xml. But we're not doing it since these IHE service clients should be
                    decoupled from client-connector, thus we manually add the new WSA To header to the new SOAP envelope. In this specific case of the XCA Retrieve,
                    since the manual WSA headers are being added under the http://www.w3.org/2005/08/addressing/anonymous namespace and not under the usual http://www.w3.org/2005/08/addressing
                    namespace, both the manually added headers and the automatically added ones from client-connector's axis2 configurations will be added. This needs to be analysed.
                    See issues EHNCP-1141 and EHNCP-1168. */
                    final SOAPEnvelope newEnv;
                    newEnv = toEnvelope(newSoapFactory,
                            retrieveDocumentSetRequest,
                            optimizeContent(new QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.RETRIEVE.NAMESPACE_REQUEST_LOCAL_PART)));

                    /* Creating the new WSA To header with the new endpoint */
                    to = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("To", ns2);
                    node3 = newSoapFactory.createOMText(endpoint);
                    to.addChild(node3);
//                    to.addAttribute(att2);

                    /* There's no way to remove a single header (and WS-Addressing specification forbids the existence of
                    more than 1 addressing:To header), so we need to remove all the old headers and add them again to _serviceClient
                    (including the new To header with the right endpoint), from which they'll be copied into the final SOAP envelope */
                    _serviceClient.removeHeaders();
                    _serviceClient.addHeader(to);
                    _serviceClient.addHeader(action);
                    _serviceClient.addHeader(id);
                    _serviceClient.addHeader(replyTo);
                    _serviceClient.addHeader(security);
                    _serviceClient.addHeadersToEnvelope(newEnv);

                    /* we create a new Message Context with the new SOAP envelope */
                    final MessageContext newMessageContext = new MessageContext();
                    newMessageContext.setEnvelope(newEnv);

                    /* add the new message context to the new operation client */
                    newOperationClient.addMessageContext(newMessageContext);

                    /* we need to reset the previous variables with the new content, to be used later */
                    _operationClient = newOperationClient;
                    _messageContext = newMessageContext;
                    env = newEnv;
                    /* we retry the request */
                    newOperationClient.execute(true);
                    LOGGER.debug("Successfully retried the request! Proceeding with the normal workflow...");
                } else {
                    /* if we cannot solve this issue through the Central Services, then there's nothing we can do, so we let it be thrown */
                    eadcError = "Could not find configurations in the Central Services for [" + endpoint + "], the service will fail.";
                    LOGGER.error(eadcError);
                    throw new XCAException(getErrorCode(classCode), e.getMessage(), null);
                }
            }
            _returnMessageContext = _operationClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            returnEnv = _returnMessageContext.getEnvelope();
            transactionEndTime = new Date();

            /* Log soap response */
            final String logResponseBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                    final String logResponseMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(returnEnv));
                    loggerClinical.debug("{}\n{}", XCAConstants.LOG.INCOMING_XCA_RETRIEVE_MESSAGE, logResponseMsg);
                }
                logResponseBody = XMLUtil.prettyPrint(XMLUtils.toDOM(returnEnv.getBody().getFirstElement()));
            } catch (final Exception ex) {
                throw new XCAException(getErrorCode(classCode), ex.getMessage(), null);
            }

            /* Validate Response Message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCrossCommunityAccess(logResponseBody, NcpSide.NCP_B, List.of(classCode));
            }

            /*
             * Return
             */
            final RetrieveDocumentSetResponseType retrieveDocumentSetResponse;
            final Object object = fromOM(returnEnv.getBody().getFirstElement(), RetrieveDocumentSetResponseType.class,
                    getEnvelopeNamespaces(returnEnv));
            retrieveDocumentSetResponse = (RetrieveDocumentSetResponseType) object;

            // NRR evidences optional.
            LOGGER.info("XCA Retrieve Request received. EVIDENCE NRR");

            // Invoke eADC
            if (EadcUtilWrapper.hasTransactionErrors(returnEnv)) {
                eadcError = EadcUtilWrapper.getTransactionErrorDescription(returnEnv);
            } else {
                if (retrieveDocumentSetResponse.getDocumentResponse() != null && !retrieveDocumentSetResponse.getDocumentResponse().isEmpty()) {

                    cda = EadcUtilWrapper.toXmlDocument(retrieveDocumentSetResponse.getDocumentResponse().get(0).getDocument());
                }
                EadcUtilWrapper.invokeEadc(_messageContext, _returnMessageContext, this._getServiceClient(), cda,
                        transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.EADC,
                        EadcUtil.Direction.OUTBOUND, ServiceType.DOCUMENT_EXCHANGED_QUERY);
            }

            final String dstHomeCommunityId = OidUtil.getHomeCommunityId(countryCode.toLowerCase(Locale.ENGLISH));

            //  Create Audit messages
            final EventLog eventLog = createAndSendEventLogRetrieve(retrieveDocumentSetRequest, retrieveDocumentSetResponse,
                    _messageContext, returnEnv, env, assertionMap.get(AssertionType.HCP),
                    assertionMap.get(AssertionType.TRC), this._getServiceClient().getOptions().getTo().getAddress(),
                    classCode, dstHomeCommunityId);
            LOGGER.info("[Audit Service] Event Log '{}' sent to ATNA server", eventLog.getEventType());

            return retrieveDocumentSetResponse;

        } catch (final AxisFault axisFault) {
            // TODO A.R. Audit log SOAP Fault is still missing
            final OMElement faultElt = axisFault.getDetail();

            if (faultElt != null && faultExceptionNameMap.containsKey(faultElt.getQName())) {

                //make the fault by reflection
                try {
                    final String exceptionClassName = (String) faultExceptionClassNameMap.get(faultElt.getQName());
                    final Class exceptionClass = Class.forName(exceptionClassName);
                    final Exception ex = (Exception) exceptionClass.getDeclaredConstructor().newInstance();
                    //message class
                    final String messageClassName = (String) faultMessageMap.get(faultElt.getQName());
                    final Class messageClass = Class.forName(messageClassName);
                    final Object messageObject = fromOM(faultElt, messageClass, null);
                    final Method m = exceptionClass.getMethod("setFaultMessage", messageClass);
                    m.invoke(ex, messageObject);
                    throw new java.rmi.RemoteException(ex.getMessage(), ex);

                } catch (final Exception e) {
                    // Cannot instantiate the class - throw the original Axis fault
                    eadcError = e.getMessage();
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            eadcError = axisFault.getMessage();
            throw new XCAException(OpenNCPErrorCode.ERROR_GENERIC_CONNECTION_NOT_POSSIBLE, axisFault.getMessage(), null);
        } finally {
            if (_messageContext != null && _messageContext.getTransportOut() != null && _messageContext.getTransportOut().getSender() != null) {
                _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            }
            if(!eadcError.isEmpty()) {
                EadcUtilWrapper.invokeEadcFailure(_messageContext, _returnMessageContext, this._getServiceClient(), cda,
                        transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.EADC,
                        EadcUtil.Direction.OUTBOUND, ServiceType.DOCUMENT_EXCHANGED_QUERY, eadcError);
            }
        }
    }

    /**
     * An utility method that copies the namespaces from the SOAPEnvelope
     */
    private Map getEnvelopeNamespaces(final SOAPEnvelope env) {

        final Map returnMap = new HashMap();
        final Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
            final OMNamespace ns = (OMNamespace) namespaceIterator.next();
            returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
        }
        return returnMap;
    }

    private boolean optimizeContent(final QName opName) {

        if (opNameArray == null) {
            return false;
        }
        for (final QName anOpNameArray : opNameArray) {
            if (opName.equals(anOpNameArray)) {
                return true;
            }
        }
        return false;
    }

    private OMElement toOM(final AdhocQueryRequest param, final boolean optimizeContent) throws AxisFault {
        try {

            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final JaxbRIDataSource source = new JaxbRIDataSource(AdhocQueryRequest.class,
                    param, marshaller, XCAConstants.REGREP_QUERY, XCAConstants.ADHOC_QUERY_REQUEST);
            final OMNamespace namespace = factory.createOMNamespace(XCAConstants.REGREP_QUERY, null);

            return factory.createOMElement(source, XCAConstants.ADHOC_QUERY_REQUEST, namespace);
        } catch (final JAXBException e) {
            throw AxisFault.makeFault(e);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final AdhocQueryRequest param, final boolean optimizeContent)
            throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    private OMElement toOM(final AdhocQueryResponse param, final boolean optimizeContent)
            throws AxisFault {
        try {

            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final JaxbRIDataSource source = new JaxbRIDataSource(AdhocQueryResponse.class,
                    param,
                    marshaller,
                    XCAConstants.REGREP_QUERY,
                    XCAConstants.ADHOC_QUERY_RESPONSE);
            final OMNamespace namespace = factory.createOMNamespace(XCAConstants.REGREP_QUERY,
                    null);
            return factory.createOMElement(source, XCAConstants.ADHOC_QUERY_RESPONSE, namespace);
        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    @SuppressWarnings("unused")
    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final AdhocQueryResponse param, final boolean optimizeContent)
            throws AxisFault {
        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    private OMElement toOM(final RetrieveDocumentSetRequestType param, final boolean optimizeContent) throws AxisFault {

        try {

            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final JaxbRIDataSource source = new JaxbRIDataSource(RetrieveDocumentSetRequestType.class,
                    param,
                    marshaller,
                    XCAConstants.SOAP_HEADERS.NAMESPACE_URI,
                    XCAConstants.RETRIEVE_DOCUMENT_SET_REQUEST);
            final OMNamespace namespace = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.NAMESPACE_URI,
                    null);
            return factory.createOMElement(source, XCAConstants.RETRIEVE_DOCUMENT_SET_REQUEST, namespace);
        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final RetrieveDocumentSetRequestType param, final boolean optimizeContent)
            throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    private OMElement toOM(final RetrieveDocumentSetResponseType param, final boolean optimizeContent) throws AxisFault {

        try {
            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            final OMFactory factory = OMAbstractFactory.getOMFactory();
            final JaxbRIDataSource source = new JaxbRIDataSource(
                    RetrieveDocumentSetResponseType.class, param, marshaller, XCAConstants.SOAP_HEADERS.NAMESPACE_URI,
                    XCAConstants.RETRIEVE_DOCUMENT_SET_RESPONSE);
            final OMNamespace namespace = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, null);

            return factory.createOMElement(source, XCAConstants.RETRIEVE_DOCUMENT_SET_RESPONSE, namespace);

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    @SuppressWarnings("unused")
    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final RetrieveDocumentSetResponseType param, final boolean optimizeContent)
            throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    /**
     * get the default envelope
     */
    @SuppressWarnings("unused")
    private SOAPEnvelope toEnvelope(final SOAPFactory factory) {
        return factory.getDefaultEnvelope();
    }

    @SuppressWarnings("unchecked")
    private Object fromOM(final OMElement param, final Class type, final Map extraNamespaces) throws AxisFault {

        try {

            final Unmarshaller unmarshaller = wsContext.createUnmarshaller();
            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private EventLog createAndSendEventLogQuery(final AdhocQueryRequest request, final AdhocQueryResponse response, final MessageContext msgContext,
                                                final SOAPEnvelope _returnEnv, final SOAPEnvelope env, final Assertion idAssertion, final Assertion trcAssertion,
                                                final String address, final ClassCode classCode, final String dstHomeCommunityId) {

        final EventLog eventLog = EventLogClientUtil.prepareEventLog(msgContext, _returnEnv, address, dstHomeCommunityId);
        EventLogClientUtil.logIdAssertion(eventLog, idAssertion);
        EventLogClientUtil.logTrcAssertion(eventLog, trcAssertion);
        EventLogUtil.prepareXCACommonLogQuery(eventLog, msgContext, request, response, classCode);
        eventLog.setNcpSide(NcpSide.NCP_B);
        EventLogClientUtil.sendEventLog(eventLog);

        return eventLog;
    }

    private EventLog createAndSendEventLogRetrieve(final RetrieveDocumentSetRequestType request, final RetrieveDocumentSetResponseType response,
                                                   final MessageContext msgContext, final SOAPEnvelope _returnEnv, final SOAPEnvelope env,
                                                   final Assertion idAssertion, final Assertion trcAssertion, final String address, final ClassCode classCode, final String dstHomeCommunityId) {

        final EventLog eventLog = EventLogClientUtil.prepareEventLog(msgContext, _returnEnv, address, dstHomeCommunityId);
        EventLogClientUtil.logIdAssertion(eventLog, idAssertion);
        EventLogClientUtil.logTrcAssertion(eventLog, trcAssertion);
        EventLogUtil.prepareXCACommonLogRetrieve(eventLog, msgContext, request, response, classCode);
        eventLog.setNcpSide(NcpSide.NCP_B);
        EventLogClientUtil.sendEventLog(eventLog);

        return eventLog;
    }

    public OpenNCPErrorCode getErrorCode(final ClassCode classCode) {
        switch (classCode) {
            case PS_CLASSCODE:
                return OpenNCPErrorCode.ERROR_PS_GENERIC;
            case EP_CLASSCODE:
                return OpenNCPErrorCode.ERROR_EP_GENERIC;
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                return OpenNCPErrorCode.ERROR_ORCD_GENERIC;
        }

        return OpenNCPErrorCode.ERROR_GENERIC;
    }

    /**
     * Inner JAXBRIDataSource class
     */
    class JaxbRIDataSource extends AbstractOMDataSource {

        /**
         * Bound object for output.
         */
        private final Object outObject;
        /**
         * Bound class for output.
         */
        @SuppressWarnings("unused")
        private final Class outClazz;
        /**
         * Marshaller.
         */
        private final Marshaller marshaller;
        /**
         * Namespace
         */
        private final String nsuri;
        /**
         * Local name
         */
        private final String name;

        /**
         * Constructor from object and marshaller.
         *
         * @param obj
         * @param marshaller
         */
        public JaxbRIDataSource(final Class clazz, final Object obj, final Marshaller marshaller, final String nsuri, final String name) {
            this.outClazz = clazz;
            this.outObject = obj;
            this.marshaller = marshaller;
            this.nsuri = nsuri;
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public void serialize(final OutputStream output) throws XMLStreamException {
            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), output);
            } catch (final JAXBException e) {
                throw new XMLStreamException(XCAConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        @SuppressWarnings("unchecked")
        public void serialize(final Writer writer) throws XMLStreamException {

            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), writer);
            } catch (final JAXBException e) {
                throw new XMLStreamException(XCAConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        @SuppressWarnings("unchecked")
        public void serialize(final XMLStreamWriter xmlWriter) throws XMLStreamException {

            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), xmlWriter);
            } catch (final JAXBException e) {
                throw new XMLStreamException(XCAConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        @SuppressWarnings("unchecked")
        public XMLStreamReader getReader() throws XMLStreamException {

            try {
                final OMDocument omDocument = OMAbstractFactory.getOMFactory().createOMDocument();
                wsContext.createMarshaller().marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject),
                        omDocument.getSAXResult());

                return omDocument.getOMDocumentElement().getXMLStreamReader();
            } catch (final JAXBException e) {
                throw new XMLStreamException(XCAConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        @Override
        public boolean isDestructiveRead() {
            return false;
        }

        @Override
        public boolean isDestructiveWrite() {
            return false;
        }
    }
}
