package eu.europa.ec.sante.openncp.core.common.interceptors;

import eu.europa.ec.sante.openncp.common.security.util.AssertionUtil;
import eu.europa.ec.sante.openncp.core.common.ImmutableSecurityContext;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.WSS4JConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssertionsInInterceptor extends AbstractSoapInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionsInInterceptor.class);

    public AssertionsInInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    @Override
    public void handleMessage(final SoapMessage soapMessage) throws Fault {
        Header securityHeader = null;
        for (final Header header : soapMessage.getHeaders()) {
            final QName n = header.getName();
            if ("Security".equals(n.getLocalPart()) &&
                    (n.getNamespaceURI().equals(WSS4JConstants.WSSE_NS) || n.getNamespaceURI().equals(WSS4JConstants.WSSE11_NS))) {
                securityHeader = header;
            }
        }

        if (securityHeader == null) {
            throw new RuntimeException("No security header found");
        }

        final List<Assertion> assertions = new ArrayList<>();
        final Element el = (Element) securityHeader.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            toAssertion(child).ifPresent(assertions::add);
            child = DOMUtils.getNextElement(child);
        }

        final SamlDetails samlDetails = SamlDetails.of(assertions);
        SecurityContextProvider
                .getSecurityContext()
                .map(ImmutableSecurityContext::copyOf)
                .map(immutableSecurityContext -> immutableSecurityContext.withSamlDetails(samlDetails))
                .ifPresentOrElse(SecurityContextProvider::setAssertionContext,
                        () -> SecurityContextProvider.setAssertionContext(ImmutableSecurityContext.builder()
                                .samlDetails(samlDetails)
                                .build())
                );
    }

    private Optional<Assertion> toAssertion(final Element element) {
        if ("Assertion".equals(element.getLocalName()) &&
                (WSS4JConstants.SAML_NS.equals(element.getNamespaceURI()) || WSS4JConstants.SAML2_NS.equals(element.getNamespaceURI()))) {
            return AssertionUtil.toAssertion(element);
        } else {
            return Optional.empty();
        }
    }
}
