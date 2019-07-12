package _2007.xds_b.iti.ihe;

import com.spirit.epsos.cc.adc.EadcEntry;
import epsos.ccd.gnomon.auditmanager.AuditService;
import epsos.ccd.gnomon.auditmanager.EventLog;
import eu.epsos.pt.eadc.EadcUtilWrapper;
import eu.epsos.pt.eadc.util.EadcUtil;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.eadc.ServiceType;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditServiceFactory;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.SAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.receivers.AbstractInOutMessageReceiver;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.XMLUtil;
import tr.com.srdc.epsos.util.http.HTTPUtil;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * XDR_ServiceMessageReceiverInOut message receiver
 */
public class XDR_ServiceMessageReceiverInOut extends AbstractInOutMessageReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(XDR_ServiceMessageReceiverInOut.class);
    private static final javax.xml.bind.JAXBContext wsContext;

    static {

        LOGGER.debug("Loading the WS-Security init libraries in XDR 2007");
        org.apache.xml.security.Init.init();
    }

    static {

        JAXBContext jc = null;

        try {
            jc = JAXBContext.newInstance(ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.class, oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType.class);
        } catch (JAXBException ex) {
            LOGGER.error("Unable to create JAXBContext: '{}'", ex.getMessage(), ex);
            Runtime.getRuntime().exit(-1);
        } finally {
            wsContext = jc;
        }
    }

    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    private String getIPofSender(MessageContext messageContext) {

        return (String) messageContext.getProperty(MessageContext.REMOTE_ADDR);
    }

    private String getMessageID(org.apache.axiom.soap.SOAPEnvelope envelope) {
        Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getText();
        } else {
            return Constants.UUID_PREFIX;
        }
    }

    public void invokeBusinessLogic(MessageContext msgContext, MessageContext newMsgContext) throws AxisFault {

        try {
            Date startTime = new Date();
            Document cda = null;
            // get the implementation class for the Web Service
            Object obj = getTheImplementationObject(msgContext);

            XDR_ServiceSkeleton skel = (XDR_ServiceSkeleton) obj;
            // Out Envelop
            SOAPEnvelope envelope;
            // Find the axisOperation that has been set by the Dispatch phase.
            AxisOperation op = msgContext.getOperationContext().getAxisOperation();

            if (op == null) {
                throw new AxisFault(
                        "Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
            }

            String randomUUID = Constants.UUID_PREFIX + UUID.randomUUID().toString();
            String methodName;

            if ((op.getName() != null) && ((methodName = JavaUtils.xmlNameToJavaIdentifier(op.getName().getLocalPart())) != null)) {

                SOAPHeader sh = msgContext.getEnvelope().getHeader();

                EventLog eventLog = new EventLog();
                String ip = getIPofSender(msgContext);
                eventLog.setSourceip(ip);
                eventLog.setReqM_ParticipantObjectID(getMessageID(msgContext.getEnvelope()));
                eventLog.setReqM_PatricipantObjectDetail(msgContext.getEnvelope().getHeader().toString().getBytes());

                HttpServletRequest req = (HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
                String clientDN = HTTPUtil.getClientCertificate(req);
                eventLog.setSC_UserID(clientDN);
                eventLog.setTargetip(HTTPUtil.getHostIpAddress(req.getServerName()));

                if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                    loggerClinical.debug("Incoming XDR Request Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(msgContext.getEnvelope())));
                }

                if (StringUtils.equals("documentRecipient_ProvideAndRegisterDocumentSetB", methodName)) {

                    /* Validate incoming request */
                    String requestMessage = XMLUtil.prettyPrint(XMLUtils.toDOM(msgContext.getEnvelope().getBody().getFirstElement()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateXDRMessage(requestMessage, NcpSide.NCP_A);
                    }

                    ProvideAndRegisterDocumentSetRequestType wrappedParam = (ProvideAndRegisterDocumentSetRequestType) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(), ProvideAndRegisterDocumentSetRequestType.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    if (!wrappedParam.getDocument().isEmpty() && ArrayUtils.isNotEmpty(wrappedParam.getDocument().get(0).getValue())) {
                        cda = EadcUtilWrapper.convertToDomDocument(wrappedParam.getDocument().get(0).getValue());
                    }
                    RegistryResponseType registryResponse = skel.documentRecipient_ProvideAndRegisterDocumentSetB(wrappedParam, sh, eventLog);

                    envelope = toEnvelope(getSOAPFactory(msgContext), registryResponse, false);

                    eventLog.setResM_ParticipantObjectID(randomUUID);
                    eventLog.setResM_PatricipantObjectDetail(envelope.getHeader().toString().getBytes());
                    eventLog.setNcpSide(NcpSide.NCP_A);

                    AuditService auditService = AuditServiceFactory.getInstance();
                    auditService.write(eventLog, "", "1");

                    /* Validate outgoing response */
                    String responseMessage = XMLUtil.prettyPrint(XMLUtils.toDOM(envelope.getBody().getFirstElement()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateXDRMessage(responseMessage, NcpSide.NCP_A);
                    }
                    if (!org.apache.commons.lang3.StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                        loggerClinical.debug("Response Header:\n{}", envelope.getHeader().toString());
                        loggerClinical.debug("Outgoing XDR Response Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(envelope)));
                    }

                } else {
                    LOGGER.error("Method not found: '{}'", methodName);
                    throw new RuntimeException("method not found");
                }

                Date endTime = new Date();
                newMsgContext.setEnvelope(envelope);
                newMsgContext.getOptions().setMessageId(randomUUID);

                //TODO: Review EADC specification for INBOUND/OUTBOUND [EHNCP-829]
                try {
                    EadcUtilWrapper.invokeEadc(msgContext, newMsgContext, null, cda, startTime,
                            endTime, Constants.COUNTRY_CODE, EadcEntry.DsTypes.XDR, EadcUtil.Direction.INBOUND, ServiceType.DOCUMENT_EXCHANGED_RESPONSE);
                } catch (Exception e) {
                    LOGGER.error("EADC INVOCATION FAILED: '{}'", e.getMessage(), e);
                }
            }
        } catch (java.lang.Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private org.apache.axiom.om.OMElement toOM(ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType param,
                                               boolean optimizeContent) throws AxisFault {
        try {

            Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            OMFactory factory = org.apache.axiom.om.OMAbstractFactory.getOMFactory();

            JaxbRIDataSource source = new JaxbRIDataSource(ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.class,
                    param, marshaller, "urn:ihe:iti:xds-b:2007", "ProvideAndRegisterDocumentSetRequest");
            OMNamespace namespace = factory.createOMNamespace("urn:ihe:iti:xds-b:2007", null);

            return factory.createOMElement(source, "ProvideAndRegisterDocumentSetRequest", namespace);

        } catch (javax.xml.bind.JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType param,
                                                          boolean optimizeContent) throws org.apache.axis2.AxisFault {

        org.apache.axiom.soap.SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));

        return envelope;
    }

    private org.apache.axiom.om.OMElement toOM(oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType param,
                                               boolean optimizeContent) throws AxisFault {

        try {

            Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            OMFactory factory = org.apache.axiom.om.OMAbstractFactory.getOMFactory();

            JaxbRIDataSource source = new JaxbRIDataSource(oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType.class,
                    param, marshaller, "urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", "RegistryResponse");
            OMNamespace namespace = factory.createOMNamespace("urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", null);

            return factory.createOMElement(source, "RegistryResponse", namespace);

        } catch (JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(SOAPFactory factory, oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType param,
                                    boolean optimizeContent) throws AxisFault {

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

            Unmarshaller unmarshaller = wsContext.createUnmarshaller();

            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();

        } catch (JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    /**
     * A utility method that copies the namepaces from the SOAPEnvelope
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

    private AxisFault createAxisFault(Exception e) {

        AxisFault f;
        Throwable cause = e.getCause();
        if (cause != null) {
            f = new AxisFault(e.getMessage(), cause);
        } else {
            f = new AxisFault(e.getMessage());
        }

        return f;
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

                SAXOMBuilder builder = new SAXOMBuilder();
                Marshaller marshaller = wsContext.createMarshaller();
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), builder);

                return builder.getRootElement().getXMLStreamReader();

            } catch (JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }
    }
}
