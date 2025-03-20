package eu.europa.ec.sante.openncp.api.server;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201306UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType;
import eu.europa.ec.sante.openncp.core.server.ihe.xcpd.XCPDService;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.feature.Features;
import org.apache.cxf.headers.Header;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.wss4j.common.WSS4JConstants;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.WebServiceContext;

@WebService(serviceName = "XCPD_Service", portName = "RespondingGatewayPort",
        targetNamespace = "urn:ihe:iti:xcpd:2009", wsdlLocation = "classpath:xcpd/XCPD_Service.wsdl",
        endpointInterface = "eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType")
@Service
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class XcpdService implements RespondingGatewayPortType {
    private final XCPDService service;

    @Resource
    private WebServiceContext wsContext;

    public XcpdService(final XCPDService service) {
        this.service = Validate.notNull(service, "xcpd service must not be null");
    }


    @Override
    public PRPAIN201306UV02 respondingGatewayPRPAIN201305UV02(final PRPAIN201305UV02 body) {
        // Access SOAP message from the CXF PhaseInterceptorChain
        final SoapMessage soapMessage = (SoapMessage) PhaseInterceptorChain.getCurrentMessage();
        Header securityHeader = null;
        for (final Header header : soapMessage.getHeaders()) {
            final QName n = header.getName();
            if ("Security".equals(n.getLocalPart()) &&
                    (n.getNamespaceURI().equals(WSS4JConstants.WSSE_NS) || n.getNamespaceURI().equals(WSS4JConstants.WSSE11_NS))) {
                securityHeader = header;
                break;
            }
        }

        if (securityHeader == null || !(securityHeader.getObject() instanceof Element)) {
            throw new RuntimeException("No valid WS-Security header found in the CXF message");
        }

        SOAPHeader soapHeader = null;
        try {
            soapHeader = convertToSOAPHeader(securityHeader);
        } catch (SOAPException e) {
            throw new RuntimeException("Could not convert CXF Header to a javax.xml.SOAPHeader");
        }

        // Prepare EventLog for audit purpose.
        final EventLog eventLog = new EventLog();
//        eventLog.setReqM_ParticipantObjectID(getMessageID(msgContext.getEnvelope()));
//        eventLog.setReqM_ParticipantObjectDetail(msgContext.getEnvelope().getHeader().toString().getBytes());
//        eventLog.setSC_UserID(clientCommonName);
//        eventLog.setSourceip(EventLogUtil.getSourceGatewayIdentifier(msgContext));
//        eventLog.setTargetip(EventLogUtil.getTargetGatewayIdentifier());

        try {
            return service.queryPatient(body, soapHeader, eventLog);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SOAPHeader convertToSOAPHeader(Header header) throws SOAPException {
        Element securityElement = (Element) header.getObject();

        // Create a new SOAP Message to hold the SOAP Header
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapMessageOut = factory.createMessage();
        SOAPPart soapPart = soapMessageOut.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPHeader soapHeader = envelope.getHeader();

        if (soapHeader == null) {
            soapHeader = envelope.addHeader();
        }

        // Import the security element into the new SOAPHeader
        soapHeader.appendChild(soapHeader.getOwnerDocument().importNode(securityElement, true));

        return soapHeader;
    }
}
