package eu.europa.ec.sante.openncp.core.client.ihe.interceptors;

import eu.europa.ec.sante.openncp.core.client.ihe.context.SecurityHeaderContextHolder;
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
        final Element securityHeader = SecurityHeaderContextHolder.getSecurityHeader();

        if (securityHeader != null) {
            final QName securityQName = new QName(securityHeader.getNamespaceURI(), securityHeader.getLocalName());
            final SoapHeader outboundSecurityHeader = new SoapHeader(securityQName, securityHeader);

            final List<Header> headers = message.getHeaders();
            headers.add(outboundSecurityHeader);
        }

        SecurityHeaderContextHolder.clear();
    }
}
