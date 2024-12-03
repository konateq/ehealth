package eu.europa.ec.sante.openncp.core.client.ihe.xdr;

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
import eu.europa.ec.sante.openncp.core.common.constants.ihe.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.lcm._3.SubmitObjectsRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryErrorList;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcEntry;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtilWrapper;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.ServiceType;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XDRException;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogClientUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogUtil;
import eu.europa.ec.sante.openncp.core.common.util.OidUtil;
import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.*;
import org.apache.axiom.om.ds.AbstractOMDataSource;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.activation.DataHandler;
import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;

public class DocumentRecipient_ServiceStub extends Stub {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentRecipient_ServiceStub.class);
    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final JAXBContext wsContext;
    private static int counter = 0;

    static {
        LOGGER.debug("Loading the WS-Security init libraries in DocumentRecipient_ServiceStub");
        org.apache.xml.security.Init.init();
    }

    static {

        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(ProvideAndRegisterDocumentSetRequestType.class, RegistryResponseType.class);

        } catch (final JAXBException ex) {

            LOGGER.error("Unable to create JAXBContext: '{}'", ex.getMessage(), ex);
            Runtime.getRuntime().exit(-1);

        } finally {
            wsContext = jaxbContext;
        }
    }

    // hashmaps to keep the fault mapping
    private final HashMap faultExceptionNameMap = new HashMap();
    private final HashMap faultExceptionClassNameMap = new HashMap();
    private final HashMap faultMessageMap = new HashMap();
    private final QName[] opNameArray = null;
    private AxisOperation[] axisOperations;
    private String countryCode;
    private ClassCode classCode;

    /**
     * Constructor that takes in a configContext
     */
    public DocumentRecipient_ServiceStub(final ConfigurationContext configurationContext, final String targetEndpoint) throws AxisFault {
        this(configurationContext, targetEndpoint, false);
    }

    /**
     * Constructor that takes in a configContext and use separate listener
     */
    public DocumentRecipient_ServiceStub(final ConfigurationContext configurationContext, final String targetEndpoint, final boolean useSeparateListener) throws AxisFault {

        // To populate AxisService
        populateAxisService();
        populateFaults();

        _serviceClient = new ServiceClient(configurationContext, _service);
        _serviceClient.getOptions().setTo(new EndpointReference(targetEndpoint));
        _serviceClient.getOptions().setUseSeparateListener(useSeparateListener);
        //  Wait time after which a client times out in a blocking scenario: 3 minutes
        _serviceClient.getOptions().setTimeOutInMilliSeconds(180000);

        // Set the SOAP version
        _serviceClient.getOptions().setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        _serviceClient.getOptions().setProperty(org.apache.axis2.Constants.Configuration.ENABLE_MTOM, org.apache.axis2.Constants.VALUE_TRUE);

        // Enabling Axis2 - SSL 2 ways communication (not active by default).
        try {
            _serviceClient.getServiceContext().getConfigurationContext()
                    .setProperty(HTTPConstants.CACHED_HTTP_CLIENT, HttpsClientConfiguration.getDefaultSSLClient());
            _serviceClient.getServiceContext().getConfigurationContext()
                    .setProperty(HTTPConstants.REUSE_HTTP_CLIENT, false);
        } catch (final NoSuchAlgorithmException | KeyManagementException | IOException | CertificateException |
                       KeyStoreException | UnrecoverableKeyException e) {
            throw new AxisFault("SSL Context cannot be initialized");
        }
    }

    /**
     * Constructor taking the target endpoint
     */
    public DocumentRecipient_ServiceStub(final String targetEndpoint) throws AxisFault {
        this(null, targetEndpoint);
    }

    private static synchronized String getUniqueSuffix() {
        if (counter > 99999) {
            // reset the counter if it is greater than 99999
            counter = 0;
        }
        counter++;
        return System.currentTimeMillis() + "_" + counter;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    /*
     * Methods
     */

    public void setClassCode(final ClassCode classCode) {
        this.classCode = classCode;
    }

    /**
     * @param provideAndRegisterDocumentSetRequest
     * @see DocumentRecipient_ServiceStub#documentRecipient_ProvideAndRegisterDocumentSetB
     */
    public RegistryResponseType documentRecipient_ProvideAndRegisterDocumentSetB(
            final ProvideAndRegisterDocumentSetRequestType provideAndRegisterDocumentSetRequest,
            final Map<AssertionType, Assertion> assertionMap) throws RemoteException, XDRException {

        MessageContext messageContext = null;
        MessageContext returnMessageContext = null;

        Document eDispenseCda = null;

        Date transactionStartTime = new Date();
        Date transactionEndTime = new Date();

        String eadcError = "";

        try {
            var operationClient = _serviceClient.createClient(axisOperations[0].getName());
            operationClient.getOptions().setAction(XDRConstants.SOAP_HEADERS.REQUEST_ACTION);
            operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            addPropertyToOperationClient(operationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

            // create a message context
            messageContext = new MessageContext();

            // create SOAP envelope with that payload
            SOAPEnvelope soapEnvelope = toEnvelope(getFactory(operationClient.getOptions().getSoapVersionURI()),
                    provideAndRegisterDocumentSetRequest,
                    optimizeContent(new QName(XDRConstants.NAMESPACE_URI, XDRConstants.SOAP_HEADERS.NAMESPACE_REQUEST_LOCAL_PART)));

            //  Adding SOAP soap_headers
            final var omFactory = OMAbstractFactory.getOMFactory();
            final OMNamespace ns2 = omFactory.createOMNamespace(XDRConstants.SOAP_HEADERS.OM_NAMESPACE, "");

            final SOAPHeaderBlock action = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock(XDRConstants.SOAP_HEADERS.ACTION_STR, ns2);
            final OMNode node = omFactory.createOMText(XDRConstants.SOAP_HEADERS.REQUEST_ACTION);
            action.addChild(node);

            final OMAttribute omAttribute = omFactory.createOMAttribute(XDRConstants.SOAP_HEADERS.MUST_UNDERSTAND_STR, soapEnvelope.getNamespace(), "1");
            action.addAttribute(omAttribute);

            final SOAPHeaderBlock id = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock(XDRConstants.SOAP_HEADERS.MESSAGEID_STR, ns2);
            final OMNode node2 = omFactory.createOMText(Constants.UUID_PREFIX + UUID.randomUUID());
            id.addChild(node2);


            final OMNamespace ns = omFactory.createOMNamespace(XDRConstants.SOAP_HEADERS.SECURITY_XSD, "wsse");
            final SOAPHeaderBlock security = OMAbstractFactory.getSOAP12Factory().createSOAPHeaderBlock(XDRConstants.SOAP_HEADERS.SECURITY_STR, ns);

            addSecurityHeader(assertionMap, security);

            /* The WSA To header is not being manually added, it's added by the client-connector axis2.xml configurations
            (which globally engages the addressing module, adding the wsa:To header based on the endpoint value from the transport)
            based on the assumption that these IHE Service clients will always be coupled with client-connector, which may not be
            the case in the future. When that happens, we may need to revisit this code to add the To header like it's done in the IHE XCA service client.
            See issues EHNCP-1141 and EHNCP-1168. */
            _serviceClient.addHeader(action);
            _serviceClient.addHeader(id);
            _serviceClient.addHeadersToEnvelope(soapEnvelope);

            /*
             * Prepare request
             */
            messageContext.setEnvelope(soapEnvelope);   // set the message context with that soap envelope
            operationClient.addMessageContext(messageContext);    // add the message context to the operation client

            /* Log soap request */
            final String requestLogMsg = getSoapResponseRequestMsg(soapEnvelope, XDRConstants.LOG.OUTGOING_XDR_PROVIDEANDREGISTER_MESSAGE);

            /* Perform validation of request message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateXDRMessage(requestLogMsg, NcpSide.NCP_B, null);
            }
            /*
             * Execute Operation
             */
            transactionStartTime = new Date();
            final SOAPEnvelope returnEnv;
            try {
                operationClient.execute(true);
            } catch (final AxisFault e) {
                LOGGER.error("Axis Fault error: '{}'", e.getMessage());
                LOGGER.error("Trying to automatically solve the problem by fetching configurations from the Central Services...");
                String endpoint = null;

                LOGGER.debug("ClassCode: '{}'", this.classCode);
                final DynamicDiscoveryService dynamicDiscoveryService = new DynamicDiscoveryService();
                switch (classCode) {
                    case ED_CLASSCODE:
                        endpoint = dynamicDiscoveryService.getEndpointUrl(
                                this.countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.DISPENSATION_SERVICE, true);
                        break;
                    case CONSENT_CLASSCODE:
                        endpoint = dynamicDiscoveryService.getEndpointUrl(
                                this.countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.CONSENT_SERVICE, true);
                        break;
                    default:
                        break;
                }

                if (StringUtils.isNotEmpty(endpoint)) {

                    /* if we get something from the Central Services, then we retry the request */
                    /* correctly sets the Transport information with the new endpoint */
                    LOGGER.debug("Retrying the request with the new configurations: [{}]", endpoint);
                    _serviceClient.getOptions().setTo(new EndpointReference(endpoint));

                    /* we need a new OperationClient, otherwise we'll face the error "A message was added that is not valid. However, the operation context was complete." */
                    final OperationClient newOperationClient = _serviceClient.createClient(axisOperations[0].getName());
                    newOperationClient.getOptions().setAction(XDRConstants.SOAP_HEADERS.REQUEST_ACTION);
                    newOperationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
                    addPropertyToOperationClient(newOperationClient, WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");

                    final SOAPFactory newSoapFactory = getFactory(newOperationClient.getOptions().getSoapVersionURI());

                    /* we need to create a new SOAP payload so that the wsa:To header is correctly set
                    (i.e., copied from the Transport information to the wsa:To during the running of the Addressing Phase,
                    as defined by the global engagement of the addressing module in axis2.xml). The old payload still contains the old endpoint. */
                    final SOAPEnvelope newEnv = toEnvelope(newSoapFactory, provideAndRegisterDocumentSetRequest,
                            optimizeContent(new QName(XCAConstants.SOAP_HEADERS.NAMESPACE_URI, XCAConstants.SOAP_HEADERS.RETRIEVE.NAMESPACE_REQUEST_LOCAL_PART)));

                    /* we set the previous headers in the new SOAP envelope. Note: the wsa:To header is not manually set (only Action and MessageID are) but instead handled by the
                    axis2 configuration of client-connector (my assumption). This may have impact if we decouple client-connector from the IHE service clients. If
                    they are decoupled, we most probably have to add the To header manually like it's done in the IHE XCA client, both here and in the initial
                    request. See issues EHNCP-1141 and EHNCP-1168. */
                    _serviceClient.addHeadersToEnvelope(newEnv);

                    /* we create a new Message Context with the new SOAP envelope */
                    final var newMessageContext = new MessageContext();
                    newMessageContext.setEnvelope(newEnv);

                    /* add the new message context to the new operation client */
                    newOperationClient.addMessageContext(newMessageContext);
                    /* we retry the request */
                    newOperationClient.execute(true);
                    /* we need to reset the previous variables with the new content, to be used later */
                    operationClient = newOperationClient;
                    messageContext = newMessageContext;
                    soapEnvelope = newEnv;
                    LOGGER.debug("Successfully retried the request! Proceeding with the normal workflow...");
                } else {
                    /* if we cannot solve this issue through the Central Services, then there's nothing we can do, so we let it be thrown */
                    eadcError = "Could not find configurations in the Central Services for [" + endpoint + "], the service will fail.";
                    LOGGER.error(eadcError);
                    throw new XDRException(OpenNCPErrorCode.ERROR_GENERIC_CONNECTION_NOT_POSSIBLE, e);
                }
            }
            returnMessageContext = operationClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            returnEnv = returnMessageContext.getEnvelope();
            transactionEndTime = new Date();

            // Invoke eADC service.
            if (!provideAndRegisterDocumentSetRequest.getDocument().isEmpty()
                    && ArrayUtils.isNotEmpty(provideAndRegisterDocumentSetRequest.getDocument().get(0).getValue())) {

                eDispenseCda = EadcUtilWrapper.toXmlDocument(provideAndRegisterDocumentSetRequest.getDocument().get(0).getValue());
            }
            if (!EadcUtilWrapper.hasTransactionErrors(returnEnv)) {
                EadcUtilWrapper.invokeEadc(messageContext, returnMessageContext, this._getServiceClient(), eDispenseCda,
                        transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.EADC,
                        EadcUtil.Direction.OUTBOUND, ServiceType.DISPENSATION_QUERY);
            } else {
                eadcError = EadcUtilWrapper.getTransactionErrorDescription(returnEnv);
            }
            //  Log SOAP response message.
            final String responseLogMsg = getSoapResponseRequestMsg(returnEnv, XDRConstants.LOG.INCOMING_XDR_PROVIDEANDREGISTER_MESSAGE);

            /* Perform validation of response message */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateXDRMessage(responseLogMsg, NcpSide.NCP_B, null);
            }
            /*
             * Return
             */
            final Object object = fromOM(returnEnv.getBody().getFirstElement(), RegistryResponseType.class);

            final RegistryResponseType registryResponse = (RegistryResponseType) object;

            final EventLog eventLog = createAndSendEventLogConsent(provideAndRegisterDocumentSetRequest, registryResponse.getRegistryErrorList(),
                    messageContext, returnEnv, assertionMap.get(AssertionType.HCP), assertionMap.get(AssertionType.TRC),
                    this._getServiceClient().getOptions().getTo().getAddress());

            return registryResponse;

        } catch (final AxisFault axisFault) {
            // Audit log on exception

            final OMElement faultElt = axisFault.getDetail();
            if (faultElt != null && faultExceptionNameMap.containsKey(faultElt.getQName())) {
                // make the fault by reflection
                try {
                    final String exceptionClassName = (String) faultExceptionClassNameMap.get(faultElt.getQName());
                    final Class exceptionClass = Class.forName(exceptionClassName);
                    final Exception ex = (Exception) exceptionClass.getDeclaredConstructor().newInstance();
                    // message class
                    final String messageClassName = (String) faultMessageMap.get(faultElt.getQName());
                    final Class messageClass = Class.forName(messageClassName);
                    final Object messageObject = fromOM(faultElt, messageClass);
                    final Method method = exceptionClass.getMethod("setFaultMessage", messageClass);
                    method.invoke(ex, messageObject);

                    throw new RemoteException(ex.getMessage(), ex);

                } catch (final Exception e) {
                    // Class cannot be instantiated - throwing the original Axis fault
                    eadcError = e.getMessage();
                    throw new XDRException(OpenNCPErrorCode.ERROR_GENERIC_CONNECTION_NOT_POSSIBLE, e);
                }
            }
            eadcError = axisFault.getMessage();
            throw new XDRException(OpenNCPErrorCode.ERROR_GENERIC_CONNECTION_NOT_POSSIBLE, axisFault);
        } finally {
            if (messageContext != null && messageContext.getTransportOut() != null && messageContext.getTransportOut().getSender() != null) {
                messageContext.getTransportOut().getSender().cleanup(messageContext);
            }
            if (!eadcError.isEmpty()) {
                EadcUtilWrapper.invokeEadcFailure(messageContext, returnMessageContext, this._getServiceClient(), eDispenseCda,
                        transactionStartTime, transactionEndTime, this.countryCode, EadcEntry.DsTypes.EADC,
                        EadcUtil.Direction.OUTBOUND, ServiceType.DISPENSATION_QUERY, eadcError);
            }
        }
    }

    private static String getSoapResponseRequestMsg(final SOAPEnvelope soapEnvelope, final String type) throws XDRException {
        final String msg;
        try {
            if (LOGGER_CLINICAL.isDebugEnabled()
                    && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                final String logResponseMsg = XMLUtil.prettyPrint(XMLUtils.toDOM(soapEnvelope));
                LOGGER_CLINICAL.debug("{} {} '{}'", type,
                        System.getProperty("line.separator"), logResponseMsg);
            }
            msg = XMLUtil.prettyPrint(XMLUtils.toDOM(soapEnvelope.getBody()));
        } catch (final Exception ex) {
            throw new XDRException(OpenNCPErrorCode.ERROR_GENERIC, ex);
        }

        return msg;

    }

    private void addSecurityHeader(final Map<AssertionType, Assertion> assertionMap, final SOAPHeaderBlock security) {
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
    }

    /**
     * get the default envelope
     */
    private SOAPEnvelope toEnvelope(final SOAPFactory factory) {

        return factory.getDefaultEnvelope();
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

    private OMElement toOM(final SubmitObjectsRequest param) throws AxisFault {

        try {
            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final JaxbRIDataSource source;
            source = new JaxbRIDataSource(SubmitObjectsRequest.class, param, marshaller, XDRConstants.REGREP_LCM, "SubmitObjectsRequest");
            final OMNamespace namespace = factory.createOMNamespace(XDRConstants.REGREP_LCM, null);
            return factory.createOMElement(source, "SubmitObjectsRequest", namespace);

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final ProvideAndRegisterDocumentSetRequestType param,
                                    final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        final OMNamespace namespace = factory.createOMNamespace(XDRConstants.NAMESPACE_URI, "xdr");

        final OMElement provideAndRegisterDoc = factory.createOMElement(XDRConstants.PROVIDE_AND_REGISTER_DOCUMENT_SET_REQ_STR, namespace);
        final OMElement submitObjectsRequest = toOM(param.getSubmitObjectsRequest());
        provideAndRegisterDoc.addChild(submitObjectsRequest);
        envelope.getBody().addChild(provideAndRegisterDoc);

        final List<ProvideAndRegisterDocumentSetRequestType.Document> documents = param.getDocument();
        for (final ProvideAndRegisterDocumentSetRequestType.Document document : documents) {
            final OMElement documentElement = factory.createOMElement("Document", namespace);
            provideAndRegisterDoc.addChild(submitObjectsRequest);

            final ByteArrayDataSource rawData = new ByteArrayDataSource(document.getValue());
            final DataHandler dH = new DataHandler(rawData);
            final OMText textData = factory.createOMText(dH, true);
            textData.setOptimize(true);
            textData.setContentID(document.getId());
            final String contentID = textData.getContentID();

            final OMAttribute att = factory.createOMAttribute("id", null, contentID);
            documentElement.addAttribute(att);

            documentElement.addChild(textData);
            provideAndRegisterDoc.addChild(documentElement);
        }

        return envelope;
    }

    private Object fromOM(final OMElement param, final Class type) throws AxisFault {

        try {

            final Unmarshaller unmarshaller = wsContext.createUnmarshaller();

            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    // A.R. eDispensation handling
    private EventLog createAndSendEventLogConsent(final ProvideAndRegisterDocumentSetRequestType request, final RegistryErrorList rel,
                                                  final MessageContext msgContext, final SOAPEnvelope returnEnv,
                                                  final Assertion idAssertion, final Assertion trcAssertion, final String address) {

        final String dstHomeCommunityId = OidUtil.getHomeCommunityId(countryCode.toLowerCase(Locale.ENGLISH));
        final EventLog eventLog = EventLogClientUtil.prepareEventLog(msgContext, returnEnv, address, dstHomeCommunityId);
        EventLogClientUtil.logIdAssertion(eventLog, idAssertion);
        EventLogClientUtil.logTrcAssertion(eventLog, trcAssertion);
        EventLogUtil.prepareXDRCommonLog(eventLog, request, rel);
        eventLog.setNcpSide(NcpSide.NCP_B);
        EventLogClientUtil.sendEventLog(eventLog);
        return eventLog;
    }

    private void populateAxisService() {

        // creating the Service with a unique name
        _service = new AxisService(XDRConstants.DOCUMENT_RECIPIENT_SERVICE_STR + getUniqueSuffix());
        addAnonymousOperations();

        // creating the operations
        final AxisOperation operation = new OutInAxisOperation();
        operation.setName(new QName(XDRConstants.NAMESPACE_URI, XDRConstants.PROVIDE_AND_REGISTER_DOCUMENT_SET_REQ_STR));

        axisOperations = new AxisOperation[1];
        _service.addOperation(operation);
        axisOperations[0] = operation;
    }

    // populates the faults
    private void populateFaults() {
        // populates the faults
    }

    static class JaxbRIDataSource extends AbstractOMDataSource {

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

        /**
         * @param output
         * @throws XMLStreamException
         */
        public void serialize(final OutputStream output) throws XMLStreamException {

            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), output);
            } catch (final JAXBException e) {
                throw new XMLStreamException(XDRConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        /**
         * @param writer
         * @throws XMLStreamException
         */
        public void serialize(final Writer writer) throws XMLStreamException {

            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), writer);
            } catch (final JAXBException e) {
                throw new XMLStreamException(XDRConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        /**
         * @param xmlWriter
         * @throws XMLStreamException
         */
        public void serialize(final XMLStreamWriter xmlWriter) throws XMLStreamException {

            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), xmlWriter);

            } catch (final JAXBException e) {
                throw new XMLStreamException(XDRConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
            }
        }

        /**
         * @return
         * @throws XMLStreamException
         */
        public XMLStreamReader getReader() throws XMLStreamException {

            try {

                final OMDocument omDocument = OMAbstractFactory.getOMFactory().createOMDocument();
                final Marshaller marshaller = wsContext.createMarshaller();
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), omDocument.getSAXResult());

                return omDocument.getOMDocumentElement().getXMLStreamReader();

            } catch (final JAXBException e) {
                throw new XMLStreamException(XDRConstants.EXCEPTIONS.ERROR_JAXB_MARSHALLING, e);
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
