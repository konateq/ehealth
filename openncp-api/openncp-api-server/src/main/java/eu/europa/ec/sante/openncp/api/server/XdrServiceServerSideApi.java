package eu.europa.ec.sante.openncp.api.server;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.common.SecurityContext;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import eu.europa.ec.sante.openncp.core.server.EventLogUtil;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xdr.DocumentRecipientPortType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ObjectFactory;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ProvideAndRegisterDocumentSetRequest;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RegistryResponseType;
import eu.europa.ec.sante.openncp.core.server.ihe.xdr.XdrServiceServerSide;
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

@WebService(serviceName = "XDR_Service", portName = "DocumentRecipient_PortType",
        targetNamespace = "urn:ihe:iti:xds-b:2007", wsdlLocation = "classpath:xdr/XDR_Service.wsdl",
        endpointInterface = "eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xdr.DocumentRecipientPortType")
@Service
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class XdrServiceServerSideApi implements DocumentRecipientPortType {
    private static final Logger LOGGER = LoggerFactory.getLogger(XdrServiceServerSideApi.class);

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final XdrServiceServerSide xdrServiceServerSide;

    static {
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            LOGGER.error("InitializationException: '{}'", e.getMessage());
        }
    }

    public XdrServiceServerSideApi(final XdrServiceServerSide xdrServiceServerSide) {
        this.xdrServiceServerSide = Validate.notNull(xdrServiceServerSide, "xdr xdrService must not be null");
    }


    @Override
    public RegistryResponseType documentRecipientProvideAndRegisterDocumentSetB(final ProvideAndRegisterDocumentSetRequest body) {
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
            return xdrServiceServerSide.saveDocument(body, soapHeader, eventLog);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
