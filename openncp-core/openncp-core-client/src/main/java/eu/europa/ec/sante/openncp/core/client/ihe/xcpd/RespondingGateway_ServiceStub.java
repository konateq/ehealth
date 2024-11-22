package eu.europa.ec.sante.openncp.core.client.ihe.xcpd;

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
import eu.europa.ec.sante.openncp.core.common.constants.ihe.xcpd.XCPDConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201306UV02;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcEntry;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtilWrapper;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.ServiceType;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogClientUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogUtil;
import org.apache.axiom.om.*;
import org.apache.axiom.om.ds.AbstractOMDataSource;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
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

/**
 * RespondingGateway_ServiceStub java implementation.
 */
public class RespondingGateway_ServiceStub extends Stub {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespondingGateway_ServiceStub.class);
    private static final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final JAXBContext wsContext;
    private static int counter = 0;

    static {
        LOGGER.debug("Loading the WS-Security init libraries in RespondingGateway_ServiceStub xcpd");
        org.apache.xml.security.Init.init();
    }

    static {
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(PRPAIN201305UV02.class, PRPAIN201306UV02.class);
        } catch (final JAXBException ex) {
            LOGGER.error("Unable to create JAXBContext: '{}'", ex.getMessage(), ex);
            Runtime.getRuntime().exit(-1);
        } finally {
            wsContext = jaxbContext;
        }
    }

    // HashMap to keep the fault mapping
    private final HashMap faultExceptionNameMap = new HashMap();
    private final HashMap faultExceptionClassNameMap = new HashMap();
    private final HashMap faultMessageMap = new HashMap();
    private final QName[] opNameArray = null;
    protected AxisOperation[] _operations;
    private String countryCode;

    /**
     * Constructor that takes in a configContext
     */
    public RespondingGateway_ServiceStub(final ConfigurationContext configurationContext, final String targetEndpoint) {
        this(configurationContext, targetEndpoint, false);
    }

    /**
     * Constructor that takes in a configContext and useseperate listner
     */
    public RespondingGateway_ServiceStub(final ConfigurationContext configurationContext, final String targetEndpoint, final boolean useSeparateListener) {

        // To populate AxisService
        populateAxisService();
        populateFaults();
        try {
            _serviceClient = new ServiceClient(configurationContext, _service);
        } catch (final AxisFault ex) {
            throw new RuntimeException(ex);
        }

        _serviceClient.getOptions().setTo(new EndpointReference(targetEndpoint));
        _serviceClient.getOptions().setUseSeparateListener(useSeparateListener);
        //  Wait time after which a client times out in a blocking scenario: 3 minutes
        _serviceClient.getOptions().setTimeOutInMilliSeconds(180000);

        // Set the soap version
        _serviceClient.getOptions().setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);

        // Enabling Axis2 - SSL 2 ways communication (not active by default).
        try {
            _serviceClient.getServiceContext().getConfigurationContext()
                    .setProperty(HTTPConstants.CACHED_HTTP_CLIENT, HttpsClientConfiguration.getDefaultSSLClient());
            _serviceClient.getServiceContext().getConfigurationContext()
                    .setProperty(HTTPConstants.REUSE_HTTP_CLIENT, false);
        } catch (final NoSuchAlgorithmException | KeyManagementException | IOException | CertificateException |
                       KeyStoreException | UnrecoverableKeyException e) {
            throw new RuntimeException("SSL Context cannot be initialized", e);
        }
    }

    /**
     * Constructor taking the target endpoint
     */
    public RespondingGateway_ServiceStub(final String targetEndpoint) {
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

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    private void populateAxisService() {

        // creating the Service with a unique name
        _service = new AxisService("RespondingGateway_Service" + getUniqueSuffix());
        addAnonymousOperations();

        // creating the operations
        _operations = new AxisOperation[1];
        final AxisOperation axisOperation = new OutInAxisOperation();
        axisOperation.setName(new QName(XCPDConstants.SOAP_HEADERS.NAMESPACE_URI, XCPDConstants.SOAP_HEADERS.NAMESPACE_REQUEST_LOCAL_PART));
        _service.addOperation(axisOperation);
        _operations[0] = axisOperation;
    }

    // populates the faults
    private void populateFaults() {
    }

    /**
     * Auto generated method signature
     *
     * @param prpain201305UV02
     * @param assertionMap
     * @return
     */
    public PRPAIN201306UV02 respondingGateway_PRPA_IN201305UV02(final PRPAIN201305UV02 prpain201305UV02, final Map<AssertionType, Assertion> assertionMap, final String dstHomeCommunityId) throws NoPatientIdDiscoveredException {

        MessageContext _messageContext = null;
        MessageContext _returnMessageContext = null;
        MessageContext messageContext = null;

        String eadcError = "";

        // Start Date for eADC
        Date transactionStartTime = new Date();
        // End Date for eADC
        Date transactionEndTime = new Date();

        LOGGER.info("respondingGateway_PRPA_IN201305UV02('{}', '{}'", prpain201305UV02.getId().getRoot(),
                assertionMap.get(AssertionType.HCP).getID());

        try {
            // TMP
            // XCPD request start time
            long start = System.currentTimeMillis();

            var operationClient = _serviceClient.createClient(_operations[0].getName());
            operationClient.getOptions().setAction(XCPDConstants.SOAP_HEADERS.REQUEST_ACTION);
            operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
            addPropertyToOperationClient(operationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

            final var soapFactory = getFactory(operationClient.getOptions().getSoapVersionURI());

            /* create SOAP envelope with that payload */
            SOAPEnvelope env = toEnvelope(soapFactory, prpain201305UV02, optimizeContent(
                    new QName(XCPDConstants.SOAP_HEADERS.NAMESPACE_URI, XCPDConstants.SOAP_HEADERS.NAMESPACE_REQUEST_LOCAL_PART)));

            /* adding SOAP soap_headers */
            final var factory = OMAbstractFactory.getOMFactory();
            final var ns2 = factory.createOMNamespace(XCPDConstants.SOAP_HEADERS.OM_NAMESPACE, "");
            final var soapHeaderBlock = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("Action", ns2);
            final OMNode node = factory.createOMText(XCPDConstants.SOAP_HEADERS.REQUEST_ACTION);
            soapHeaderBlock.addChild(node);
            final var omAttribute = factory.createOMAttribute(XCPDConstants.SOAP_HEADERS.MUST_UNDERSTAND, env.getNamespace(), "1");
            soapHeaderBlock.addAttribute(omAttribute);

            final var headerBlockMessageId = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("MessageID", ns2);
            final OMNode node2 = factory.createOMText(Constants.UUID_PREFIX + UUID.randomUUID());
            headerBlockMessageId.addChild(node2);

            final var omNamespace = factory.createOMNamespace(XCPDConstants.SOAP_HEADERS.SECURITY_XSD, "wsse");
            final var headerSecurity = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock("Security", omNamespace);

            try {
                if (assertionMap.containsKey(AssertionType.NOK)) {
                    final var assertionNextOfKin = assertionMap.get(AssertionType.NOK);
                    headerSecurity.addChild(XMLUtils.toOM(assertionNextOfKin.getDOM()));
                }
                final var assertionId = assertionMap.get(AssertionType.HCP);
                headerSecurity.addChild(XMLUtils.toOM(assertionId.getDOM()));

                _serviceClient.addHeader(headerSecurity);
            } catch (final Exception ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
            /* The WSA To header is not being manually added, it's added by the client-connector axis2.xml configurations
            (which globally engages the addressing module, adding the wsa:To header based on the endpoint value from the transport)
            based on the assumption that these IHE Service clients will always be coupled with client-connector, which may not be
            the case in the future. When that happens, we may need to revisit this code to add the To header like it's done in the IHE XCA service client.
            See issues EHNCP-1141 and EHNCP-1168. */
            _serviceClient.addHeader(soapHeaderBlock);
            _serviceClient.addHeader(headerBlockMessageId);
            _serviceClient.addHeadersToEnvelope(env);

            /* set the message context with that soap envelope */
            messageContext = new MessageContext();
            messageContext.setEnvelope(env);
            _messageContext = messageContext;

            // add the message context to the operation client
            operationClient.addMessageContext(messageContext);

            // Log soap request
            final String logRequestBody;
            try {
                if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                    final String logRequestMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(env));
                    loggerClinical.debug(XCPDConstants.LOG.OUTGOING_XCPD_MESSAGE, System.getProperty("line.separator") + logRequestMsg);
                }
                logRequestBody = XMLUtil.prettyPrint(XMLUtils.toDOM(env.getBody().getFirstElement()));
                // NRO NCPB_XCPD_REQ - "XCPD Request sent. EVIDENCE NRO"

            } catch (final Exception ex) {
                throw new NoPatientIdDiscoveredException(OpenNCPErrorCode.ERROR_PI_NO_MATCH,ex.getMessage());
            }

            // XCPD response end time
            long end = System.currentTimeMillis();
            LOGGER.info("XCPD REQUEST-RESPONSE TIME: '{}'", (end - start) / 1000.0);

            // Validation start time
            start = System.currentTimeMillis();

            // Validate Request Messages
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
            } catch (final AxisFault e) {
                LOGGER.error("Axis Fault: Code-'{}' Message-'{}'", e.getFaultCode(), e.getMessage());
                LOGGER.error("Trying to automatically solve the problem by fetching configurations from the Central Services...");
                final DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
                final String value = dynamicDiscoveryService.getEndpointUrl(
                        this.countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.PATIENT_IDENTIFICATION_SERVICE, true);

                if (StringUtils.isNotEmpty(value)) {

                    /* if we get something from the Central Services, then we retry the request */
                    /* correctly sets the Transport information with the new endpoint */
                    LOGGER.debug("Retrying the request with the new configurations: [{}]", value);
                    _serviceClient.getOptions().setTo(new EndpointReference(value));

                    /* we need a new OperationClient, otherwise we'll face the error "A message was added that is not valid. However, the operation context was complete." */
                    final OperationClient newOperationClient = _serviceClient.createClient(_operations[0].getName());
                    newOperationClient.getOptions().setAction(XCPDConstants.SOAP_HEADERS.REQUEST_ACTION);
                    newOperationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
                    addPropertyToOperationClient(newOperationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

                    final SOAPFactory newSoapFactory = getFactory(newOperationClient.getOptions().getSoapVersionURI());

                    /* we need to create a new SOAP payload so that the wsa:To header is correctly set
                    (i.e., copied from the Transport information to the wsa:To during the running of the Addressing Phase,
                    as defined by the global engagement of the addressing module in axis2.xml). The old payload still contains the old endpoint. */
                    final SOAPEnvelope newEnv = toEnvelope(newSoapFactory, prpain201305UV02,
                            optimizeContent(new QName(XCPDConstants.SOAP_HEADERS.NAMESPACE_URI, XCPDConstants.SOAP_HEADERS.NAMESPACE_REQUEST_LOCAL_PART)));

                    /* we set the previous headers in the new SOAP envelope. Note: the wsa:To header is not manually set (only Action and MessageID are) but instead handled by the
                    axis2 configuration of client-connector (my assumption). This may have impact if we decouple client-connector from the IHE service clients. If
                    they are decoupled, we most probably have to add the To header manually like it's done in the IHE XCA client, both here and in the initial
                    request. See issues EHNCP-1141 and EHNCP-1168. */
                    _serviceClient.addHeadersToEnvelope(newEnv);

                    /* we create a new Message Context with the new SOAP envelope */
                    final MessageContext newMessageContext = new MessageContext();
                    newMessageContext.setEnvelope(newEnv);

                    /* add the new message context to the new operation client */
                    newOperationClient.addMessageContext(newMessageContext);

                    /* we need to reset the previous variables with the new content, to be used later */
                    operationClient = newOperationClient;
                    messageContext = newMessageContext;
                    env = newEnv;
                    /* we retry the request */
                    newOperationClient.execute(true);
                    LOGGER.debug("Successfully retried the request! Proceeding with the normal workflow...");
                } else {
                    /* if we cannot solve this issue through the Central Services, then there's nothing we can do, so we let it be thrown */
                    eadcError = "Could not find configurations in the Central Services for [" + this.countryCode.toLowerCase(Locale.ENGLISH)
                            + RegisteredService.PATIENT_IDENTIFICATION_SERVICE.getServiceName() + "], the service will fail.";
                    LOGGER.error(eadcError);                    throw new NoPatientIdDiscoveredException(OpenNCPErrorCode.ERROR_PI_NO_MATCH, e);
                }
            }

            _returnMessageContext = operationClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            final SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();
            transactionEndTime = new Date();

            // TMP
            // Transaction end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD TRANSACTION TIME: '{}'", (end - start) / 1000.0);

            // TMP
            // Validation start time
            start = System.currentTimeMillis();

            /* Log soap response */
            final String logResponseBody;
            try {
                if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                    final String logResponseMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(_returnEnv));
                    loggerClinical.debug(XCPDConstants.LOG.INCOMING_XCPD_MESSAGE + System.getProperty("line.separator") + logResponseMsg);
                }
                logResponseBody = XMLUtil.prettyPrint(XMLUtils.toDOM(_returnEnv.getBody().getFirstElement()));
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }

            /* Validate Response Messages */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validatePatientDemographicResponse(logResponseBody, NcpSide.NCP_B);
            }
            final Object object = fromOM(_returnEnv.getBody().getFirstElement(), PRPAIN201306UV02.class, getEnvelopeNamespaces(_returnEnv));

            // TMP
            // Validation end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD VALIDATION RES TIME: '{}'", (end - start) / 1000.0);

            // TMP
            // eADC start time
            start = System.currentTimeMillis();

            /*
             * Invoque eADC
             */
            EadcUtilWrapper.invokeEadc(messageContext, _returnMessageContext, this._getServiceClient(), null,
                    transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.EADC,
                    EadcUtil.Direction.OUTBOUND, ServiceType.PATIENT_IDENTIFICATION_QUERY);

            // TMP
            // eADC end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD eADC TIME: '{}'", (end - start) / 1000.0);

            // TMP
            // Audit start time
            start = System.currentTimeMillis();

            // eventLog
            final EventLog eventLog = createAndSendEventLog(prpain201305UV02, (PRPAIN201306UV02) object, messageContext,
                    _returnEnv, env, assertionMap.get(AssertionType.HCP), this._getServiceClient().getOptions().getTo().getAddress(), dstHomeCommunityId);

            try {
                LOGGER.info("SOAP MESSAGE IS: '{}'", XMLUtils.toDOM(_returnEnv));
            } catch (final Exception ex) {
                LOGGER.error(null, ex);
            }

            // Audit end time
            end = System.currentTimeMillis();
            LOGGER.info("XCPD AUDIT TIME: '{}'", (end - start) / 1000.0);

            return (PRPAIN201306UV02) object;

        } catch (final AxisFault axisFault) {

            //  TODO A.R. Audit log SOAP Fault is still missing
            final OMElement faultElt = axisFault.getDetail();

            if (faultElt != null && faultExceptionNameMap.containsKey(faultElt.getQName())) {

                /* make the fault by reflection */
                try {
                    final String exceptionClassName = (String) faultExceptionClassNameMap.get(faultElt.getQName());
                    final var exceptionClass = Class.forName(exceptionClassName);
                    final Exception ex = (Exception) exceptionClass.getDeclaredConstructor().newInstance();
                    // message class
                    final String messageClassName = (String) faultMessageMap.get(faultElt.getQName());
                    final var messageClass = Class.forName(messageClassName);
                    final Object messageObject = fromOM(faultElt, messageClass, null);
                    final Method method = exceptionClass.getMethod("setFaultMessage", messageClass);
                    method.invoke(ex, messageObject);

                    throw new java.rmi.RemoteException(ex.getMessage(), ex);

                } catch (final Exception e) {
                    // we cannot instantiate the class - throw the original Axis fault
                    eadcError = axisFault.getMessage();
                    throw new RuntimeException(axisFault.getMessage(), axisFault);
                }
            }
            eadcError = OpenNCPErrorCode.ERROR_GENERIC_CONNECTION_NOT_POSSIBLE.getCode();
            throw new NoPatientIdDiscoveredException(OpenNCPErrorCode.ERROR_GENERIC_CONNECTION_NOT_POSSIBLE, "AxisFault");
        } finally {
            if (_messageContext != null && _messageContext.getTransportOut() != null && _messageContext.getTransportOut().getSender() != null) {
                try {
                    _messageContext.getTransportOut().getSender().cleanup(_messageContext);
                } catch (final AxisFault ex) {
                    LOGGER.error(null, ex);
                }
            }
            if(!eadcError.isEmpty()) {
                EadcUtilWrapper.invokeEadcFailure(messageContext, _returnMessageContext, this._getServiceClient(), null,
                        transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.EADC,
                        EadcUtil.Direction.OUTBOUND, ServiceType.PATIENT_IDENTIFICATION_QUERY, eadcError);
            }
        }
    }

    /**
     * A utility method that copies the namespaces from the SOAPEnvelope
     */
    private Map getEnvelopeNamespaces(final SOAPEnvelope env) {

        final Map returnMap = new java.util.HashMap();
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

    private OMElement toOM(final PRPAIN201305UV02 param, final boolean optimizeContent) throws AxisFault {

        try {
            final var marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            final var omFactory = OMAbstractFactory.getOMFactory();
            final var jaxbRIDataSource = new JaxbRIDataSource(PRPAIN201305UV02.class, param, marshaller,
                    XCPDConstants.HL7_V3_NAMESPACE_URI, XCPDConstants.PATIENT_DISCOVERY_REQUEST);
            final var omNamespace = omFactory.createOMNamespace(XCPDConstants.HL7_V3_NAMESPACE_URI, null);

            return omFactory.createOMElement(jaxbRIDataSource, XCPDConstants.PATIENT_DISCOVERY_REQUEST, omNamespace);
        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final PRPAIN201305UV02 param, final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    private OMElement toOM(final PRPAIN201306UV02 param, final boolean optimizeContent) throws AxisFault {

        try {
            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final JaxbRIDataSource source = new JaxbRIDataSource(PRPAIN201306UV02.class, param, marshaller,
                    XCPDConstants.HL7_V3_NAMESPACE_URI, XCPDConstants.PATIENT_DISCOVERY_RESPONSE);
            final OMNamespace namespace = factory.createOMNamespace(XCPDConstants.HL7_V3_NAMESPACE_URI, null);
            return factory.createOMElement(source, XCPDConstants.PATIENT_DISCOVERY_RESPONSE, namespace);
        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final PRPAIN201306UV02 param, final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));
        return envelope;
    }

    /**
     * get the default envelope
     */
    private SOAPEnvelope toEnvelope(final SOAPFactory factory) {

        return factory.getDefaultEnvelope();
    }

    private Object fromOM(final OMElement param, final Class type, final Map extraNamespaces) throws AxisFault {

        try {
            final JAXBContext context = wsContext;
            final Unmarshaller unmarshaller = context.createUnmarshaller();

            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private EventLog createAndSendEventLog(final PRPAIN201305UV02 sended, final PRPAIN201306UV02 received, final MessageContext msgContext,
                                           final SOAPEnvelope _returnEnv, final SOAPEnvelope env, final Assertion idAssertion, final String address, final String dstHomeCommunityId) {

        final EventLog eventLog = EventLogClientUtil.prepareEventLog(msgContext, _returnEnv, address, dstHomeCommunityId);
        eventLog.setNcpSide(NcpSide.NCP_B);
        EventLogClientUtil.logIdAssertion(eventLog, idAssertion);
        EventLogUtil.prepareXCPDCommonLog(eventLog, msgContext, sended, received);
        EventLogClientUtil.sendEventLog(eventLog);
        return eventLog;
    }

    class JaxbRIDataSource extends AbstractOMDataSource {

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

        public void serialize(final OutputStream output) throws XMLStreamException {

            try {
                marshaller.marshal(new javax.xml.bind.JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), output);

            } catch (final JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }

        public void serialize(final Writer writer) throws XMLStreamException {
            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), writer);

            } catch (final JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }

        public void serialize(final XMLStreamWriter xmlWriter) throws XMLStreamException {
            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), xmlWriter);

            } catch (final JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }

        public XMLStreamReader getReader() throws XMLStreamException {

            try {
                final var omDocument = OMAbstractFactory.getOMFactory().createOMDocument();
                wsContext.createMarshaller().marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), omDocument.getSAXResult());

                return omDocument.getOMDocumentElement().getXMLStreamReader();
            } catch (final JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
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
