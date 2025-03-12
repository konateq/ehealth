package eu.europa.ec.sante.openncp.api.server;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.server.ihe.xcpd.XCPDServiceInterface;
import ihe.iti.xcpd._2009.RespondingGatewayPortType;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.feature.Features;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201306UV02;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceContext;

@WebService(serviceName = "XCPD_Service", portName = "RespondingGatewayPort",
        targetNamespace = "urn:ihe:iti:xcpd:2009", wsdlLocation = "classpath:xcpd/XCPD_Service.wsdl",
        endpointInterface = "ihe.iti.xcpd._2009.RespondingGatewayPortType")
@Service
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class XcpdService implements RespondingGatewayPortType {
    private final XCPDServiceInterface service;

    @Resource
    private WebServiceContext wsContext;

    public XcpdService(final XCPDServiceInterface service) {
        this.service = Validate.notNull(service, "xcpd service must not be null");
    }


    @Override
    public PRPAIN201306UV02 respondingGatewayPRPAIN201305UV02(final PRPAIN201305UV02 body) {
        // Access SOAP message from the CXF PhaseInterceptorChain
        final SoapMessage soapMessage = (SoapMessage) PhaseInterceptorChain.getCurrentMessage();
        final SOAPMessage message = soapMessage.getContent(SOAPMessage.class);
        final SOAPHeader soapHeader;
        try {
            soapHeader = message.getSOAPHeader();
        } catch (final SOAPException e) {
            throw new RuntimeException("Could not extract SoapHeader", e);
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
}
