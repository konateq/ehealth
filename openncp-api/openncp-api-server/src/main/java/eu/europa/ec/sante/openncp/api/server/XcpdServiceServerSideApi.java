package eu.europa.ec.sante.openncp.api.server;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.common.SecurityContext;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import eu.europa.ec.sante.openncp.core.server.EventLogUtil;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201306UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType;
import eu.europa.ec.sante.openncp.core.server.ihe.xcpd.XcpdServiceServerSide;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.feature.Features;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPHeader;

import static eu.europa.ec.sante.openncp.core.server.EventLogUtil.getMessageID;

@WebService(serviceName = "XCPD_Service", portName = "RespondingGatewayPort",
        targetNamespace = "urn:ihe:iti:xcpd:2009", wsdlLocation = "classpath:xcpd/XCPD_Service.wsdl",
        endpointInterface = "eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType")
@Service
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class XcpdServiceServerSideApi implements RespondingGatewayPortType {
    private static final Logger LOGGER = LoggerFactory.getLogger(XcpdServiceServerSideApi.class);

    static {
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            LOGGER.error("InitializationException: '{}'", e.getMessage());
        }
    }

    private final XcpdServiceServerSide service;

    public XcpdServiceServerSideApi(final XcpdServiceServerSide service) {
        this.service = Validate.notNull(service, "xcpd service must not be null");
    }


    @Override
    public PRPAIN201306UV02 respondingGatewayPRPAIN201305UV02(final PRPAIN201305UV02 body) {
        final SoapMessage soapMessage = (SoapMessage) PhaseInterceptorChain.getCurrentMessage();
        final HttpServletRequest httpServletRequest = (HttpServletRequest) soapMessage.get(AbstractHTTPDestination.HTTP_REQUEST);


        final SOAPHeader soapHeader = SecurityContextProvider.getSecurityContext()
                .flatMap(SecurityContext::getSoapHeader)
                .orElseThrow(() -> new RuntimeException("No SOAP header found"));

        // Prepare EventLog for audit purpose.
        final EventLog eventLog = new EventLog();
        eventLog.setReqM_ParticipantObjectID(getMessageID(soapHeader));
        eventLog.setReqM_ParticipantObjectDetail(soapHeader.toString().getBytes());
        eventLog.setSC_UserID(EventLogUtil.getClientCommonName(httpServletRequest));
        eventLog.setSourceip(EventLogUtil.getSourceGatewayIdentifier(soapMessage));
        eventLog.setTargetip(EventLogUtil.getTargetGatewayIdentifier());

        try {
            return service.queryPatient(body, soapHeader, eventLog);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
