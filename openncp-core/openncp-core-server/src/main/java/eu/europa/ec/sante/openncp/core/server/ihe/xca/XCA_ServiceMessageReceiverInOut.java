package eu.europa.ec.sante.openncp.core.server.ihe.xca;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.AuditServiceFactory;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcEntry;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtilWrapper;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.ServiceType;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogUtil;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.XCAServiceImpl;
import eu.europa.ec.sante.openncp.core.server.ihe.xdr.impl.XDRServiceImpl;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPMessage;
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

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import org.apache.axiom.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import java.util.*;
import org.apache.axiom.soap.SOAPFactory;
import org.w3c.dom.Element;

import java.util.Iterator;

/**
 * XCA_ServiceMessageReceiverInOut message receiver
 */
public class XCA_ServiceMessageReceiverInOut extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XCA_ServiceMessageReceiverInOut.class);
    private static final JAXBContext wsContext;

    public XCA_ServiceMessageReceiverInOut() {
        super(Phase.RECEIVE); // Or Phase.INVOKE, depending on when you need it
    }

    static {
        LOGGER.debug("Loading the WS-Security init libraries in XCA 2007");
        org.apache.xml.security.Init.init();
    }

    static {
        JAXBContext jc = null;
        try {
            jc = JAXBContext.newInstance(
                    AdhocQueryRequest.class,
                    AdhocQueryResponse.class,
                    RetrieveDocumentSetRequestType.class,
                    RetrieveDocumentSetResponseType.class);
        } catch (final JAXBException ex) {
            LOGGER.error("Unable to create JAXBContext: '{}'", ex.getMessage(), ex);
            Runtime.getRuntime().exit(-1);
        } finally {
            wsContext = jc;
        }
    }

    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");



    public void invokeBusinessLogic(final Message message, final Message newMessage) throws Fault {

        javax.xml.soap.SOAPMessage soapMessage = message.getContent(javax.xml.soap.SOAPMessage.class);
        String eadcError = "";


        // Start Date for eADC
        final Date startTime = new Date();

        // End Date for eADC
        Date endTime = new Date();

        // Out Envelop
        SOAPEnvelope envelope = null;

        final RetrieveDocumentSetResponseType retrieveDocumentSetResponseType = null;
        ServiceType serviceType = null;
        Document clinicalDocument = null;
        try {

            final Object serviceObject = getServiceObject(message);

            XCAServiceImpl xcaService = (XCAServiceImpl)getServiceObject(message);

            // Find the axisOperation that has been set by the Dispatch phase.
          /* // final AxisOperation op = msgContext.getOperationContext().getAxisOperation();

            if (op == null) {
                final String err = "Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider";

                //eadcFailure(msgContext, err, ServiceType.DOCUMENT_LIST_RESPONSE);
                eadcError = err;
                serviceType = ServiceType.DOCUMENT_LIST_RESPONSE;

                throw new AxisFault(err);
            }*/

            final String randomUUID = Constants.UUID_PREFIX + UUID.randomUUID();
            final String methodName;

            OperationInfo operationInfo = message.getExchange().get(OperationInfo.class);

            if ((operationInfo.getName() != null) && ((methodName = xmlNameToJavaIdentifier(operationInfo.getName().getLocalPart())) != null)) {

                final SOAPHeader sh = (SOAPHeader) soapMessage.getSOAPPart().getEnvelope().getHeader();
                //  Identification of the TLS Common Name of the client.
                final String clientCommonName = EventLogUtil.getClientCommonName(message);

                final EventLog eventLog = new EventLog();
                eventLog.setReqM_ParticipantObjectID(getMessageID(message));
                eventLog.setReqM_ParticipantObjectDetail(soapMessage.getSOAPPart().getEnvelope().getHeader().toString().getBytes());
                eventLog.setSC_UserID(clientCommonName);
                eventLog.setSourceip(EventLogUtil.getSourceGatewayIdentifier(message));
                eventLog.setTargetip(EventLogUtil.getTargetGatewayIdentifier());

                if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name()) && loggerClinical.isDebugEnabled()) {
                    loggerClinical.debug("Incoming XCA Request Message:\n{}", XMLUtil.prettyPrint(DOMUtils.getFirstElement(soapMessage.getSOAPPart().getEnvelope())));
                }
                if (StringUtils.equals(XCAOperation.SERVICE_CROSS_GATEWAY_QUERY, methodName)) {

                    LOGGER.info("[ITI-38] Incoming XCA List from '{}'", clientCommonName);
                    /* Validate incoming query request */
                    final String requestMessage = XMLUtil.prettyPrintForValidation(DOMUtils.getFirstElement(soapMessage.getSOAPPart().getEnvelope().getBody().getFirstChild()));

                    final AdhocQueryResponse adhocQueryResponse1;
                    final AdhocQueryRequest wrappedParam = (AdhocQueryRequest) fromOM(
                            (OMElement) soapMessage.getSOAPPart().getEnvelope().getFirstChild(), AdhocQueryRequest.class,
                            getEnvelopeNamespaces((SoapMessage) message));

                    final List<ClassCode> classCodes = extractClassCodesFromQueryRequest(wrappedParam);
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCrossCommunityAccess(requestMessage, NcpSide.NCP_A, classCodes);
                    }

                    adhocQueryResponse1 = xcaService.respondingGateway_CrossGatewayQuery(wrappedParam, sh, eventLog);
                    envelope = toEnvelope(me, adhocQueryResponse1, false);
                    eventLog.setResM_ParticipantObjectID(randomUUID);
                    eventLog.setResM_ParticipantObjectDetail(envelope.getHeader().toString().getBytes());
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("[Audit Debug] Responder: ParticipantId: '{}'\nObjectDetail: '{}'",
                                randomUUID, envelope.getHeader().toString());
                    }
                    eventLog.setNcpSide(NcpSide.NCP_A);
                    eventLog.setQueryByParameter(" ");
                    eventLog.setHciIdentifier(" ");

                    EventLogUtil.extractQueryByParamFromHeader(eventLog, message, "PRPA_IN201305UV02", "controlActProcess", "queryByParameter");
                    EventLogUtil.extractHCIIdentifierFromHeader(eventLog, message);

                    AuditServiceFactory.getInstance().write(eventLog, "", "1");


                    /* Validate outgoing query response */
                    final String responseMessage = XMLUtil.prettyPrintForValidation(DOMUtils.getFirstElement(soapMessage.getSOAPPart().getEnvelope().getFirstChild()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCrossCommunityAccess(responseMessage, NcpSide.NCP_A, classCodes);
                    }

                    if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name()) && loggerClinical.isDebugEnabled()) {
                        loggerClinical.debug("Response Header:\n{}", envelope.getHeader().toString());
                        loggerClinical.debug("Outgoing XCA Response Message:\n{}", XMLUtil.prettyPrint(DOMUtils.getFirstElement(envelope)));
                    }
                    serviceType = ServiceType.DOCUMENT_LIST_RESPONSE;

                } else if (StringUtils.equals(XCAOperation.SERVICE_CROSS_GATEWAY_RETRIEVE, methodName)) {

                    LOGGER.info("[ITI-39] Incoming XCA Retrieve from '{}'", clientCommonName);
                    /* Validate incoming retrieve request */
                    final String requestMessage = XMLUtil.prettyPrint(DOMUtils.getFirstElement(soapMessage.getSOAPPart().getEnvelope().getFirstChild()));

                    final RetrieveDocumentSetRequestType wrappedParam = (RetrieveDocumentSetRequestType) fromOM(
                            (OMElement) soapMessage.getSOAPPart().getEnvelope().getFirstChild(), RetrieveDocumentSetRequestType.class,
                            getEnvelopeNamespaces((SoapMessage) message));

                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCrossCommunityAccess(requestMessage, NcpSide.NCP_A, null);
                    }

                    final OMFactory factory = OMAbstractFactory.getOMFactory();
                    final OMNamespace ns = factory.createOMNamespace("urn:ihe:iti:xds-b:2007", "");
                    final OMElement omElement = factory.createOMElement("RetrieveDocumentSetResponse", ns);
                    skel.respondingGateway_CrossGatewayRetrieve(wrappedParam, sh, eventLog, omElement);

                    envelope = toEnvelope(getSOAPFactory(message), omElement);

                    eventLog.setResM_ParticipantObjectID(randomUUID);
                    eventLog.setResM_ParticipantObjectDetail(envelope.getHeader().toString().getBytes());
                    eventLog.setNcpSide(NcpSide.NCP_A);
                    eventLog.setQueryByParameter(" ");
                    eventLog.setHciIdentifier(" ");

                    EventLogUtil.extractQueryByParamFromHeader(eventLog, message, "PRPA_IN201305UV02", "controlActProcess", "queryByParameter");
                    EventLogUtil.extractHCIIdentifierFromHeader(eventLog, message);

                    AuditServiceFactory.getInstance().write(eventLog, "", "1");

                    if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name()) && loggerClinical.isDebugEnabled()) {
                        loggerClinical.debug("Outgoing XCA Response Message:\n{}", XMLUtil.prettyPrint(DOMUtils.getFirstElement(soapMessage.getSOAPPart().getEnvelope())));
                    }

                    /*final Options options = new Options();
                    options.setProperty(org.apache.axis2.Constants.Configuration.ENABLE_MTOM, org.apache.axis2.Constants.VALUE_TRUE);
                    newMsgContext.setOptions(options);*/

                    /* Validate outgoing retrieve response */
                    final String responseMessage = XMLUtil.prettyPrint(DOMUtils.getFirstElement(soapMessage.getSOAPPart().getEnvelope()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCrossCommunityAccess(responseMessage, NcpSide.NCP_A, null);
                    }

                    final RetrieveDocumentSetResponseType responseType = (RetrieveDocumentSetResponseType) fromOM(
                            omElement, RetrieveDocumentSetResponseType.class, null);

                    clinicalDocument = EadcUtilWrapper.getCDA(responseType);

                    serviceType = ServiceType.DOCUMENT_EXCHANGED_RESPONSE;
                } else {
                    final String err = "Method not found: '"+ methodName + "'";
                    LOGGER.error(err);

                    //eadcFailure(msgContext, err, ServiceType.DOCUMENT_EXCHANGED_RESPONSE);
                    eadcError = err;
                    serviceType = ServiceType.DOCUMENT_EXCHANGED_RESPONSE;

                    throw new RuntimeException(err);
                }

                newMsgContext.setEnvelope(envelope);
                newMsgContext.getOptions().setMessageId(randomUUID);
                endTime = new Date();

                if(!EadcUtilWrapper.hasTransactionErrors(envelope)) {
                    EadcUtilWrapper.invokeEadc(msgContext, newMsgContext, null, clinicalDocument, startTime, endTime,
                            Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC, EadcUtil.Direction.INBOUND, serviceType);
                } else {
                    eadcError = EadcUtilWrapper.getTransactionErrorDescription(envelope);
                }

            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);

            //eadcFailure(msgContext, e.getMessage(), ServiceType.DOCUMENT_LIST_RESPONSE);
            eadcError = e.getMessage();
            serviceType = ServiceType.DOCUMENT_LIST_RESPONSE;

            throw AxisFault.makeFault(e);
        } finally {
            if(!eadcError.isEmpty()) {
                EadcUtilWrapper.invokeEadcFailure(message, newMessage, null, clinicalDocument, startTime, endTime,
                        Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC, EadcUtil.Direction.INBOUND, serviceType, eadcError);
                eadcError = "";
            }
        }
    }

    private List<ClassCode> extractClassCodesFromQueryRequest(final AdhocQueryRequest wrappedParam) {
        final ArrayList<ClassCode> list = new ArrayList<>();
        if (wrappedParam != null) {
            wrappedParam.getAdhocQuery().getSlot().forEach(slot -> {
                if (StringUtils.equals(XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_CLASSCODE_SLOT_NAME, slot.getName())) {
                    if (slot.getValueList() != null && slot.getValueList().getValue().size() > 0) {
                        for (int i = 0; i < slot.getValueList().getValue().size(); i++) {
                            final String item = StringUtils.substringBetween(slot.getValueList().getValue().get(i), "('", "^^");
                            if (StringUtils.isNotBlank(item)) {
                                list.add(ClassCode.getByCode(item));
                            }
                        }
                    }
                }
            });
        }
        return list;
    }

    private  OMElement toOM(final RetrieveDocumentSetResponseType param, final boolean optimizeContent) throws JAXBException {
        try {
            // Ensure wsContext is properly initialized elsewhere in the class
            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            // Create an Axiom OMFactory instance
            final OMFactory factory = OMAbstractFactory.getOMFactory();

            // Define the XML namespace
            final String namespaceURI = "urn:ihe:iti:xds-b:2007";
            final OMNamespace namespace = factory.createOMNamespace(namespaceURI, null);

            // Create the JaxbRIDataSource for marshalling
            final JaxbRIDataSource source = new JaxbRIDataSource(
                    RetrieveDocumentSetResponseType.class,
                    param,
                    marshaller,
                    namespaceURI,
                    "RetrieveDocumentSetResponse"
            );

            // Create and return the OMElement
            return factory.createOMElement(source, "RetrieveDocumentSetResponse", namespace);

        } catch (final JAXBException ex) {
            // Handle the exception as per CXF standards, rethrow or log
            throw ex; // Or you can throw a custom exception if needed
        }
    }


    private OMElement toOM(final AdhocQueryResponse param, final boolean optimizeContent) throws AxisFault {

        try {

            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final JaxbRIDataSource source = new JaxbRIDataSource(AdhocQueryResponse.class,
                    param, marshaller, "urn:oasis:names:tc:ebxml-regrep:xsd:query:3.0", "AdhocQueryResponse");
            final OMNamespace namespace = factory.createOMNamespace("urn:oasis:names:tc:ebxml-regrep:xsd:query:3.0", null);

            return factory.createOMElement(source, "AdhocQueryResponse", namespace);

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final AdhocQueryResponse param, final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));

        return envelope;
    }


    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final OMElement param) throws SOAPException {
        if (factory == null) {
            throw new SOAPException("SOAPFactory cannot be null.");
        }
        if (param == null) {
            throw new SOAPException("OMElement parameter cannot be null.");
        }

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(param);

        return envelope;
    }


    private Object fromOM(final OMElement param, final Class type, final Map extraNamespaces) throws Fault {

        try {
            final Unmarshaller unmarshaller = wsContext.createUnmarshaller();

            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();

        } catch (final JAXBException bex) {
            throw new RuntimeException("Error in CXF Webserive",bex);
        }
    }


    @Override
    public void handleMessage(Message message) throws Fault {

    }



    private  Object getServiceObject(Message message) {

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
}
