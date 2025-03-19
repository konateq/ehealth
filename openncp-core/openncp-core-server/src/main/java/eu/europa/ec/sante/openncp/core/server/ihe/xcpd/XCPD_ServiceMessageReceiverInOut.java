package eu.europa.ec.sante.openncp.core.server.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.AuditService;
import eu.europa.ec.sante.openncp.common.audit.AuditServiceFactory;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.OpenNCPErrorCodeException;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201306UV02;
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
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.receivers.AbstractInOutMessageReceiver;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;

/**
 * XCPD_ServiceMessageReceiverInOut message receiver
 */
public class XCPD_ServiceMessageReceiverInOut extends AbstractInOutMessageReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(XCPD_ServiceMessageReceiverInOut.class);
    private static final JAXBContext wsContext;
    private static final String PRPA_IN_201305_UV_02 = "PRPA_IN201305UV02";
    private static final String URN_HL_7_ORG_V_3 = "urn:hl7-org:v3";

    static {

        LOGGER.debug("Loading the WS-Security init libraries in XCPD_ServiceMessageReceiverInOut XCPD 2009");
        org.apache.xml.security.Init.init();
    }

    static {

        JAXBContext jc = null;
        try {
            jc = JAXBContext.newInstance(PRPAIN201305UV02.class, PRPAIN201306UV02.class);
        } catch (javax.xml.bind.JAXBException ex) {
            LOGGER.error("Unable to create JAXBContext: '{}'", ex.getMessage(), ex);
        } finally {
            wsContext = jc;
        }
    }

    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    private String getMessageID(SOAPEnvelope envelope) {

        Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(
                new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getText();
        } else {
            return Constants.UUID_PREFIX;
        }
    }

    public void invokeBusinessLogic(MessageContext msgContext, MessageContext newMsgContext) throws AxisFault {
        String eadcError = "";

        // Start Date for eADC
        Date startTime = new Date();

        try {
            //  Identification of the TLS Common Name of the client.
            String clientCommonName = EventLogUtil.getClientCommonName(msgContext);
            LOGGER.info("[ITI-55] Incoming XCPD Request from '{}'", clientCommonName);

            // Get the implementation class for the Web Service
            Object serviceObject = getTheImplementationObject(msgContext);
            SOAPHeader soapHeader = msgContext.getEnvelope().getHeader();

            // Prepare EventLog for audit purpose.
            EventLog eventLog = new EventLog();
            eventLog.setReqM_ParticipantObjectID(getMessageID(msgContext.getEnvelope()));
            eventLog.setReqM_ParticipantObjectDetail(msgContext.getEnvelope().getHeader().toString().getBytes());
            eventLog.setSC_UserID(clientCommonName);
            eventLog.setSourceip(EventLogUtil.getSourceGatewayIdentifier(msgContext));
            eventLog.setTargetip(EventLogUtil.getTargetGatewayIdentifier());

            if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                loggerClinical.debug("Incoming XCPD Request Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(msgContext.getEnvelope())));
            }

            /* Validate incoming request message */
            String message = XMLUtil.prettyPrint(XMLUtils.toDOM(msgContext.getEnvelope().getBody().getFirstElement()));
            if (GazelleValidation.isValidationEnable()) {
                GazelleValidation.validatePatientDemographicRequest(message, NcpSide.NCP_A);
            }

            XCPD_ServiceSkeleton skeleton = (XCPD_ServiceSkeleton) serviceObject;
            // Out Envelop
            SOAPEnvelope envelope;
            // Find the axisOperation that has been set by the Dispatch phase.
            AxisOperation axisOperation = msgContext.getOperationContext().getAxisOperation();
            if (axisOperation == null) {
                String err = "Operation is not located, if this is doclit style the SOAP-ACTION " +
                        "should specified via the SOAP Action to use the RawXMLProvider";
                eadcError = err;
                throw new AxisFault(err);
            }

            String randomUUID = Constants.UUID_PREFIX + UUID.randomUUID();
            String methodName;

            if ((axisOperation.getName() != null) && ((methodName = JavaUtils.xmlNameToJavaIdentifier(axisOperation.getName().getLocalPart())) != null)) {

                if (StringUtils.equals("respondingGateway_PRPA_IN201305UV02", methodName)) {

                    PRPAIN201305UV02 wrappedParam = (PRPAIN201305UV02) fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            PRPAIN201305UV02.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    PRPAIN201306UV02 PRPAIN201306UV02 = skeleton.respondingGateway_PRPA_IN201305UV02(wrappedParam, soapHeader, eventLog);

                    envelope = toEnvelope(getSOAPFactory(msgContext), PRPAIN201306UV02);

                    /* Validate response message */
                    if (GazelleValidation.isValidationEnable()) {
                        GazelleValidation.validatePatientDemographicResponse(XMLUtil.prettyPrint(XMLUtils.toDOM(
                                envelope.getBody().getFirstElement())), NcpSide.NCP_A);
                    }
                    eventLog.setResM_ParticipantObjectID(randomUUID);
                    eventLog.setResM_ParticipantObjectDetail(envelope.getHeader().toString().getBytes());
                    eventLog.setNcpSide(NcpSide.NCP_A);

                    EventLogUtil.extractXcpdQueryByParamFromHeader(eventLog, msgContext, PRPA_IN_201305_UV_02, "controlActProcess", "queryByParameter");
                    EventLogUtil.extractHCIIdentifierFromHeader(eventLog, msgContext);

                    AuditService auditService = AuditServiceFactory.getInstance();
                    auditService.write(eventLog, "", "1");
                    LOGGER.info("EventLog: '{}' generated.", eventLog.getEventType());

                    if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                        loggerClinical.debug("Outgoing XCPD Response Message:\n{}", XMLUtil.prettyPrint(XMLUtils.toDOM(envelope)));
                    }

                } else {
                    String err = "Method not Found: '" + methodName + "'";
                    LOGGER.error(err);
                    eadcError = err;
                    throw new RuntimeException(err);
                }
                newMsgContext.setEnvelope(envelope);
                newMsgContext.getOptions().setMessageId(randomUUID);

                EadcUtilWrapper.invokeEadc(msgContext, newMsgContext, null, null, startTime,
                        new Date(), Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC, EadcUtil.Direction.INBOUND,
                        ServiceType.PATIENT_IDENTIFICATION_RESPONSE);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            eadcError = e.getMessage();

            final AxisFault axisFault = AxisFault.makeFault(e);
            if (e instanceof OpenNCPErrorCodeException) {
                axisFault.setFaultSubCodes(axisFault.getFaultSubCodes() != null ? axisFault.getFaultSubCodes() : new ArrayList<>());
                axisFault.getFaultSubCodes().add(new QName(((OpenNCPErrorCodeException) e).getErrorCode().getCode()));
            }
            throw axisFault;
        } finally {
            if (!eadcError.isEmpty()) {
                EadcUtilWrapper.invokeEadcFailure(msgContext, newMsgContext, null, null, startTime,
                        new Date(), Constants.COUNTRY_CODE, EadcEntry.DsTypes.EADC, EadcUtil.Direction.INBOUND,
                        ServiceType.PATIENT_IDENTIFICATION_RESPONSE, eadcError);
            }
        }
    }

    private OMElement toOM(PRPAIN201306UV02 param) throws AxisFault {

        try {

            Marshaller marshaller = wsContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            OMFactory factory = OMAbstractFactory.getOMFactory();
            JaxbRIDataSource source = new JaxbRIDataSource(PRPAIN201306UV02.class, param, marshaller, URN_HL_7_ORG_V_3, "PRPA_IN201306UV02");
            OMNamespace namespace = factory.createOMNamespace(URN_HL_7_ORG_V_3, null);

            return factory.createOMElement(source, "PRPA_IN201306UV02", namespace);

        } catch (JAXBException bex) {
            throw AxisFault.makeFault(bex);
        }
    }

    private SOAPEnvelope toEnvelope(SOAPFactory factory, PRPAIN201306UV02 param) throws AxisFault {

        SOAPEnvelope envelope = factory.getDefaultEnvelope();
        envelope.getBody().addChild(toOM(param));

        return envelope;
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
     * A utility method that copies the namespaces from the SOAPEnvelope
     */
    private Map getEnvelopeNamespaces(SOAPEnvelope env) {

        Map returnMap = new HashMap();
        Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
            OMNamespace ns = (OMNamespace) namespaceIterator.next();
            returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
        }
        return returnMap;
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
        public JaxbRIDataSource(Class clazz, Object obj, Marshaller marshaller, String nsuri, String name) {
            this.outClazz = clazz;
            this.outObject = obj;
            this.marshaller = marshaller;
            this.nsuri = nsuri;
            this.name = name;
        }

        public void serialize(OutputStream output) throws XMLStreamException {

            try {
                marshaller.marshal(new JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), output);

            } catch (JAXBException e) {
                throw new XMLStreamException("Error in JAXB marshalling", e);
            }
        }

        public void serialize(Writer writer) throws XMLStreamException {

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

                OMDocument omDocument = OMAbstractFactory.getOMFactory().createOMDocument();
                Marshaller marshallerWsContext = wsContext.createMarshaller();
                marshallerWsContext.marshal(new javax.xml.bind.JAXBElement(new QName(nsuri, name), outObject.getClass(), outObject), omDocument.getSAXResult());

                return omDocument.getOMDocumentElement().getXMLStreamReader();

            } catch (JAXBException e) {
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
