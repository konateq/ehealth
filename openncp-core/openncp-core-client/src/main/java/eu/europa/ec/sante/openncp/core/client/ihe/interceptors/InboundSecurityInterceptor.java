package eu.europa.ec.sante.openncp.core.client.ihe.interceptors;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.WSS4JConstants;

import javax.xml.namespace.QName;

/**
 * Fetches the inbound security header and puts it on the request context.
 * See ({@link OutboundSecurityInterceptor}
 */
public class InboundSecurityInterceptor extends AbstractSecurityInterceptor {

    public InboundSecurityInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addBefore(OutboundSecurityInterceptor.class.getName());
    }

    @Override
    public void handleMessage(final SoapMessage message) throws Fault {
        Header securityHeader = null;
        for (final Header header : message.getHeaders()) {
            final QName n = header.getName();
            if ("Security".equals(n.getLocalPart()) &&
                    (n.getNamespaceURI().equals(WSS4JConstants.WSSE_NS) || n.getNamespaceURI().equals(WSS4JConstants.WSSE11_NS))) {
                securityHeader = header;
            }
        }

        // Store the security header in the message context
        message.getExchange().put(SECURITY_HEADER_KEY, securityHeader);
    }
}
