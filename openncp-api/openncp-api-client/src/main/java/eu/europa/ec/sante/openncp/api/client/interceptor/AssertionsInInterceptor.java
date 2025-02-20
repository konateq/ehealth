package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.api.client.ImmutableRequestContext;
import eu.europa.ec.sante.openncp.api.client.RequestContext;
import eu.europa.ec.sante.openncp.api.client.RequestContextProvider;
import eu.europa.ec.sante.openncp.common.security.util.AssertionUtil;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.WSS4JConstants;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
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

        final Element securityElement = (Element) securityHeader.getObject();
        final NodeList assertionList = securityElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion");
        final List<Assertion> assertions = AssertionUtil.toAssertions(assertionList);
        final SamlDetails samlDetails = SamlDetails.of(assertions);
        final RequestContext requestContext = ImmutableRequestContext.builder().samlDetails(samlDetails).build();

        RequestContextProvider.replaceRequestContext(requestContext);
    }

    private static Optional<Assertion> toAssertion(final Element element) {
        if ("Assertion".equals(element.getLocalName()) &&
                (WSS4JConstants.SAML_NS.equals(element.getNamespaceURI()) || WSS4JConstants.SAML2_NS.equals(element.getNamespaceURI()))) {
            return AssertionUtil.toAssertion(element);
        } else {
            return Optional.empty();
        }
    }
}
