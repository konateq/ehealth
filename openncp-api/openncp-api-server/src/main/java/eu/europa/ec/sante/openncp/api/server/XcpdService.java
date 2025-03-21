package eu.europa.ec.sante.openncp.api.server;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.common.SecurityContext;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201306UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType;
import eu.europa.ec.sante.openncp.core.server.ihe.xcpd.XCPDService;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.feature.Features;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.WebServiceContext;

@WebService(serviceName = "XCPD_Service", portName = "RespondingGatewayPort",
        targetNamespace = "urn:ihe:iti:xcpd:2009", wsdlLocation = "classpath:xcpd/XCPD_Service.wsdl",
        endpointInterface = "eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType")
@Service
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class XcpdService implements RespondingGatewayPortType {
    private static final Logger LOGGER = LoggerFactory.getLogger(XcpdService.class);

    static {
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            LOGGER.error("InitializationException: '{}'", e.getMessage());
        }
    }

    private final XCPDService service;

    @Resource
    private WebServiceContext wsContext;

    public XcpdService(final XCPDService service) {
        this.service = Validate.notNull(service, "xcpd service must not be null");
    }


    @Override
    public PRPAIN201306UV02 respondingGatewayPRPAIN201305UV02(final PRPAIN201305UV02 body) {


        // Prepare EventLog for audit purpose.
        final EventLog eventLog = new EventLog();
//        eventLog.setReqM_ParticipantObjectID(getMessageID(msgContext.getEnvelope()));
//        eventLog.setReqM_ParticipantObjectDetail(msgContext.getEnvelope().getHeader().toString().getBytes());
//        eventLog.setSC_UserID(clientCommonName);
//        eventLog.setSourceip(EventLogUtil.getSourceGatewayIdentifier(msgContext));
//        eventLog.setTargetip(EventLogUtil.getTargetGatewayIdentifier());

        final SOAPHeader soapHeader = SecurityContextProvider.getSecurityContext()
                .flatMap(SecurityContext::getSoapHeader)
                .orElseThrow(() -> new RuntimeException("No SOAP header found"));

        try {
            return service.queryPatient(body, soapHeader, eventLog);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
