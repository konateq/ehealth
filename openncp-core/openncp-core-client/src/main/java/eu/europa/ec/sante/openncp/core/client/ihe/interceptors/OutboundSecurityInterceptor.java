package eu.europa.ec.sante.openncp.core.client.ihe.interceptors;

import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.SecurityContext;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.soap.wssecurity.Security;
import org.opensaml.soap.wssecurity.impl.SecurityBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.List;

public class OutboundSecurityInterceptor extends AbstractSecurityInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundSecurityInterceptor.class);

    private final MarshallerFactory marshallerFactory;

    static {
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            LOGGER.error("InitializationException: '{}'", e.getMessage());
        }
    }

    public OutboundSecurityInterceptor() {
        super(Phase.PRE_PROTOCOL);
        marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
    }

    @Override
    public void handleMessage(final SoapMessage message) throws Fault {
        try {

            final SecurityBuilder securityBuilder = new SecurityBuilder();
            final Security security = securityBuilder.buildObject();

            final SamlDetails samlDetails = SecurityContextProvider.getSecurityContext()
                    .flatMap(SecurityContext::getSamlDetails)
                    .orElseThrow(() -> new IllegalStateException("Could not get samlDetails from the securityContext"));
            final List<AssertionDetails> assertions = samlDetails.getAssertions();
            assertions.forEach((assertion) -> {
                security.getUnknownXMLObjects().add(assertion.getAssertion());
            });

            final Marshaller marshaller = marshallerFactory.getMarshaller(security);
            final Element marshalledSecurity = marshaller.marshall(security);

            final SoapHeader header = new SoapHeader(security.getElementQName(), marshalledSecurity);
            header.setMustUnderstand(false);
            message.getHeaders().add(header);
        } catch (final Exception e) {
            throw new RuntimeException("Error adding the assertions to the security header", e);
        }
    }
}
