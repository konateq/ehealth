package _2007.xds_b.iti.ihe;

import com.spirit.epsos.cc.adc.EadcEntry;
import ee.affecto.epsos.util.EventLogUtil;
import epsos.ccd.gnomon.auditmanager.EventLog;
import eu.epsos.pt.eadc.EadcUtilWrapper;
import eu.epsos.pt.eadc.util.EadcUtil;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.eadc.ServiceType;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditService;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditServiceFactory;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.axiom.om.*;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.receivers.AbstractInOutMessageReceiver;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;

/**
 * XDR_ServiceMessageReceiverInOut message receiver
 */
public class XDR_ServiceMessageReceiverInOut extends AbstractInOutMessageReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(XDR_ServiceMessageReceiverInOut.class);
    private static final JAXBContext wsContext;

    static {

        LOGGER.debug("[XDR Services] Loading the WS-Security init libraries in XDR 2007");
        org.apache.xml.security.Init.init();
    }

    static {

        JAXBContext jaxbContext = null;

        try {
            jaxbContext = JAXBContext.newInstance(ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.class,
                    oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType.class);
        } catch (final JAXBException ex) {
            LOGGER.error("Unable to create JAXBContext: '{}'", ex.getMessage(), ex);
            Runtime.getRuntime().exit(-1);
        } finally {
            wsContext = jaxbContext;
        }
    }

    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    private String getMessageID(final SOAPEnvelope envelope) {

        final Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(new QName(AddressingConstants.Final.WSA_NAMESPACE,
                AddressingConstants.WSA_MESSAGE_ID));
        if (it.hasNext()) {
            return it.next().getText();
        } else {
            return Constants.UUID_PREFIX;
        }
    }

    /**
     * Axis2 method invoking web service and business logic related to XDR IHE Profile.
     *
     * @param msgContext     - SOAP MessageContext request.
     * @param newMsgContext- SOAP MessageContext response.
     * @throws AxisFault - Exception returned during the process.
     */
    public void invokeBusinessLogic(final MessageContext msgContext, final MessageContext newMsgContext) throws AxisFault {

        String eadcError = "";

        final Date startTime = new Date();
        Date endTime = new Date();

        Document eDispenseCda = null;

        // Out Envelop
        SOAPEnvelope envelope = null;

        try {
            // get the implementation class for the Web Service
            final Object serviceObject = getTheImplementationObject(msgContext);
            final XDR_ServiceSkeleton xdrServiceSkeleton = (XDR_ServiceSkeleton) serviceObject;

            // Find the axisOperation that has been set by the Dispatch phase.
            final AxisOperation axisOperation = msgContext.getOperationContext().getAxisOperation();

            if (axisOperation == null) {
                final String err = "Operation is not located, if this is Doc/lit style the SOAP-ACTION should specified via the " +
                        "SOAP Action to use the RawXMLProvider";

                //eadcFailure(msgContext, err);
                eadcError = err;

                throw new AxisFault(err);
            }

            final String randomUUID = Constants.UUID_PREFIX + UUID.randomUUID();
            final String methodName;

            if ((axisOperation.getName() != null) && ((methodName = JavaUtils.xmlNameToJavaIdentifier(axisOperation.getName().getLocalPart())) != null)) {

                final SOAPHeader soapHeader = msgContext.getEnvelope().getHeader();
                //  Identification of the TLS Common Name of the client.
                final String clientCommonName = EventLogUtil.getClientCommonName(msgContext);
                LOGGER.info("[ITI-41] Incoming XDR Request from '{}'", clientCommonName);

                final EventLog eventLog = new EventLog();
                eventLog.setReqM_ParticipantObjectID(getMessageID(msgContext.getEnvelope()));
                eventLog.setReqM_ParticipantObjectDetail(msgContext.getEnvelope().getHeader().toString().getBytes());
                eventLog.setSC_UserID(clientCommonName);
                eventLog.setSourceip(EventLogUtil.getSourceGatewayIdentifier(msgContext));
                eventLog.setTargetip(EventLogUtil.getTargetGatewayIdentifier());

                if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                    loggerClinical.debug("Incoming XDR Request Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(msgContext.getEnvelope())));
                }

                if (StringUtils.equals("documentRecipient_ProvideAndRegisterDocumentSetB", methodName)) {

                    /* Validate incoming request */
                    final String requestMessage = XMLUtil.prettyPrint(XMLUtils.toDOM(msgContext.getEnvelope().getBody().getFirstElement()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateXDRMessage(requestMessage, NcpSide.NCP_A, null);
                    }
                    final ProvideAndRegisterDocumentSetRequestType wrappedParam = (ProvideAndRegisterDocumentSetRequestType) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    eventLog.setNcpSide(NcpSide.NCP_A);
                    final RegistryResponseType registryResponse = xdrServiceSkeleton.documentRecipient_ProvideAndRegisterDocumentSetB(wrappedParam, soapHeader, eventLog);

                    envelope = toEnvelope(getSOAPFactory(msgContext), registryResponse, false);

                    eventLog.setResM_ParticipantObjectID(randomUUID);
                    eventLog.setResM_ParticipantObjectDetail(envelope.getHeader().toString().getBytes());
                    eventLog.setQueryByParameter(" ");
                    eventLog.setHciIdentifier(" ");

                    EventLogUtil.extractQueryByParamFromHeader(eventLog, msgContext, "PRPA_IN201305UV02", "controlActProcess", "queryByParameter");
                    EventLogUtil.extractHCIIdentifierFromHeader(eventLog, msgContext);

                    final AuditService auditService = AuditServiceFactory.getInstance();
                    auditService.write(eventLog, "", "1");

                    /* Validate outgoing response */
                    final String responseMessage = XMLUtil.prettyPrint(XMLUtils.toDOM(envelope.getBody().getFirstElement()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateXDRMessage(responseMessage, NcpSide.NCP_A, null);
                    }
                    if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
                        loggerClinical.debug("Response Header:\n{}", envelope.getHeader().toString());
                        loggerClinical.debug("Outgoing XDR Response Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(envelope)));
                    }
                    // eADC: extract of the eDispense CDA required by the KPIs.
                    eDispenseCda = EadcUtilWrapper.toXmlDocument(wrappedParam.getDocument().get(0).getValue());

                } else {
                    final String err = "Method not found: '" + methodName + "'";
                    LOGGER.error(err);

                    //eadcFailure(msgContext, err);
                    eadcError = err;

                    throw new RuntimeException(err);
                }

                endTime = new Date();
                newMsgContext.setEnvelope(envelope);
                newMsgContext.getOptions().setMessageId(randomUUID);

                if(!EadcUtilWrapper.hasTransactionErrors(envelope)) {
                    EadcUtilWrapper.invokeEadc(msgContext, newMsgContext, null, eDispenseCda, startTime,
                            endTime, Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC, EadcUtil.Direction.INBOUND,
                            ServiceType.DOCUMENT_EXCHANGED_RESPONSE);
                } else {
                    eadcError = EadcUtilWrapper.getTransactionErrorDescription(envelope);
                }
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);

            //eadcFailure(msgContext, e.getMessage());
            eadcError = e.getMessage();

            throw AxisFault.makeFault(e);
        } finally {
            if(!eadcError.isEmpty()) {
                EadcUtilWrapper.invokeEadcFailure(msgContext, newMsgContext, null, eDispenseCda, startTime, endTime,
                        tr.com.srdc.epsos.util.Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC, EadcUtil.Direction.INBOUND,
                        ServiceType.DOCUMENT_EXCHANGED_RESPONSE, eadcError);
                eadcError = "";
            }
        }

    }

//    private boolean hasTransactionErrors(SOAPEnvelope envelope) {
//        Iterator<OMElement> it = envelope.getBody().getChildElements();
//        String errorDescription = null;
//        while(it.hasNext()) {
//            OMElement elementDocSet = it.next();
//            if(StringUtils.equals(elementDocSet.getLocalName(), "RegistryResponse")) {
//                String status = elementDocSet.getAttributeValue(QName.valueOf("status"));
//                if(StringUtils.equals(status, AdhocQueryResponseStatus.FAILURE)) {
//                    return true;
//                }
//            }
//            it = elementDocSet.getChildElements();
//        }
//        return false;
//    }
//
//    private String getTransactionErrorDescription(SOAPEnvelope envelope) {
//        Iterator<OMElement> it = envelope.getBody().getChildElements();
//        String errorDescription = "unknown";
//        while(it.hasNext()) {
//            OMElement elementDocSet = it.next();
//            if(StringUtils.equals(elementDocSet.getLocalName(), "RegistryError")) {
//                String err = elementDocSet.getAttributeValue(QName.valueOf("errorCode"));
//                String cod = elementDocSet.getAttributeValue(QName.valueOf("codeContext"));
//                errorDescription = cod + " [" + err + "]";
//                break;
//            }
//            it = elementDocSet.getChildElements();
//        }
//        return errorDescription;
//    }

    /*
    private void eadcFailure(MessageContext messageContext, String errorDescription) {
        var transactionStartTime = new Date();
        Date transactionEndTime = new Date();
        MessageContext ctx = new MessageContext();
        EadcUtilWrapper.invokeEadcFailure(messageContext, ctx, null, null,
                transactionStartTime, transactionEndTime, Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC,
                EadcUtil.Direction.INBOUND, ServiceType.DOCUMENT_EXCHANGED_RESPONSE, errorDescription);
    }
    */

    private OMElement toOM(final ProvideAndRegisterDocumentSetRequestType param, final boolean optimizeContent) throws AxisFault {

        try {
            final OMFactory factory = OMAbstractFactory.getOMFactory();
            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            final JaxbRIDataSource source = new JaxbRIDataSource(ProvideAndRegisterDocumentSetRequestType.class,
                    param, marshaller, "urn:ihe:iti:xds-b:2007", "ProvideAndRegisterDocumentSetRequest");
            final OMNamespace namespace = factory.createOMNamespace("urn:ihe:iti:xds-b:2007", null);

            return factory.createOMElement(source, "ProvideAndRegisterDocumentSetRequest", namespace);

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final ProvideAndRegisterDocumentSetRequestType param,
                                    final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));

        return envelope;
    }

    private OMElement toOM(final RegistryResponseType param, final boolean optimizeContent) throws AxisFault {

        try {
            final OMFactory factory = OMAbstractFactory.getOMFactory();
            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            final JaxbRIDataSource source = new JaxbRIDataSource(RegistryResponseType.class, param, marshaller,
                    "urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", "RegistryResponse");
            final OMNamespace namespace = factory.createOMNamespace("urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", null);

            return factory.createOMElement(source, "RegistryResponse", namespace);

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType param,
                                    final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));

        return envelope;
    }

    /**
     * Returns default SOAP envelope.
     */
    private SOAPEnvelope toEnvelope(final SOAPFactory factory) {
        return factory.getDefaultEnvelope();
    }

    private Object fromOM(final OMElement param, final Class type, final Map extraNamespaces) throws AxisFault {

        try {

            final Unmarshaller unmarshaller = wsContext.createUnmarshaller();

            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    /**
     * A utility method that copies the namespaces from the SOAPEnvelope.
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

    private AxisFault createAxisFault(final Exception e) {

        final AxisFault axisFault;
        final Throwable cause = e.getCause();
        if (cause != null) {
            axisFault = new AxisFault(e.getMessage(), cause);
        } else {
            axisFault = new AxisFault(e.getMessage());
        }

        return axisFault;
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

        public void serialize(final OutputStream output, final OMOutputFormat format) throws XMLStreamException {

            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), output);

            } catch (final JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }

        public void serialize(final Writer writer, final OMOutputFormat format) throws XMLStreamException {
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

                final OMDocument omDocument = OMAbstractFactory.getOMFactory().createOMDocument();
                final Marshaller marshaller = wsContext.createMarshaller();
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), omDocument.getSAXResult());

                return omDocument.getOMDocumentElement().getXMLStreamReader();

            } catch (final JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }
    }
}
