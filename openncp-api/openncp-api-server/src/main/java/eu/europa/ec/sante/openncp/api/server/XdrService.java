package eu.europa.ec.sante.openncp.api.server;

import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xdr.DocumentRecipientPortType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ObjectFactory;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ProvideAndRegisterDocumentSetRequest;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RegistryResponseType;
import org.apache.cxf.feature.Features;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.jws.WebService;

@WebService(serviceName = "XDR_Service", portName = "DocumentRecipient_PortType",
        targetNamespace = "urn:ihe:iti:xds-b:2007", wsdlLocation = "classpath:xca/XDR_Service.wsdl",
        endpointInterface = "eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xdr.DocumentRecipientPortType")
@Service
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class XdrService implements DocumentRecipientPortType {
    private static final Logger LOGGER = LoggerFactory.getLogger(XdrService.class);

    private final ObjectFactory objectFactory = new ObjectFactory();

    static {
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            LOGGER.error("InitializationException: '{}'", e.getMessage());
        }
    }


    @Override
    public RegistryResponseType documentRecipientProvideAndRegisterDocumentSetB(final ProvideAndRegisterDocumentSetRequest body) {
        return null;
    }
}
