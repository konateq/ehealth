package eu.europa.ec.sante.openncp.core.server.ihe.xdr;


import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.AuditService;
import eu.europa.ec.sante.openncp.common.audit.AuditServiceFactory;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcEntry;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtilWrapper;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.ServiceType;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogUtil;
import eu.europa.ec.sante.openncp.core.server.ihe.xdr.impl.XDRServiceImpl;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.OperationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.soap.*;

import org.apache.axiom.soap.SOAPFactory;

import java.util.*;

/**
 * XDR_ServiceMessageReceiverInOut message receiver
 */
public class XDR_ServiceMessageReceiverInOut extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XDR_ServiceMessageReceiverInOut.class);
    private static final JAXBContext wsContext;

    public XDR_ServiceMessageReceiverInOut() {
        super(Phase.RECEIVE); // Or Phase.INVOKE, depending on when you need it
    }

    static {

        LOGGER.debug("[XDR Services] Loading the WS-Security init libraries in XDR 2007");
        org.apache.xml.security.Init.init();
    }

    static {

        JAXBContext jaxbContext = null;

        try {
            jaxbContext = JAXBContext.newInstance(ProvideAndRegisterDocumentSetRequestType.class,
                    RegistryResponseType.class);
        } catch (final JAXBException ex) {
            LOGGER.error("Unable to create JAXBContext: '{}'", ex.getMessage(), ex);
            Runtime.getRuntime().exit(-1);
        } finally {
            wsContext = jaxbContext;
        }
    }

    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

   /* private String getMessageID(final SOAPEnvelope envelope) {

        final Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(new QName(AddressingConstants.Final.WSA_NAMESPACE,
                AddressingConstants.WSA_MESSAGE_ID));
        if (it.hasNext()) {
            return it.next().getText();
        } else {
            return Constants.UUID_PREFIX;
        }
    }*/

    private static String getMessageID(final Message message) {

        if (!(message instanceof SoapMessage)) {
            LOGGER.warn("Message is not a SOAP message. Cannot extract MessageID.");
            return Constants.UUID_PREFIX; // Or throw an exception if appropriate
        }

        SoapMessage soapMessage = (SoapMessage) message;
        List<Header> headers = soapMessage.getHeaders();

        if (headers != null) {
            for (Header header : headers) {
                if (header.getObject() instanceof org.w3c.dom.Element) {
                    org.w3c.dom.Element headerElement = (Element) header.getObject();

                    // More robust approach using XPath (recommended):
                    try {
                        javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
                        javax.xml.xpath.XPath xpath = xpathFactory.newXPath();
                        String expression = "//wsa:MessageID"; // wsa prefix for WS-Addressing namespace
                        javax.xml.xpath.XPathExpression compiledExpression = xpath.compile(expression);

                        String messageID = compiledExpression.evaluate(headerElement);
                        if (messageID != null && !messageID.isEmpty()) {
                            return messageID;
                        }

                    } catch (javax.xml.xpath.XPathExpressionException e) {
                        LOGGER.error("Error evaluating XPath expression: " + e.getMessage());
                        // Fallback to DOM traversal (less robust):
                        QName messageIDQName = new QName("http://www.w3.org/2005/08/addressing", "MessageID");
                        org.w3c.dom.NodeList nodeList = headerElement.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "MessageID");
                        if (nodeList.getLength() > 0) {
                            return nodeList.item(0).getTextContent();
                        }
                    }

                }
            }
        }

        return Constants.UUID_PREFIX; // MessageID not found.
    }

    /**
     * Axis2 method invoking web service and business logic related to XDR IHE Profile.
     *
     * @param message     - SOAP MessageContext request.
     * @param newMessage- SOAP MessageContext response.
     * @throws Fault - Exception returned during the process.
     */
    public void invokeBusinessLogic(final Message message, final Message newMessage) throws Fault {

        SOAPMessage soapMessage = message.getContent(SOAPMessage.class);

        String eadcError = "";

        final Date startTime = new Date();
        Date endTime = new Date();

        Document eDispenseCda = null;

        // Out Envelop
        final SOAPEnvelope envelope;

        try {
            // get the implementation class for the Web Service
            final Object serviceObject = getServiceObject(message);

            XDRServiceImpl xdrService = (XDRServiceImpl) getServiceObject(message); // Using the method from above


       /*     // Find the axisOperation that has been set by the Dispatch phase.
            final AxisOperation axisOperation = msgContext.getOperationContext().getAxisOperation();

            if (axisOperation == null) {
                final String err = "Operation is not located, if this is Doc/lit style the SOAP-ACTION should specified via the " +
                        "SOAP Action to use the RawXMLProvider";

                eadcError = err;

                throw new AxisFault(err);
            }*/

            final String randomUUID = Constants.UUID_PREFIX + UUID.randomUUID();
            final String methodName;

            OperationInfo operationInfo = message.getExchange().get(OperationInfo.class);


            if ((operationInfo.getName() != null) && ((methodName = xmlNameToJavaIdentifier(operationInfo.getName().getLocalPart())) != null)) {

                final SOAPHeader soapHeader = soapMessage.getSOAPHeader();

                //  Identification of the TLS Common Name of the client.
                final String clientCommonName = EventLogUtil.getClientCommonName(message);
                LOGGER.info("[ITI-41] Incoming XDR Request from '{}'", clientCommonName);

                final EventLog eventLog = new EventLog();
                eventLog.setReqM_ParticipantObjectID(getMessageID(message));
                eventLog.setReqM_ParticipantObjectDetail(soapMessage.getSOAPPart().getEnvelope().getHeader().toString().getBytes());
                eventLog.setSC_UserID(clientCommonName);
                eventLog.setSourceip(EventLogUtil.getSourceGatewayIdentifier(message));
                eventLog.setTargetip(EventLogUtil.getTargetGatewayIdentifier());

                if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                    loggerClinical.debug("Incoming XDR Request Message:\n{}", XMLUtil.prettyPrint(DOMUtils.getFirstElement(soapMessage.getSOAPPart().getEnvelope())));
                }

                if (StringUtils.equals("documentRecipient_ProvideAndRegisterDocumentSetB", methodName)) {

                    /* Validate incoming request */
                    final String requestMessage = XMLUtil.prettyPrint(DOMUtils.getFirstElement(soapMessage.getSOAPPart().getEnvelope().getBody().getFirstChild()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateXDRMessage(requestMessage, NcpSide.NCP_A, null);
                    }
                    final ProvideAndRegisterDocumentSetRequestType wrappedParam = (ProvideAndRegisterDocumentSetRequestType) fromOM(
                            (OMElement) soapMessage.getSOAPPart().getEnvelope().getBody().getFirstChild(),
                            ProvideAndRegisterDocumentSetRequestType.class,
                            getEnvelopeNamespaces((SoapMessage) message));

                    eventLog.setNcpSide(NcpSide.NCP_A);
                    final RegistryResponseType registryResponse = xdrService.saveDocument(wrappedParam, eventLog);

                    envelope = soapMessage.getSOAPPart().getEnvelope();

                    eventLog.setResM_ParticipantObjectID(randomUUID);
                    eventLog.setResM_ParticipantObjectDetail(envelope.getHeader().toString().getBytes());
                    eventLog.setQueryByParameter(" ");
                    eventLog.setHciIdentifier(" ");

                    EventLogUtil.extractQueryByParamFromHeader(eventLog, message, "PRPA_IN201305UV02", "controlActProcess", "queryByParameter");
                    EventLogUtil.extractHCIIdentifierFromHeader(eventLog, message);

                    final AuditService auditService = AuditServiceFactory.getInstance();
                    auditService.write(eventLog, "", "1");

                    /* Validate outgoing response */
                    final String responseMessage = XMLUtil.prettyPrint(DOMUtils.getFirstElement(envelope.getBody().getFirstChild()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateXDRMessage(responseMessage, NcpSide.NCP_A, null);
                    }
                    if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                        loggerClinical.debug("Response Header:\n{}", envelope.getHeader());
                        loggerClinical.debug("Outgoing XDR Response Message:\n{}", XMLUtil.prettyPrint(DOMUtils.getFirstElement(envelope)));
                    }
                    // eADC: extract of the eDispense CDA required by the KPIs.
                    eDispenseCda = EadcUtilWrapper.toXmlDocument(wrappedParam.getDocument().get(0).getValue());

                } else {
                    final String err = "Method not found: '" + methodName + "'";
                    LOGGER.error(err);

                    eadcError = err;

                    throw new RuntimeException(err);
                }

                endTime = new Date();
                // newMessage.setEnvelope(envelope);
                // newMsgContext.getOptions().setMessageId(randomUUID);

                if (!EadcUtilWrapper.hasTransactionErrors(message)) {
                    EadcUtilWrapper.invokeEadc(message, newMessage, null, eDispenseCda, startTime,
                            endTime, Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC, EadcUtil.Direction.INBOUND,
                            ServiceType.DISPENSATION_RESPONSE);
                } else {
                    eadcError = EadcUtilWrapper.getTransactionErrorDescription(message);
                }
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);

            eadcError = e.getMessage();

            throw new RuntimeException("CXF ", e);
        } finally {
            if (!eadcError.isEmpty()) {
                EadcUtilWrapper.invokeEadcFailure(message, newMessage, null, eDispenseCda, startTime, endTime,
                        Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC, EadcUtil.Direction.INBOUND,
                        ServiceType.DISPENSATION_RESPONSE, eadcError);
            }
        }

    }

    private static String xmlNameToJavaIdentifier(String xmlName) {
        if (xmlName == null || xmlName.isEmpty()) {
            return null;
        }

        // Replace invalid characters (anything except a-z, A-Z, 0-9, and underscores)
        String javaIdentifier = xmlName.replaceAll("[^a-zA-Z0-9_]", "_");

        // Ensure the identifier does not start with a digit
        if (Character.isDigit(javaIdentifier.charAt(0))) {
            javaIdentifier = "_" + javaIdentifier;
        }

        return javaIdentifier;
    }

    private OMElement toOM(final RegistryResponseType param) throws Fault {

        try {
            final OMFactory factory = OMAbstractFactory.getOMFactory();
            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            final JaxbRIDataSource source = new JaxbRIDataSource(RegistryResponseType.class, param, marshaller,
                    "urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", "RegistryResponse");
            final OMNamespace namespace = factory.createOMNamespace("urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", null);

            return factory.createOMElement(source, "RegistryResponse", namespace);

        } catch (final JAXBException bex) {
            throw new RuntimeException("CXF Error", bex);
        }
    }

    private SOAPMessage toEnvelope(final RegistryResponseType param) throws SOAPException, JAXBException {
        // Create a SOAP Message
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPBody soapBody = soapMessage.getSOAPBody();

        // Marshal RegistryResponseType to SOAPBody
        JAXBContext jaxbContext = JAXBContext.newInstance(RegistryResponseType.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(param, soapBody);

        return soapMessage;
    }

    /**
     * Returns default SOAP envelope.
     */
    // Method to create a default SOAP Envelope
    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(final SOAPFactory factory) {
        // Simply return the default envelope from the factory
        return factory.getDefaultEnvelope();
    }

    // Method to convert OMElement to Java object
    private Object fromOM(final OMElement param, final Class<?> type, final Map<String, String> extraNamespaces) throws Fault {
        try {
            // Create the unmarshaller from the wsContext
            final Unmarshaller unmarshaller = wsContext.createUnmarshaller();

            // Unmarshal the OMElement to the specified class type and return the value
            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();
        } catch (final JAXBException bex) {
            // Throw a specific AxisFault with the exception details
            throw new RuntimeException("Error creating SOAP Fault", bex);
        }
    }


    /**
     * A utility method that copies the namespaces from the SOAPEnvelope.
     */
    private Map<String, String> getEnvelopeNamespaces(final SoapMessage message) {
        // Create a map to store the namespaces
        final Map<String, String> returnMap = new HashMap<>();

        // Get the SOAP Body from the SoapMessage (using getContent(OMElement.class))
        OMElement element = message.getContent(OMElement.class);

        // Get the namespaces in scope for this OMElement
        final Iterator<OMNamespace> namespaceIterator = element.getNamespacesInScope();

        // Iterate over the namespaces and add them to the map
        while (namespaceIterator.hasNext()) {
            final OMNamespace ns = namespaceIterator.next();
            if (ns != null) {
                returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
            }
        }

        return returnMap;
    }


    @Override
    public void handleMessage(Message message) throws Fault {
        invokeBusinessLogic(message, message);
    }

    private Object getServiceObject(Message message) {

        try {
            if (message == null) {
                throw new RuntimeException("No CXF message found."); // Handle appropriately
            }

            Exchange exchange = message.getExchange();
            if (exchange == null) {
                throw new RuntimeException("No CXF exchange found."); // Handle appropriately
            }

            // 2. Get the Implementation Object
            Object serviceObject = exchange.getService();

            if (serviceObject == null) {
                throw new RuntimeException("Service object not found in exchange.");
            }
            //3. Cast to Service Impl
            XDRServiceImpl xdrServiceSkeleton = (XDRServiceImpl) serviceObject;

            return xdrServiceSkeleton;

        } catch (Exception e) {
            // Handle exceptions appropriately (log, throw custom exception, etc.)
            throw new RuntimeException("Error getting service object: " + e.getMessage(), e); // Example
        }
    }
}
