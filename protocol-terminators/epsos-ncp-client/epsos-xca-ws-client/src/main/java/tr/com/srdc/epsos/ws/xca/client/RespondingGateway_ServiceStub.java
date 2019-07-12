package tr.com.srdc.epsos.ws.xca.client;

import com.spirit.epsos.cc.adc.EadcEntry;
import ee.affecto.epsos.util.EventLogClientUtil;
import ee.affecto.epsos.util.EventLogUtil;
import epsos.ccd.gnomon.auditmanager.EventLog;
import eu.epsos.pt.eadc.EadcUtilWrapper;
import eu.epsos.pt.eadc.util.EadcUtil.Direction;
import eu.epsos.pt.transformation.TMServices;
import eu.epsos.util.xca.XCAConstants;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.eadc.ServiceType;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.RegisteredService;
import eu.europa.ec.sante.ehdsi.openncp.pt.common.DynamicDiscoveryService;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import org.apache.axiom.om.*;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12HeaderBlockImpl;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.namespace.QName;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/*
 *  RespondingGateway_ServiceStub java implementation
 */
public class RespondingGateway_ServiceStub extends org.apache.axis2.client.Stub {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespondingGateway_ServiceStub.class);
    private static final javax.xml.bind.JAXBContext wsContext;
    private static int counter = 0;

    static {

        LOGGER.debug("Loading the WS-Security init libraries in RespondingGateway_ServiceStub xca");
        org.apache.xml.security.Init.init();
    }

    static {

        javax.xml.bind.JAXBContext jc;
        jc = null;
        try {
            jc = javax.xml.bind.JAXBContext.newInstance(oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest.class,
                    oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse.class, RetrieveDocumentSetRequestType.class,
                    RetrieveDocumentSetResponseType.class);
        } catch (javax.xml.bind.JAXBException ex) {
            LOGGER.error(XCAConstants.EXCEPTIONS.UNABLE_CREATE_JAXB_CONTEXT + " " + ex.getMessage(), ex);
            Runtime.getRuntime().exit(-1);
        } finally {
            wsContext = jc;
        }
    }

    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    protected org.apache.axis2.description.AxisOperation[] _operations;
    //hashmaps to keep the fault mapping
    private java.util.HashMap faultExceptionNameMap = new java.util.HashMap();
    private java.util.HashMap faultExceptionClassNameMap = new java.util.HashMap();
    private java.util.HashMap faultMessageMap = new java.util.HashMap();
    private String addr;
    private String countryCode;
    private Date transactionStartTime;
    private Date transactionEndTime;
    private javax.xml.namespace.QName[] opNameArray = null;

    /**
     * Constructor that takes in a configContext
     */
    public RespondingGateway_ServiceStub(org.apache.axis2.context.ConfigurationContext configurationContext, java.lang.String targetEndpoint)
            throws org.apache.axis2.AxisFault {
        this(configurationContext, targetEndpoint, false);
    }

    /**
     * Constructor that takes in a configContext and use separate listener
     */
    public RespondingGateway_ServiceStub(ConfigurationContext configurationContext, String targetEndpoint, boolean useSeparateListener)
            throws AxisFault {
        //To populate AxisService
        populateAxisService();
        populateFaults();

        _serviceClient = new org.apache.axis2.client.ServiceClient(configurationContext, _service);

        _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(targetEndpoint));
        _serviceClient.getOptions().setUseSeparateListener(useSeparateListener);
        //  Wait time after which a client times out in a blocking scenario: 3 minutes
        _serviceClient.getOptions().setTimeOutInMilliSeconds(180000);

        //Set the soap version
        _serviceClient.getOptions().setSoapVersionURI(org.apache.axiom.soap.SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
    }

    /**
     * Default Constructor
     */
    public RespondingGateway_ServiceStub(org.apache.axis2.context.ConfigurationContext configurationContext)
            throws org.apache.axis2.AxisFault {
        this(configurationContext, "http://195.142.27.167:8111/tr-xca/services/RespondingGateway_Service");
    }

    /**
     * Default Constructor
     */
    public RespondingGateway_ServiceStub() throws org.apache.axis2.AxisFault {
        this("http://195.142.27.167:8111/tr-xca/services/RespondingGateway_Service");
    }

    /**
     * Constructor taking the target endpoint
     */
    public RespondingGateway_ServiceStub(java.lang.String targetEndpoint)
            throws org.apache.axis2.AxisFault {
        this(null, targetEndpoint);
    }

    private static synchronized java.lang.String getUniqueSuffix() {
        // reset the counter if it is greater than 99999
        if (counter > 99999) {
            counter = 0;
        }
        counter++;
        return System.currentTimeMillis() + "_" + counter;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    private void populateAxisService() {
        //creating the Service with a unique name
        _service = new org.apache.axis2.description.AxisService(XCAConstants.RESPONDING_GATEWAY_SERVICE + getUniqueSuffix());
        addAnonymousOperations();
        //creating the operations
        org.apache.axis2.description.AxisOperation __operation;
        _operations = new org.apache.axis2.description.AxisOperation[2];
        __operation = new org.apache.axis2.description.OutInAxisOperation();
        __operation.setName(new javax.xml.namespace.QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.QUERY.NAMESPACE_REQUEST_LOCAL_PART));
        _service.addOperation(__operation);
        _operations[0] = __operation;
        __operation = new org.apache.axis2.description.OutInAxisOperation();
        __operation.setName(new javax.xml.namespace.QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.RETRIEVE.NAMESPACE_REQUEST_LOCAL_PART));
        _service.addOperation(__operation);
        _operations[1] = __operation;
    }

    //populates the faults
    private void populateFaults() {
    }

    /**
     * Auto generated method signature
     *
     * @param adhocQueryRequest
     * @see tr.com.srdc.epsos.test.xca.RespondingGateway_Service#respondingGateway_CrossGatewayQuery
     */
    public AdhocQueryResponse respondingGateway_CrossGatewayQuery(AdhocQueryRequest adhocQueryRequest,
                                                                  Assertion idAssertion,
                                                                  Assertion trcAssertion,
                                                                  String classCode)
            throws java.rmi.RemoteException {
        MessageContext _messageContext = null;
        try {
            // TMP
            // XCA list request start time
            long start = System.currentTimeMillis();

            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[0].getName());
            _operationClient.getOptions().setAction(XCAConstants.SOAP_HEADERS.QUERY.REQUEST_ACTION);
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            addPropertyToOperationClient(_operationClient, org.apache.axis2.description.WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

            // create a message context
            _messageContext = new org.apache.axis2.context.MessageContext();

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env;
            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    adhocQueryRequest,
                    optimizeContent(new javax.xml.namespace.QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.QUERY.NAMESPACE_REQUEST_LOCAL_PART)));

            /*
             * adding SOAP soap_headers
             */
            SOAPFactory soapFactory = getFactory(_operationClient.getOptions().getSoapVersionURI());
            OMFactory factory = OMAbstractFactory.getOMFactory();

            OMNamespace ns2 = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.QUERY.OM_NAMESPACE, "addressing");

            SOAPHeaderBlock action = new SOAP12HeaderBlockImpl("Action", ns2, soapFactory);
            OMNode node = factory.createOMText(XCAConstants.SOAP_HEADERS.QUERY.REQUEST_ACTION);
            action.addChild(node);
            OMAttribute att1 = factory.createOMAttribute(XCAConstants.SOAP_HEADERS.MUST_UNDERSTAND, env.getNamespace(), "1");
            action.addAttribute(att1);

            SOAPHeaderBlock id = new SOAP12HeaderBlockImpl("MessageID", ns2, soapFactory);
            OMNode node2 = factory.createOMText(Constants.UUID_PREFIX + UUID.randomUUID().toString());
            id.addChild(node2);

            SOAPHeaderBlock to = new SOAP12HeaderBlockImpl("To", ns2, soapFactory);
            OMNode node3 = factory.createOMText(addr);
            to.addChild(node3);
            OMAttribute att2 = factory.createOMAttribute(XCAConstants.SOAP_HEADERS.MUST_UNDERSTAND, env.getNamespace(), "1");
            to.addAttribute(att2);

            SOAPHeaderBlock replyTo = new SOAP12HeaderBlockImpl("ReplyTo", ns2, soapFactory);
            OMElement address = new SOAP12HeaderBlockImpl("Address", ns2, soapFactory);
            OMNode node4 = factory.createOMText(XCAConstants.SOAP_HEADERS.ADDRESSING_NAMESPACE);
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

            OMNamespace ns = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.SECURITY_XSD, "wsse");
            SOAPHeaderBlock security = new SOAP12HeaderBlockImpl("Security", ns, soapFactory);
            try {
                security.addChild(XMLUtils.toOM(trcAssertion.getDOM()));
                security.addChild(XMLUtils.toOM(idAssertion.getDOM()));
                _serviceClient.addHeader(security);
            } catch (Exception ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }

            _serviceClient.addHeadersToEnvelope(env);

            /*
             * Prepare request
             */
            _messageContext.setEnvelope(env);   // set the message context with that soap envelope
            _operationClient.addMessageContext(_messageContext);    // add the message contxt to the operation client

            /* Log soap request */
            String logRequestBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                    String logRequestMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(env));
                    loggerClinical.debug("{}\n{}", XCAConstants.LOG.OUTGOING_XCA_QUERY_MESSAGE, logRequestMsg);
                }
                logRequestBody = XMLUtil.prettyPrint(XMLUtils.toDOM(env.getBody().getFirstElement()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
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
                OpenNCPValidation.validateCrossCommunityAccess(logRequestBody, NcpSide.NCP_B);
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
            } catch (AxisFault e) {
                LOGGER.error("Axis Fault error: '{}'", e.getMessage());
                LOGGER.error("Trying to automatically solve the problem by fetching configurations from the Central Services...");
                String endpoint = null;
                LOGGER.debug("ClassCode: '{}'", classCode);
                DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
                switch (classCode) {
                    case Constants.PS_CLASSCODE:
                        endpoint = dynamicDiscoveryService.getEndpointUrl(
                                this.countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.PATIENT_SERVICE, true);
                        break;
                    case Constants.EP_CLASSCODE:
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
                    org.apache.axis2.client.OperationClient newOperationClient = _serviceClient.createClient(_operations[0].getName());
                    newOperationClient.getOptions().setAction(XCAConstants.SOAP_HEADERS.QUERY.REQUEST_ACTION);
                    newOperationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
                    addPropertyToOperationClient(newOperationClient, org.apache.axis2.description.WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

                    SOAPFactory newSoapFactory = getFactory(newOperationClient.getOptions().getSoapVersionURI());

                    /* As there's no simple way of replacing the previously set WSA To header (still containing the old endpoint) we need to create a new SOAP
                    payload so that the wsa:To header is correctly set. Here we have 2 situations. If we rely on the client-connector axis2.xml configurations,
                    then the new endpoint will be copied from the Transport information to the wsa:To during the running of the Addressing Phase,
                    as defined by the global engagement of the addressing module in axis2.xml. But we're not doing it since these IHE service clients should be
                    decoupled from client-connector, thus we manually add the new WSA To header to the new SOAP envelope. See issues EHNCP-1141 and EHNCP-1168. */
                    org.apache.axiom.soap.SOAPEnvelope newEnv;
                    newEnv = toEnvelope(newSoapFactory,
                            adhocQueryRequest,
                            optimizeContent(new javax.xml.namespace.QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.QUERY.NAMESPACE_REQUEST_LOCAL_PART)));

                    /* Creating the new WSA To header with the new endpoint */
                    to = new SOAP12HeaderBlockImpl("To", ns2, soapFactory);
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
                    _serviceClient.addHeader(security);
                    _serviceClient.addHeadersToEnvelope(newEnv);

                    /* we create a new Message Context with the new SOAP envelope */
                    org.apache.axis2.context.MessageContext newMessageContext = new org.apache.axis2.context.MessageContext();
                    newMessageContext.setEnvelope(newEnv);

                    /* add the new message contxt to the new operation client */
                    newOperationClient.addMessageContext(newMessageContext);
                    /* we retry the request */
                    newOperationClient.execute(true);
                    /* we need to reset the previous variables with the new content, to be used later */
                    _operationClient = newOperationClient;
                    _messageContext = newMessageContext;
                    env = newEnv;
                    LOGGER.debug("Successfully retried the request! Proceeding with the normal workflow...");
                } else {
                    /* if we cannot solve this issue through the Central Services, then there's nothing we can do, so we let it be thrown */
                    LOGGER.error("Could not find configurations in the Central Services for [{}], the service will fail.", endpoint);
                    throw e;
                }
            }

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();
            transactionEndTime = new Date();

            // TMP
            // Transaction end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST TRANSACTION TIME: '{}'", (end - start) / 1000.0);

            // TMP
            // Transaction start time
            start = System.currentTimeMillis();

            /* Log soap response */
            String logResponseBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                    String logResponseMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(_returnEnv));
                    loggerClinical.debug("{}\n{}", XCAConstants.LOG.INCOMING_XCA_QUERY_MESSAGE, logResponseMsg);
                }
                logResponseBody = XMLUtil.prettyPrint(XMLUtils.toDOM(_returnEnv.getBody().getFirstElement()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            /* Validate Response Message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCrossCommunityAccess(logResponseBody, NcpSide.NCP_B);
            }

            // TMP
            // Validation end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST VALIDATION RES TIME: '{}'", (end - start) / 1000);

            // TMP
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

            /*
             * Invoque eADC
             */
            try {
                EadcUtilWrapper.invokeEadc(_messageContext, // Request message context
                        _returnMessageContext, // Response message context
                        this._getServiceClient(), //Service Client
                        null, // CDA document
                        transactionStartTime, // Transaction Start Time
                        transactionEndTime, // Transaction End Time
                        this.countryCode, // Country A ISO Code
                        EadcEntry.DsTypes.XCA, // Data source type
                        Direction.OUTBOUND, ServiceType.DOCUMENT_LIST_QUERY); // Transaction direction
            } catch (Exception ex) {
                LOGGER.error("EADC INVOCATION FAILED!", ex);
            }

            // TMP
            // eADC end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST eADC TIME: '{}' ms", (end - start) / 1000.0);

            // TMP
            // Audit start time
            start = System.currentTimeMillis();

            /*
             * Return
             */
            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            AdhocQueryResponse adhocQueryResponse = (AdhocQueryResponse) object;
            EventLog eventLog = createAndSendEventLogQuery(adhocQueryRequest, adhocQueryResponse,
                    _messageContext, _returnEnv, env, idAssertion, trcAssertion,
                    this._getServiceClient().getOptions().getTo().getAddress(),
                    classCode); // Audit
            // TMP
            // Audit end time
            end = System.currentTimeMillis();
            LOGGER.info("XCA LIST AUDIT TIME: '{}' ms", (end - start) / 1000.0);

            return adhocQueryResponse;

        } catch (org.apache.axis2.AxisFault f) {
            // TODO A.R. Audit log SOAP Fault is still missing
            org.apache.axiom.om.OMElement faultElt = f.getDetail();

            if (faultElt != null) {

                if (faultExceptionNameMap.containsKey(faultElt.getQName())) {

                    //make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExceptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex
                                = (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                messageClass);
                        m.invoke(ex, messageObject);

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);

                        /* we cannot intantiate the class - throw the original Axis fault */
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            }
            throw new RuntimeException(f.getMessage(), f);

        } finally {
            if (_messageContext != null && _messageContext.getTransportOut() != null && _messageContext.getTransportOut().getSender() != null) {
                _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            }
        }
    }

    /**
     * Auto generated method signature
     *
     * @param retrieveDocumentSetRequest
     * @param idAssertion
     * @param trcAssertion
     * @param classCode                  Class code of the document to be retrieved, needed for
     *                                   audit log preparation
     * @return
     * @throws java.rmi.RemoteException
     * @see tr.com.srdc.epsos.test.xca.RespondingGateway_Service#respondingGateway_CrossGatewayRetrieve
     */
    public RetrieveDocumentSetResponseType respondingGateway_CrossGatewayRetrieve(RetrieveDocumentSetRequestType retrieveDocumentSetRequest,
                                                                                  Assertion idAssertion,
                                                                                  Assertion trcAssertion,
                                                                                  String classCode)
            throws java.rmi.RemoteException {
        org.apache.axis2.context.MessageContext _messageContext = null;
        org.apache.axiom.soap.SOAPEnvelope env;
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[1].getName());
            _operationClient.getOptions().setAction(XCAConstants.SOAP_HEADERS.RETRIEVE.REQUEST_ACTION);
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            addPropertyToOperationClient(_operationClient, org.apache.axis2.description.WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");
            addPropertyToOperationClient(_operationClient, org.apache.axis2.Constants.Configuration.ENABLE_MTOM, org.apache.axis2.Constants.VALUE_TRUE);

            // create a message context
            _messageContext = new org.apache.axis2.context.MessageContext();

            // create SOAP envelope with that payload
            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    retrieveDocumentSetRequest,
                    optimizeContent(new javax.xml.namespace.QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.RETRIEVE.NAMESPACE_REQUEST_LOCAL_PART)));

            /*
             * adding SOAP soap_headers
             */
            SOAPFactory soapFactory = getFactory(_operationClient.getOptions().getSoapVersionURI());
            OMFactory factory = OMAbstractFactory.getOMFactory();

            OMNamespace ns2 = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.ADDRESSING_NAMESPACE, "addressing");

            OMElement action = new SOAP12HeaderBlockImpl("Action", ns2, soapFactory);
            OMNode node = factory.createOMText(XCAConstants.SOAP_HEADERS.RETRIEVE.REQUEST_ACTION);
            action.addChild(node);

            //OMAttribute att1 = factory.createOMAttribute(XCAConstants.SOAP_HEADERS.MUST_UNDERSTAND, env.getNamespace(), "1");
            //action.addAttribute(att1);
            OMElement id = new SOAP12HeaderBlockImpl("MessageID", ns2, soapFactory);
            OMNode node2 = factory.createOMText(Constants.UUID_PREFIX + UUID.randomUUID().toString());
            id.addChild(node2);

            OMElement to = new SOAP12HeaderBlockImpl("To", ns2, soapFactory);
            OMNode node3 = factory.createOMText(addr);
            to.addChild(node3);

            //OMAttribute att2 = factory.createOMAttribute(XCAConstants.SOAP_HEADERS.MUST_UNDERSTAND, env.getNamespace(), "1");
            //to.addAttribute(att2);
            OMElement replyTo = new SOAP12HeaderBlockImpl("ReplyTo", ns2, soapFactory);
            OMElement address = new SOAP12HeaderBlockImpl("Address", ns2, soapFactory);
            OMNode node4 = factory.createOMText(XCAConstants.SOAP_HEADERS.ADDRESSING_NAMESPACE);
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

            Element assertion1 = idAssertion.getDOM();
            Element assertion2 = trcAssertion.getDOM();
            OMNamespace ns = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.SECURITY_XSD, "wsse");
            OMElement security = new SOAP12HeaderBlockImpl("Security", ns, soapFactory);

            try {
                security.addChild(XMLUtils.toOM(assertion2));
                security.addChild(XMLUtils.toOM(assertion1));
                _serviceClient.addHeader(security);
            } catch (Exception ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }

            _serviceClient.addHeadersToEnvelope(env);

            /*
             * Prepare request
             */
            _messageContext.setEnvelope(env);   // set the message context with that soap envelope
            _operationClient.addMessageContext(_messageContext);    // add the message contxt to the operation client

            /* Log soap request */
            String logRequestBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                    String logRequestMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(env));
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
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            /* Validate Request Message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCrossCommunityAccess(logRequestBody, NcpSide.NCP_B);
            }

            /*
             * Execute Operation
             */
            transactionStartTime = new Date();
            org.apache.axiom.soap.SOAPEnvelope returnEnv;
            try {
                _operationClient.execute(true);
            } catch (AxisFault e) {
                LOGGER.error("Axis Fault error: '{}'", e.getMessage());
                LOGGER.error("Trying to automatically solve the problem by fetching configurations from the Central Services...");

                String endpoint = null;
                LOGGER.debug("ClassCode: " + classCode);
                DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
                switch (classCode) {
                    case Constants.PS_CLASSCODE:
                        endpoint = dynamicDiscoveryService.getEndpointUrl(
                                this.countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.PATIENT_SERVICE, true);
                        break;
                    case Constants.EP_CLASSCODE:
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
                    org.apache.axis2.client.OperationClient newOperationClient = _serviceClient.createClient(_operations[1].getName());
                    newOperationClient.getOptions().setAction(XCAConstants.SOAP_HEADERS.RETRIEVE.REQUEST_ACTION);
                    newOperationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
                    addPropertyToOperationClient(newOperationClient, org.apache.axis2.description.WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");
                    addPropertyToOperationClient(newOperationClient, org.apache.axis2.Constants.Configuration.ENABLE_MTOM, org.apache.axis2.Constants.VALUE_TRUE);

                    SOAPFactory newSoapFactory = getFactory(newOperationClient.getOptions().getSoapVersionURI());

                    /* As there's no simple way of replacing the previously set WSA To header (still containing the old endpoint) we need to create a new SOAP
                    payload so that the wsa:To header is correctly set. Here we have 2 situations. If we rely on the client-connector axis2.xml configurations,
                    then the new endpoint will be copied from the Transport information to the wsa:To during the running of the Addressing Phase,
                    as defined by the global engagement of the addressing module in axis2.xml. But we're not doing it since these IHE service clients should be
                    decoupled from client-connector, thus we manually add the new WSA To header to the new SOAP envelope. In this specific case of the XCA Retrieve,
                    since the manual WSA headers are being added under the http://www.w3.org/2005/08/addressing/anonymous namespace and not under the usual http://www.w3.org/2005/08/addressing
                    namespace, both the manually added headers and the automatically added ones from client-connector's axis2 configurations will be added. This needs to be analysed.
                    See issues EHNCP-1141 and EHNCP-1168. */
                    org.apache.axiom.soap.SOAPEnvelope newEnv;
                    newEnv = toEnvelope(newSoapFactory,
                            retrieveDocumentSetRequest,
                            optimizeContent(new javax.xml.namespace.QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.RETRIEVE.NAMESPACE_REQUEST_LOCAL_PART)));

                    /* Creating the new WSA To header with the new endpoint */
                    to = new SOAP12HeaderBlockImpl("To", ns2, soapFactory);
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
                    org.apache.axis2.context.MessageContext newMessageContext = new org.apache.axis2.context.MessageContext();
                    newMessageContext.setEnvelope(newEnv);

                    /* add the new message context to the new operation client */
                    newOperationClient.addMessageContext(newMessageContext);
                    /* we retry the request */
                    newOperationClient.execute(true);
                    /* we need to reset the previous variables with the new content, to be used later */
                    _operationClient = newOperationClient;
                    _messageContext = newMessageContext;
                    env = newEnv;
                    LOGGER.debug("Successfully retried the request! Proceeding with the normal workflow...");
                } else {
                    /* if we cannot solve this issue through the Central Services, then there's nothing we can do, so we let it be thrown */
                    LOGGER.error("Could not find configurations in the Central Services for [{}], the service will fail.", endpoint);
                    throw e;
                }
            }
            org.apache.axis2.context.MessageContext _returnMessageContext;
            _returnMessageContext = _operationClient.getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            returnEnv = _returnMessageContext.getEnvelope();
            transactionEndTime = new Date();

            /* Log soap response */
            String logResponseBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                    String logResponseMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(returnEnv));
                    loggerClinical.debug("{}\n{}", XCAConstants.LOG.INCOMING_XCA_RETRIEVE_MESSAGE, logResponseMsg);
                }
                logResponseBody = XMLUtil.prettyPrint(XMLUtils.toDOM(returnEnv.getBody().getFirstElement()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            /* Validate Response Message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCrossCommunityAccess(logResponseBody, NcpSide.NCP_B);
            }

            /*
             * Return
             */
            RetrieveDocumentSetResponseType retrieveDocumentSetResponse;
            Object object = fromOM(returnEnv.getBody().getFirstElement(), RetrieveDocumentSetResponseType.class,
                    getEnvelopeNamespaces(returnEnv));
            retrieveDocumentSetResponse = (RetrieveDocumentSetResponseType) object;

            LOGGER.info("XCA Retrieve Request received. EVIDENCE NRR");

//            // NRR
//            try {
//                EvidenceUtils.createEvidenceREMNRR(XMLUtil.prettyPrint(XMLUtils.toDOM(env)),
//                        tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
//                        tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                        tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                        EventType.epsosOrderServiceRetrieve.getCode(),
//                        new DateTime(),
//                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                        "NCPB_XCA_RETRIEVE_RES");
//            } catch (Exception e) {
//                LOGGER.error(ExceptionUtils.getStackTrace(e));
//            }

            //  Invoke eADC
            try {
                Document cda = null;
                if (retrieveDocumentSetResponse.getDocumentResponse() != null && !retrieveDocumentSetResponse.getDocumentResponse().isEmpty()) {
                    byte[] cdaBytes = retrieveDocumentSetResponse.getDocumentResponse().get(0).getDocument();
                    cda = TMServices.byteToDocument(cdaBytes);
                }

                EadcUtilWrapper.invokeEadc(_messageContext, _returnMessageContext, this._getServiceClient(),
                        cda, transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.XCA,
                        Direction.OUTBOUND, ServiceType.DOCUMENT_EXCHANGED_QUERY);
            } catch (Exception ex) {
                LOGGER.error("EADC INVOCATION FAILED!", ex);
            }

            /*
             * Create Audit messages
             */
            EventLog eventLog = createAndSendEventLogRetrieve(retrieveDocumentSetRequest, retrieveDocumentSetResponse,
                    _messageContext, returnEnv, env, idAssertion, trcAssertion,
                    this._getServiceClient().getOptions().getTo().getAddress(),
                    classCode);

            return retrieveDocumentSetResponse;

        } catch (org.apache.axis2.AxisFault f) {
            // TODO A.R. Audit log SOAP Fault is still missing
            org.apache.axiom.om.OMElement faultElt = f.getDetail();

            if (faultElt != null) {

                if (faultExceptionNameMap.containsKey(faultElt.getQName())) {

                    //make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExceptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex
                                = (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                messageClass);
                        m.invoke(ex, messageObject);

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);

                    } catch (Exception e) { // we cannot intantiate the class - throw the original Axis fault
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            }
            throw new RuntimeException(f.getMessage(), f);
        } finally {
            if (_messageContext != null && _messageContext.getTransportOut() != null && _messageContext.getTransportOut().getSender() != null) {
                _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            }
        }
    }

    /**
     * A utility method that copies the namepaces from the SOAPEnvelope
     */
    private java.util.Map getEnvelopeNamespaces(org.apache.axiom.soap.SOAPEnvelope env) {

        java.util.Map returnMap = new java.util.HashMap();
        java.util.Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
            org.apache.axiom.om.OMNamespace ns = (org.apache.axiom.om.OMNamespace) namespaceIterator.next();
            returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
        }
        return returnMap;
    }

    private boolean optimizeContent(javax.xml.namespace.QName opName) {

        if (opNameArray == null) {
            return false;
        }
        for (QName anOpNameArray : opNameArray) {
            if (opName.equals(anOpNameArray)) {
                return true;
            }
        }
        return false;
    }

    private org.apache.axiom.om.OMElement toOM(oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {
        try {

            javax.xml.bind.Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            org.apache.axiom.om.OMFactory factory = org.apache.axiom.om.OMAbstractFactory.getOMFactory();

            RespondingGateway_ServiceStub.JaxbRIDataSource source = new RespondingGateway_ServiceStub.JaxbRIDataSource(oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest.class,
                    param,
                    marshaller,
                    XCAConstants.REGREP_QUERY,
                    XCAConstants.ADHOC_QUERY_REQUEST);
            org.apache.axiom.om.OMNamespace namespace = factory.createOMNamespace(XCAConstants.REGREP_QUERY,
                    null);
            return factory.createOMElement(source, XCAConstants.ADHOC_QUERY_REQUEST, namespace);
        } catch (javax.xml.bind.JAXBException bex) {
            throw org.apache.axis2.AxisFault.makeFault(bex);
        }
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

        org.apache.axiom.soap.SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    private org.apache.axiom.om.OMElement toOM(oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {
        try {

            javax.xml.bind.Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            org.apache.axiom.om.OMFactory factory = org.apache.axiom.om.OMAbstractFactory.getOMFactory();

            RespondingGateway_ServiceStub.JaxbRIDataSource source = new RespondingGateway_ServiceStub.JaxbRIDataSource(oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse.class,
                    param,
                    marshaller,
                    XCAConstants.REGREP_QUERY,
                    XCAConstants.ADHOC_QUERY_RESPONSE);
            org.apache.axiom.om.OMNamespace namespace = factory.createOMNamespace(XCAConstants.REGREP_QUERY,
                    null);
            return factory.createOMElement(source, XCAConstants.ADHOC_QUERY_RESPONSE, namespace);
        } catch (javax.xml.bind.JAXBException bex) {
            throw org.apache.axis2.AxisFault.makeFault(bex);
        }
    }

    @SuppressWarnings("unused")
    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {
        org.apache.axiom.soap.SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    private org.apache.axiom.om.OMElement toOM(RetrieveDocumentSetRequestType param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {
        try {

            javax.xml.bind.Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            org.apache.axiom.om.OMFactory factory = org.apache.axiom.om.OMAbstractFactory.getOMFactory();

            RespondingGateway_ServiceStub.JaxbRIDataSource source = new RespondingGateway_ServiceStub.JaxbRIDataSource(RetrieveDocumentSetRequestType.class,
                    param,
                    marshaller,
                    XCAConstants.SOAP_HEADERS.NAMESPACE_URI,
                    XCAConstants.RETRIEVE_DOCUMENTSET_REQUEST);
            org.apache.axiom.om.OMNamespace namespace = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.NAMESPACE_URI,
                    null);
            return factory.createOMElement(source, XCAConstants.RETRIEVE_DOCUMENTSET_REQUEST, namespace);
        } catch (javax.xml.bind.JAXBException bex) {
            throw org.apache.axis2.AxisFault.makeFault(bex);
        }
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, RetrieveDocumentSetRequestType param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

        org.apache.axiom.soap.SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    private org.apache.axiom.om.OMElement toOM(RetrieveDocumentSetResponseType param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {
        try {

            javax.xml.bind.Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            org.apache.axiom.om.OMFactory factory = org.apache.axiom.om.OMAbstractFactory.getOMFactory();

            RespondingGateway_ServiceStub.JaxbRIDataSource source = new RespondingGateway_ServiceStub.JaxbRIDataSource(RetrieveDocumentSetResponseType.class,
                    param,
                    marshaller,
                    XCAConstants.SOAP_HEADERS.NAMESPACE_URI,
                    XCAConstants.RETRIEVE_DOCUMENTSET_RESPONSE);
            org.apache.axiom.om.OMNamespace namespace = factory.createOMNamespace(XCAConstants.SOAP_HEADERS.NAMESPACE_URI,
                    null);
            return factory.createOMElement(source, XCAConstants.RETRIEVE_DOCUMENTSET_RESPONSE, namespace);
        } catch (javax.xml.bind.JAXBException bex) {
            throw org.apache.axis2.AxisFault.makeFault(bex);
        }
    }

    @SuppressWarnings("unused")
    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, RetrieveDocumentSetResponseType param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {
        org.apache.axiom.soap.SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    /**
     * get the default envelope
     */
    @SuppressWarnings("unused")
    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory) {
        return factory.getDefaultEnvelope();
    }

    @SuppressWarnings("unchecked")
    private java.lang.Object fromOM(
            org.apache.axiom.om.OMElement param,
            java.lang.Class type,
            java.util.Map extraNamespaces) throws org.apache.axis2.AxisFault {
        try {
            javax.xml.bind.JAXBContext context = wsContext;
            javax.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();

            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();
        } catch (javax.xml.bind.JAXBException bex) {
            throw org.apache.axis2.AxisFault.makeFault(bex);
        }
    }

    private EventLog createAndSendEventLogQuery(AdhocQueryRequest request, AdhocQueryResponse response,
                                                org.apache.axis2.context.MessageContext msgContext,
                                                org.apache.axiom.soap.SOAPEnvelope _returnEnv,
                                                org.apache.axiom.soap.SOAPEnvelope env,
                                                Assertion idAssertion, Assertion trcAssertion,
                                                String address, String classCode) {
        EventLog eventLog = EventLogClientUtil.prepareEventLog(msgContext, _returnEnv, address);
        EventLogClientUtil.logIdAssertion(eventLog, idAssertion);
        EventLogClientUtil.logTrcAssertion(eventLog, trcAssertion);
        EventLogUtil.prepareXCACommonLogQuery(eventLog, request, response, classCode);
        eventLog.setNcpSide(NcpSide.NCP_B);
        EventLogClientUtil.sendEventLog(eventLog);
        return eventLog;
    }

    private EventLog createAndSendEventLogRetrieve(RetrieveDocumentSetRequestType request, RetrieveDocumentSetResponseType response,
                                                   org.apache.axis2.context.MessageContext msgContext,
                                                   org.apache.axiom.soap.SOAPEnvelope _returnEnv,
                                                   org.apache.axiom.soap.SOAPEnvelope env,
                                                   Assertion idAssertion, Assertion trcAssertion,
                                                   String address, String classCode) {
        EventLog eventLog = EventLogClientUtil.prepareEventLog(msgContext, _returnEnv, address);
        EventLogClientUtil.logIdAssertion(eventLog, idAssertion);
        EventLogClientUtil.logTrcAssertion(eventLog, trcAssertion);
        EventLogUtil.prepareXCACommonLogRetrieve(eventLog, request, response, classCode);
        eventLog.setNcpSide(NcpSide.NCP_B);
        EventLogClientUtil.sendEventLog(eventLog);
        return eventLog;
    }

    class JaxbRIDataSource implements org.apache.axiom.om.OMDataSource {

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
        private final javax.xml.bind.Marshaller marshaller;
        /**
         * Namespace
         */
        private String nsuri;
        /**
         * Local name
         */
        private String name;

        /**
         * Constructor from object and marshaller.
         *
         * @param obj
         * @param marshaller
         */
        public JaxbRIDataSource(Class clazz, Object obj, javax.xml.bind.Marshaller marshaller, String nsuri, String name) {
            this.outClazz = clazz;
            this.outObject = obj;
            this.marshaller = marshaller;
            this.nsuri = nsuri;
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public void serialize(java.io.OutputStream output, org.apache.axiom.om.OMOutputFormat format) throws javax.xml.stream.XMLStreamException {
            try {
                marshaller.marshal(new javax.xml.bind.JAXBElement(
                        new javax.xml.namespace.QName(nsuri, name), outObject.getClass(), outObject), output);
            } catch (javax.xml.bind.JAXBException e) {
                throw new javax.xml.stream.XMLStreamException(XCAConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        @SuppressWarnings("unchecked")
        public void serialize(java.io.Writer writer, org.apache.axiom.om.OMOutputFormat format) throws javax.xml.stream.XMLStreamException {
            try {
                marshaller.marshal(new javax.xml.bind.JAXBElement(
                        new javax.xml.namespace.QName(nsuri, name), outObject.getClass(), outObject), writer);
            } catch (javax.xml.bind.JAXBException e) {
                throw new javax.xml.stream.XMLStreamException(XCAConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        @SuppressWarnings("unchecked")
        public void serialize(javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
            try {
                marshaller.marshal(new javax.xml.bind.JAXBElement(
                        new javax.xml.namespace.QName(nsuri, name), outObject.getClass(), outObject), xmlWriter);
            } catch (javax.xml.bind.JAXBException e) {
                throw new javax.xml.stream.XMLStreamException(XCAConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        @SuppressWarnings("unchecked")
        public javax.xml.stream.XMLStreamReader getReader() throws javax.xml.stream.XMLStreamException {

            try {
                org.apache.axiom.om.impl.builder.SAXOMBuilder builder = new org.apache.axiom.om.impl.builder.SAXOMBuilder();
                javax.xml.bind.Marshaller marshaller = wsContext.createMarshaller();
                marshaller.marshal(new javax.xml.bind.JAXBElement(
                        new javax.xml.namespace.QName(nsuri, name), outObject.getClass(), outObject), builder);

                return builder.getRootElement().getXMLStreamReader();
            } catch (javax.xml.bind.JAXBException e) {
                throw new javax.xml.stream.XMLStreamException(XCAConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }
    }
}
