package tr.com.srdc.epsos.ws.xcpd.client;

import com.spirit.epsos.cc.adc.EadcEntry;
import ee.affecto.epsos.util.EventLogClientUtil;
import ee.affecto.epsos.util.EventLogUtil;
import epsos.ccd.gnomon.auditmanager.EventLog;
import eu.epsos.pt.eadc.EadcUtilWrapper;
import eu.epsos.pt.eadc.util.EadcUtil;
import eu.epsos.util.xcpd.XCPDConstants;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.eadc.ServiceType;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.RegisteredService;
import eu.europa.ec.sante.ehdsi.openncp.pt.common.DynamicDiscoveryService;
import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.SAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12HeaderBlockImpl;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.XMLUtils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang.StringUtils;
import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201306UV02;
import org.opensaml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.*;

/**
 * RespondingGateway_ServiceStub java implementation.
 *
 * @author SRDC<code> - epsos@srdc.com.tr>
 * @author Aarne Roosi<code> - Aarne.Roosi@Affecto.com</code>
 * @author Luís Pinto<code> - luis.pinto@iuz.pt</code>
 */
public class RespondingGateway_ServiceStub extends org.apache.axis2.client.Stub {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespondingGateway_ServiceStub.class);
    private static final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final javax.xml.bind.JAXBContext wsContext;
    private static int counter = 0;

    static {
        LOGGER.debug("Loading the WS-Security init libraries in RespondingGateway_ServiceStub XCPD");
        org.apache.xml.security.Init.init();
    }

    static {
        javax.xml.bind.JAXBContext jc;
        jc = null;
        try {
            jc = JAXBContext.newInstance(PRPAIN201305UV02.class, PRPAIN201306UV02.class);
        } catch (JAXBException ex) {
            LOGGER.error("Unable to create JAXBContext: '{}'", ex.getMessage(), ex);
            ex.printStackTrace(System.err);
            Runtime.getRuntime().exit(-1);
        } finally {
            wsContext = jc;
        }
    }

    protected org.apache.axis2.description.AxisOperation[] _operations;
    // HashMap to keep the fault mapping
    private HashMap faultExceptionNameMap = new HashMap();
    private HashMap faultExceptionClassNameMap = new HashMap();
    private HashMap faultMessageMap = new HashMap();
    private String countryCode;
    private Date transactionStartTime;
    private Date transactionEndTime;
    private QName[] opNameArray = null;

    /**
     * Constructor that takes in a configContext
     */
    public RespondingGateway_ServiceStub(ConfigurationContext configurationContext, String targetEndpoint) {
        this(configurationContext, targetEndpoint, false);
    }

    /**
     * Constructor that takes in a configContext and useseperate listner
     */
    public RespondingGateway_ServiceStub(org.apache.axis2.context.ConfigurationContext configurationContext, String targetEndpoint, boolean useSeparateListener) {
        // To populate AxisService
        populateAxisService();
        populateFaults();
        try {
            _serviceClient = new org.apache.axis2.client.ServiceClient(configurationContext, _service);
        } catch (AxisFault ex) {
            throw new RuntimeException(ex);
        }

        _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(targetEndpoint));
        _serviceClient.getOptions().setUseSeparateListener(useSeparateListener);
        //  Wait time after which a client times out in a blocking scenario: 3 minutes
        _serviceClient.getOptions().setTimeOutInMilliSeconds(180000);

        // Set the soap version
        _serviceClient.getOptions().setSoapVersionURI(org.apache.axiom.soap.SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);

        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();

        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(20);
        multiThreadedHttpConnectionManager.setParams(params);
        HttpClient httpClient = new HttpClient(multiThreadedHttpConnectionManager);

        this._getServiceClient().getServiceContext().getConfigurationContext().setProperty(HTTPConstants.REUSE_HTTP_CLIENT, false);
        this._getServiceClient().getServiceContext().getConfigurationContext().setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpClient);
        this._getServiceClient().getServiceContext().getConfigurationContext().setProperty(HTTPConstants.AUTO_RELEASE_CONNECTION, false);
    }

    /**
     * Constructor taking the target endpoint
     */
    public RespondingGateway_ServiceStub(String targetEndpoint) {
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

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    private void populateAxisService() {

        // creating the Service with a unique name
        _service = new org.apache.axis2.description.AxisService("RespondingGateway_Service" + getUniqueSuffix());
        addAnonymousOperations();

        // creating the operations
        org.apache.axis2.description.AxisOperation __operation;
        _operations = new org.apache.axis2.description.AxisOperation[1];
        __operation = new org.apache.axis2.description.OutInAxisOperation();
        __operation.setName(new javax.xml.namespace.QName(XCPDConstants.SOAP_HEADERS.NAMESPACE_URI, XCPDConstants.SOAP_HEADERS.NAMESPACE_REQUEST_LOCAL_PART));
        _service.addOperation(__operation);
        _operations[0] = __operation;

    }

    // populates the faults
    private void populateFaults() {
    }

    /**
     * Auto generated method signature
     *
     * @param pRPA_IN201305UV02
     * @param idAssertion
     * @return
     */
    public org.hl7.v3.PRPAIN201306UV02 respondingGateway_PRPA_IN201305UV02(PRPAIN201305UV02 pRPA_IN201305UV02, Assertion idAssertion) {
        org.apache.axis2.context.MessageContext _messageContext = null;

        LOGGER.info("respondingGateway_PRPA_IN201305UV02('{}', '{}'", pRPA_IN201305UV02.getId().getRoot(), idAssertion.getID());

        try {

            // XCPD request start time
            long start = System.currentTimeMillis();

            OperationClient operationClient = _serviceClient.createClient(_operations[0].getName());
            operationClient.getOptions().setAction(XCPDConstants.SOAP_HEADERS.REQUEST_ACTION);
            operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
            addPropertyToOperationClient(operationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

            SOAPFactory soapFactory = getFactory(operationClient.getOptions().getSoapVersionURI());

            /* create SOAP envelope with that payload */
            org.apache.axiom.soap.SOAPEnvelope env;
            env = toEnvelope(soapFactory, pRPA_IN201305UV02, optimizeContent(
                    new QName(XCPDConstants.SOAP_HEADERS.NAMESPACE_URI, XCPDConstants.SOAP_HEADERS.NAMESPACE_REQUEST_LOCAL_PART)));

            /* adding SOAP soap_headers */
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMNamespace ns2 = factory.createOMNamespace(XCPDConstants.SOAP_HEADERS.OM_NAMESPACE, "");

            SOAPHeaderBlock action = new SOAP12HeaderBlockImpl("Action", ns2, soapFactory);
            OMNode node = factory.createOMText(XCPDConstants.SOAP_HEADERS.REQUEST_ACTION);
            action.addChild(node);
            OMAttribute att = factory.createOMAttribute(XCPDConstants.SOAP_HEADERS.MUST_UNDERSTAND, env.getNamespace(), "1");
            action.addAttribute(att);

            SOAPHeaderBlock id = new SOAP12HeaderBlockImpl("MessageID", ns2, soapFactory);
            OMNode node2 = factory.createOMText(Constants.UUID_PREFIX + UUID.randomUUID().toString());
            id.addChild(node2);

            OMNamespace ns = factory.createOMNamespace(XCPDConstants.SOAP_HEADERS.SECURITY_XSD, "wsse");
            SOAPHeaderBlock shbSecurity = new SOAP12HeaderBlockImpl("Security", ns, soapFactory);

            try {
                shbSecurity.addChild(XMLUtils.toOM(idAssertion.getDOM()));
                _serviceClient.addHeader(shbSecurity);
            } catch (Exception ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
            /* The WSA To header is not being manually added, it's added by the client-connector axis2.xml configurations
            (which globally engages the addressing module, adding the wsa:To header based on the endpoint value from the transport)
            based on the assumption that these IHE Service clients will always be coupled with client-connector, which may not be
            the case in the future. When that happens, we may need to revisit this code to add the To header like it's done in the IHE XCA service client.
            See issues EHNCP-1141 and EHNCP-1168. */
            _serviceClient.addHeader(action);
            _serviceClient.addHeader(id);
            _serviceClient.addHeadersToEnvelope(env);

            /* set the message context with that soap envelope */
            org.apache.axis2.context.MessageContext messageContext = new org.apache.axis2.context.MessageContext();
            messageContext.setEnvelope(env);

            /* add the message contxt to the operation client */
            operationClient.addMessageContext(messageContext);

            /* Log soap request */

            String logRequestBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                    String logRequestMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(env));
                    loggerClinical.debug(XCPDConstants.LOG.OUTGOING_XCPD_MESSAGE + System.getProperty("line.separator") + logRequestMsg);
                }
                logRequestBody = XMLUtil.prettyPrint(XMLUtils.toDOM(env.getBody().getFirstElement()));

                //  Invoke NRO - Considered as optional.
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            // XCPD response end time
            long end = System.currentTimeMillis();
            LOGGER.info("XCPD REQUEST-RESPONSE TIME: '{}'", (end - start) / 1000.0);


            // Validation start time
            start = System.currentTimeMillis();

            /* Validate Request Messages */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validatePatientDemographicRequest(logRequestBody, NcpSide.NCP_B);
            }

            // Transaction end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD VALIDATION REQ TIME: '{}'", (end - start) / 1000.0);


            // Transaction start time
            start = System.currentTimeMillis();

            /* execute the operation client */
            transactionStartTime = new Date();
            try {
                operationClient.execute(true);
            } catch (AxisFault e) {
                LOGGER.error("Axis Fault: Code-'{}' Message-'{}'", e.getFaultCode(), e.getMessage());
                LOGGER.error("Trying to automatically solve the problem by fetching configurations from the Central Services...");
                DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
                String value = dynamicDiscoveryService.getEndpointUrl(
                        this.countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.PATIENT_IDENTIFICATION_SERVICE, true);

                if (StringUtils.isNotEmpty(value)) {

                    /* if we get something from the Central Services, then we retry the request */
                    /* correctly sets the Transport information with the new endpoint */
                    LOGGER.debug("Retrying the request with the new configurations: [" + value + "]");
                    _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(value));

                    /* we need a new OperationClient, otherwise we'll face the error "A message was added that is not valid. However, the operation context was complete." */
                    OperationClient newOperationClient = _serviceClient.createClient(_operations[0].getName());
                    newOperationClient.getOptions().setAction(XCPDConstants.SOAP_HEADERS.REQUEST_ACTION);
                    newOperationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
                    addPropertyToOperationClient(newOperationClient, org.apache.axis2.description.WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

                    SOAPFactory newSoapFactory = getFactory(newOperationClient.getOptions().getSoapVersionURI());

                    /* we need to create a new SOAP payload so that the wsa:To header is correctly set
                    (i.e., copied from the Transport information to the wsa:To during the running of the Addressing Phase,
                    as defined by the global engagement of the addressing module in axis2.xml). The old payload still contains the old endpoint. */
                    org.apache.axiom.soap.SOAPEnvelope newEnv;
                    newEnv = toEnvelope(newSoapFactory, pRPA_IN201305UV02,
                            optimizeContent(new QName(XCPDConstants.SOAP_HEADERS.NAMESPACE_URI, XCPDConstants.SOAP_HEADERS.NAMESPACE_REQUEST_LOCAL_PART)));

                    /* we set the previous headers in the new SOAP envelope. Note: the wsa:To header is not manually set (only Action and MessageID are) but instead handled by the
                    axis2 configuration of client-connector (my assumption). This may have impact if we decouple client-connector from the IHE service clients. If
                    they are decoupled, we most probably have to add the To header manually like it's done in the IHE XCA client, both here and in the initial
                    request. See issues EHNCP-1141 and EHNCP-1168. */
                    _serviceClient.addHeadersToEnvelope(newEnv);

                    /* we create a new Message Context with the new SOAP envelope */
                    MessageContext newMessageContext = new MessageContext();
                    newMessageContext.setEnvelope(newEnv);

                    /* add the new message context to the new operation client */
                    newOperationClient.addMessageContext(newMessageContext);
                    /* we retry the request */
                    newOperationClient.execute(true);
                    /* we need to reset the previous variables with the new content, to be used later */
                    operationClient = newOperationClient;
                    messageContext = newMessageContext;
                    env = newEnv;
                    LOGGER.debug("Successfully retried the request! Proceeding with the normal workflow...");
                } else {
                    /* if we cannot solve this issue through the Central Services, then there's nothing we can do, so we let it be thrown */
                    LOGGER.error("Could not find configurations in the Central Services for [" + this.countryCode.toLowerCase(Locale.ENGLISH)
                            + RegisteredService.PATIENT_IDENTIFICATION_SERVICE.getServiceName() + "], the service will fail.");
                    throw e;
                }
            }

            MessageContext _returnMessageContext = operationClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();
            transactionEndTime = new Date();


            // Transaction end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD TRANSACTION TIME: '{}'", (end - start) / 1000.0);


            // Validation start time
            start = System.currentTimeMillis();

            /* Log soap response */
            String logResponseBody;
            try {
                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                    String logResponseMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(_returnEnv));
                    loggerClinical.debug(XCPDConstants.LOG.INCOMING_XCPD_MESSAGE + System.getProperty("line.separator") + logResponseMsg);
                }
                logResponseBody = XMLUtil.prettyPrint(XMLUtils.toDOM(_returnEnv.getBody().getFirstElement()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            /* Validate Response Messages */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validatePatientDemographicResponse(logResponseBody, NcpSide.NCP_B);
            }
            Object object = fromOM(_returnEnv.getBody().getFirstElement(), PRPAIN201306UV02.class, getEnvelopeNamespaces(_returnEnv));


            // Validation end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD VALIDATION RESPONSE TIME: '{}'", (end - start) / 1000.0);


            // eADC start time
            start = System.currentTimeMillis();

            //  Invoke NRR - Considered as optional.

            //  Invoque eADC
            try {
                EadcUtilWrapper.invokeEadc(messageContext, _returnMessageContext, this._getServiceClient(), null,
                        transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.XCPD, EadcUtil.Direction.OUTBOUND,
                        ServiceType.PATIENT_IDENTIFICATION_QUERY);
            } catch (Exception ex) {
                LOGGER.error("EADC INVOCATION FAILED: '{}'", ex.getMessage(), ex);
            }

            // eADC end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD eADC TIME: '{}'", (end - start) / 1000.0);


            // Audit start time
            start = System.currentTimeMillis();

            // eventLog
            EventLog eventLog = createAndSendEventLog(pRPA_IN201305UV02, (org.hl7.v3.PRPAIN201306UV02) object, messageContext,
                    _returnEnv, env, idAssertion, this._getServiceClient().getOptions().getTo().getAddress());
            LOGGER.info("[Audit] EventLog: '{}'", eventLog.getEventType());

            try {
                LOGGER.info("SOAP MESSAGE IS: '{}'", XMLUtils.toDOM(_returnEnv));
            } catch (Exception ex) {
                LOGGER.error(null, ex);
            }

            // Audit end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD AUDIT TIME: '{}'", (end - start) / 1000.0);

            return (org.hl7.v3.PRPAIN201306UV02) object;

        } catch (AxisFault f) {
            // TODO A.R. Audit log SOAP Fault is still missing
            // LOGGER.error(f.getLocalizedMessage(), f);

            OMElement faultElt = f.getDetail();

            if (faultElt != null) {

                if (faultExceptionNameMap.containsKey(faultElt.getQName())) {

                    /* make the fault by reflection */
                    try {
                        String exceptionClassName = (String) faultExceptionClassNameMap.get(faultElt.getQName());
                        Class exceptionClass = Class.forName(exceptionClassName);
                        Exception ex = (Exception) exceptionClass.newInstance();
                        // message class
                        String messageClassName = (String) faultMessageMap.get(faultElt.getQName());
                        Class messageClass = Class.forName(messageClassName);
                        Object messageObject = fromOM(faultElt, messageClass, null);
                        Method m = exceptionClass.getMethod("setFaultMessage", messageClass);
                        m.invoke(ex, messageObject);

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);

                    } catch (Exception e) {
                        // we cannot instantiate the class - throw the original Axis fault
                        throw new RuntimeException(f.getMessage(), f);
                    }
                }
            }
            throw new RuntimeException(f.getMessage(), f);

        } finally {
            if (_messageContext != null && _messageContext.getTransportOut() != null && _messageContext.getTransportOut().getSender() != null) {
                try {
                    _messageContext.getTransportOut().getSender().cleanup(_messageContext);
                } catch (AxisFault ex) {
                    LOGGER.error(null, ex);
                }
            }
        }
    }

    /**
     * A utility method that copies the namespaces from the SOAPEnvelope
     */
    private Map getEnvelopeNamespaces(SOAPEnvelope env) {

        Map returnMap = new java.util.HashMap();
        Iterator namespaceIterator = env.getAllDeclaredNamespaces();

        while (namespaceIterator.hasNext()) {
            OMNamespace ns = (OMNamespace) namespaceIterator.next();
            returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
        }

        return returnMap;
    }

    private boolean optimizeContent(QName opName) {

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

    private OMElement toOM(PRPAIN201305UV02 param, boolean optimizeContent) throws AxisFault {

        try {
            JAXBContext context = wsContext;
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            OMFactory factory = OMAbstractFactory.getOMFactory();

            JaxbRIDataSource source = new JaxbRIDataSource(PRPAIN201305UV02.class, param, marshaller,
                    XCPDConstants.HL7_V3_NAMESPACE_URI, XCPDConstants.PATIENT_DISCOVERY_REQUEST);
            OMNamespace namespace = factory.createOMNamespace(XCPDConstants.HL7_V3_NAMESPACE_URI, null);
            return factory.createOMElement(source, XCPDConstants.PATIENT_DISCOVERY_REQUEST, namespace);
        } catch (JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(SOAPFactory factory, PRPAIN201305UV02 param, boolean optimizeContent) throws AxisFault {

        SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    private OMElement toOM(PRPAIN201306UV02 param, boolean optimizeContent) throws AxisFault {

        try {
            JAXBContext context = wsContext;
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            OMFactory factory = OMAbstractFactory.getOMFactory();

            JaxbRIDataSource source = new JaxbRIDataSource(PRPAIN201306UV02.class, param, marshaller,
                    XCPDConstants.HL7_V3_NAMESPACE_URI, XCPDConstants.PATIENT_DISCOVERY_RESPONSE);
            OMNamespace namespace = factory.createOMNamespace(XCPDConstants.HL7_V3_NAMESPACE_URI, null);
            return factory.createOMElement(source, XCPDConstants.PATIENT_DISCOVERY_RESPONSE, namespace);
        } catch (JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(SOAPFactory factory, PRPAIN201306UV02 param, boolean optimizeContent) throws AxisFault {

        SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    /**
     * get the default envelope
     */
    private SOAPEnvelope toEnvelope(SOAPFactory factory) {

        return factory.getDefaultEnvelope();
    }

    private Object fromOM(OMElement param, Class type, Map extraNamespaces) throws AxisFault {

        try {
            JAXBContext context = wsContext;
            Unmarshaller unmarshaller = context.createUnmarshaller();

            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();

        } catch (JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private EventLog createAndSendEventLog(PRPAIN201305UV02 sended, PRPAIN201306UV02 received, MessageContext msgContext,
                                           SOAPEnvelope _returnEnv, SOAPEnvelope env, Assertion idAssertion, String address) {

        EventLog eventLog = EventLogClientUtil.prepareEventLog(msgContext, _returnEnv, address);
        eventLog.setNcpSide(NcpSide.NCP_B);
        EventLogClientUtil.logIdAssertion(eventLog, idAssertion);
        EventLogUtil.prepareXCPDCommonLog(eventLog, sended, received);
        EventLogClientUtil.sendEventLog(eventLog);
        return eventLog;
    }

    class JaxbRIDataSource implements OMDataSource {

        /**
         * Bound object for output.
         */
        private final Object outObject;
        /**
         * Bound class for output.
         */
        private final Class outClazz;
        /**
         * Marshaller.
         */
        private final Marshaller marshaller;
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
        public JaxbRIDataSource(Class clazz, Object obj, Marshaller marshaller, String nsuri, String name) {
            this.outClazz = clazz;
            this.outObject = obj;
            this.marshaller = marshaller;
            this.nsuri = nsuri;
            this.name = name;
        }

        public void serialize(OutputStream output, OMOutputFormat format) throws XMLStreamException {

            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), output);

            } catch (JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }

        public void serialize(Writer writer, OMOutputFormat format) throws XMLStreamException {
            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), writer);

            } catch (JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }

        public void serialize(XMLStreamWriter xmlWriter) throws XMLStreamException {
            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), xmlWriter);

            } catch (JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }

        public XMLStreamReader getReader() throws XMLStreamException {

            try {
                JAXBContext context = wsContext;
                SAXOMBuilder builder = new SAXOMBuilder();
                Marshaller marshaller = context.createMarshaller();
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), builder);

                return builder.getRootElement().getXMLStreamReader();

            } catch (JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }
    }
}
