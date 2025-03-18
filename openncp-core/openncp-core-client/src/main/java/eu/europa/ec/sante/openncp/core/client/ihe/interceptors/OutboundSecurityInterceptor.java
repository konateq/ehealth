package eu.europa.ec.sante.openncp.core.client.ihe.interceptors;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

public class OutboundSecurityInterceptor extends AbstractSecurityInterceptor {

    private static final String SECURITY_HEADER_KEY = "SECURITY_HEADER";

    public OutboundSecurityInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(final SoapMessage message) throws Fault {
        // Retrieve the security header from request context
        final Element securityHeader = (Element) message.getExchange().get(SECURITY_HEADER_KEY);

        if (securityHeader != null) {
            final QName securityQName = new QName(securityHeader.getNamespaceURI(), securityHeader.getLocalName());
            final SoapHeader outboundSecurityHeader = new SoapHeader(securityQName, securityHeader);

            final List<Header> headers = message.getHeaders();
            headers.add(outboundSecurityHeader);
        }
    }
}
