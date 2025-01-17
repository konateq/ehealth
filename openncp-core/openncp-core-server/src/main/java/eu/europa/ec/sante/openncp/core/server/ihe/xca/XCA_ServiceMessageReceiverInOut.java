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
import eu.europa.ec.sante.openncp.core.common.constants.ihe.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcEntry;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtilWrapper;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.ServiceType;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogUtil;
import org.apache.axiom.om.*;
import org.apache.axiom.om.ds.AbstractOMDataSource;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.receivers.AbstractInOutMessageReceiver;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;

/**
 * XCA_ServiceMessageReceiverInOut message receiver
 */
public class XCA_ServiceMessageReceiverInOut extends AbstractInOutMessageReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(XCA_ServiceMessageReceiverInOut.class);
    private static final JAXBContext wsContext;

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

    private String getMessageID(final SOAPEnvelope envelope) {

        final Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getText();
        } else {
            return Constants.UUID_PREFIX;
        }
    }

    public void invokeBusinessLogic(final MessageContext msgContext, final MessageContext newMsgContext) throws AxisFault {
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
            // get the implementation class for the Web Service
            final Object obj = getTheImplementationObject(msgContext);

            final XCA_ServiceSkeleton skel = (XCA_ServiceSkeleton) obj;

            // Find the axisOperation that has been set by the Dispatch phase.
            final AxisOperation op = msgContext.getOperationContext().getAxisOperation();

            if (op == null) {
                final String err = "Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider";

                //eadcFailure(msgContext, err, ServiceType.DOCUMENT_LIST_RESPONSE);
                eadcError = err;
                serviceType = ServiceType.DOCUMENT_LIST_RESPONSE;

                throw new AxisFault(err);
            }

            final String randomUUID = Constants.UUID_PREFIX + UUID.randomUUID();
            final String methodName;

            if ((op.getName() != null) && ((methodName = JavaUtils.xmlNameToJavaIdentifier(op.getName().getLocalPart())) != null)) {

                final SOAPHeader sh = msgContext.getEnvelope().getHeader();
                //  Identification of the TLS Common Name of the client.
                final String clientCommonName = EventLogUtil.getClientCommonName(msgContext);

                final EventLog eventLog = new EventLog();
                eventLog.setReqM_ParticipantObjectID(getMessageID(msgContext.getEnvelope()));
                eventLog.setReqM_ParticipantObjectDetail(msgContext.getEnvelope().getHeader().toString().getBytes());
                eventLog.setSC_UserID(clientCommonName);
                eventLog.setSourceip(EventLogUtil.getSourceGatewayIdentifier(msgContext));
                eventLog.setTargetip(EventLogUtil.getTargetGatewayIdentifier());

                if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name()) && loggerClinical.isDebugEnabled()) {
                    loggerClinical.debug("Incoming XCA Request Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(msgContext.getEnvelope())));
                }
                if (StringUtils.equals(XCAOperation.SERVICE_CROSS_GATEWAY_QUERY, methodName)) {

                    LOGGER.info("[ITI-38] Incoming XCA List from '{}'", clientCommonName);
                    /* Validate incoming query request */
                    final String requestMessage = XMLUtil.prettyPrintForValidation(XMLUtils.toDOM(msgContext.getEnvelope().getBody().getFirstElement()));

                    final AdhocQueryResponse adhocQueryResponse1;
                    final AdhocQueryRequest wrappedParam = (AdhocQueryRequest) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(), AdhocQueryRequest.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    final List<ClassCode> classCodes = extractClassCodesFromQueryRequest(wrappedParam);
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCrossCommunityAccess(requestMessage, NcpSide.NCP_A, classCodes);
                    }

                    adhocQueryResponse1 = skel.respondingGateway_CrossGatewayQuery(wrappedParam, sh, eventLog);
                    envelope = toEnvelope(getSOAPFactory(msgContext), adhocQueryResponse1, false);
                    eventLog.setResM_ParticipantObjectID(randomUUID);
                    eventLog.setResM_ParticipantObjectDetail(envelope.getHeader().toString().getBytes());
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("[Audit Debug] Responder: ParticipantId: '{}'\nObjectDetail: '{}'",
                                randomUUID, envelope.getHeader().toString());
                    }
                    eventLog.setNcpSide(NcpSide.NCP_A);
                    eventLog.setQueryByParameter(" ");
                    eventLog.setHciIdentifier(" ");

                    EventLogUtil.extractQueryByParamFromHeader(eventLog, msgContext, "PRPA_IN201305UV02", "controlActProcess", "queryByParameter");
                    EventLogUtil.extractHCIIdentifierFromHeader(eventLog, msgContext);

                    AuditServiceFactory.getInstance().write(eventLog, "", "1");

                    /* Validate outgoing query response */
                    final String responseMessage = XMLUtil.prettyPrintForValidation(XMLUtils.toDOM(envelope.getBody().getFirstElement()));
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCrossCommunityAccess(responseMessage, NcpSide.NCP_A, classCodes);
                    }

                    if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name()) && loggerClinical.isDebugEnabled()) {
                        loggerClinical.debug("Response Header:\n{}", envelope.getHeader().toString());
                        loggerClinical.debug("Outgoing XCA Response Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(envelope)));
                    }
                    serviceType = ServiceType.DOCUMENT_LIST_RESPONSE;

                } else if (StringUtils.equals(XCAOperation.SERVICE_CROSS_GATEWAY_RETRIEVE, methodName)) {

                    LOGGER.info("[ITI-39] Incoming XCA Retrieve from '{}'", clientCommonName);
                    /* Validate incoming retrieve request */
                    final String requestMessage = XMLUtil.prettyPrint(XMLUtils.toDOM(msgContext.getEnvelope().getBody().getFirstElement()));

                    final RetrieveDocumentSetRequestType wrappedParam = (RetrieveDocumentSetRequestType) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(), RetrieveDocumentSetRequestType.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateCrossCommunityAccess(requestMessage, NcpSide.NCP_A, null);
                    }

                    final OMFactory factory = OMAbstractFactory.getOMFactory();
                    final OMNamespace ns = factory.createOMNamespace("urn:ihe:iti:xds-b:2007", "");
                    final OMElement omElement = factory.createOMElement("RetrieveDocumentSetResponse", ns);
                    skel.respondingGateway_CrossGatewayRetrieve(wrappedParam, sh, eventLog, omElement);

                    envelope = toEnvelope(getSOAPFactory(msgContext), omElement);

                    eventLog.setResM_ParticipantObjectID(randomUUID);
                    eventLog.setResM_ParticipantObjectDetail(envelope.getHeader().toString().getBytes());
                    eventLog.setNcpSide(NcpSide.NCP_A);
                    eventLog.setQueryByParameter(" ");
                    eventLog.setHciIdentifier(" ");

                    EventLogUtil.extractQueryByParamFromHeader(eventLog, msgContext, "PRPA_IN201305UV02", "controlActProcess", "queryByParameter");
                    EventLogUtil.extractHCIIdentifierFromHeader(eventLog, msgContext);

                    AuditServiceFactory.getInstance().write(eventLog, "", "1");

                    if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name()) && loggerClinical.isDebugEnabled()) {
                        loggerClinical.debug("Outgoing XCA Response Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(envelope)));
                    }

                    final Options options = new Options();
                    options.setProperty(org.apache.axis2.Constants.Configuration.ENABLE_MTOM, org.apache.axis2.Constants.VALUE_TRUE);
                    newMsgContext.setOptions(options);

                    /* Validate outgoing retrieve response */
                    final String responseMessage = XMLUtil.prettyPrint(XMLUtils.toDOM(envelope.getBody().getFirstElement()));
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
                EadcUtilWrapper.invokeEadcFailure(msgContext, newMsgContext, null, clinicalDocument, startTime, endTime,
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

    private OMElement toOM(final AdhocQueryRequest param, final boolean optimizeContent) throws AxisFault {

        try {

            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final JaxbRIDataSource source = new JaxbRIDataSource(AdhocQueryRequest.class,
                    param, marshaller, "urn:oasis:names:tc:ebxml-regrep:xsd:query:3.0", "AdhocQueryRequest");
            final OMNamespace namespace = factory.createOMNamespace("urn:oasis:names:tc:ebxml-regrep:xsd:query:3.0", null);

            return factory.createOMElement(source, "AdhocQueryRequest", namespace);

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final AdhocQueryRequest param,
                                    final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));

        return envelope;
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

    private OMElement toOM(final RetrieveDocumentSetRequestType param, final boolean optimizeContent) throws AxisFault {

        try {

            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            final OMFactory factory = OMAbstractFactory.getOMFactory();

            final JaxbRIDataSource source = new JaxbRIDataSource(RetrieveDocumentSetRequestType.class, param,
                    marshaller, "urn:ihe:iti:xds-b:2007", "RetrieveDocumentSetRequest");
            final OMNamespace namespace = factory.createOMNamespace("urn:ihe:iti:xds-b:2007", null);

            return factory.createOMElement(source, "RetrieveDocumentSetRequest", namespace);

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final RetrieveDocumentSetRequestType param, final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));

        return envelope;
    }

    private OMElement toOM(final RetrieveDocumentSetResponseType param, final boolean optimizeContent) throws AxisFault {

        try {

            final Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            final OMFactory factory = OMAbstractFactory.getOMFactory();
            final JaxbRIDataSource source = new JaxbRIDataSource(RetrieveDocumentSetResponseType.class,
                    param, marshaller, "urn:ihe:iti:xds-b:2007", "RetrieveDocumentSetResponse");

            final OMNamespace namespace = factory.createOMNamespace("urn:ihe:iti:xds-b:2007", null);

            return factory.createOMElement(source, "RetrieveDocumentSetResponse", namespace);

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final RetrieveDocumentSetResponseType param, final boolean optimizeContent) throws AxisFault {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param, optimizeContent));

        return envelope;
    }

    private SOAPEnvelope toEnvelope(final SOAPFactory factory, final OMElement param) {

        final SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(param);

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
            final Unmarshaller unmarshaller = wsContext.createUnmarshaller();

            return unmarshaller.unmarshal(param.getXMLStreamReaderWithoutCaching(), type).getValue();

        } catch (final JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    /**
     * A utility method that copies the namespaces from the SOAPEnvelope
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

        final AxisFault f;
        final Throwable cause = e.getCause();
        if (cause != null) {
            f = new AxisFault(e.getMessage(), cause);
        } else {
            f = new AxisFault(e.getMessage());
        }

        return f;
    }

    class JaxbRIDataSource  extends AbstractOMDataSource {

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
