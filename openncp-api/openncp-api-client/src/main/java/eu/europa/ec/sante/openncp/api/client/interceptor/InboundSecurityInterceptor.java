package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.core.client.ihe.context.SecurityHeaderContextHolder;
import eu.europa.ec.sante.openncp.core.client.ihe.interceptors.AbstractSecurityInterceptor;
import eu.europa.ec.sante.openncp.core.client.ihe.interceptors.OutboundSecurityInterceptor;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.WSS4JConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Fetches the inbound security header and puts it on the request context.
 * See ({@link OutboundSecurityInterceptor}
 */
public class InboundSecurityInterceptor extends AbstractSecurityInterceptor {

    public InboundSecurityInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(final SoapMessage message) throws Fault {
        SecurityHeaderContextHolder.clear();

        for (final Header header : message.getHeaders()) {
            final QName n = header.getName();
            if ("Security".equals(n.getLocalPart()) &&
                    (n.getNamespaceURI().equals(WSS4JConstants.WSSE_NS) || n.getNamespaceURI().equals(WSS4JConstants.WSSE11_NS))) {

                SecurityHeaderContextHolder.setSecurityHeader(header);
//                if (header instanceof SoapHeader) {
//                    final SoapHeader soapHeader = (SoapHeader) header;
//                    final Object headerObject = soapHeader.getObject();
//
//                    if (headerObject instanceof Element) {
//                        final Element securityElement = (Element) headerObject;
//                        final Element securityElementCopy = deepCopyElement(securityElement);
//
//                        // Store security header in ThreadLocal storage
//                        SecurityHeaderContextHolder.setSecurityHeader(securityElementCopy);
//                        break;
//                    }
//                }
            }
        }
    }

    private Element deepCopyElement(final Element element) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document newDocument = builder.newDocument();
            return (Element) newDocument.importNode(element, true);
        } catch (final Exception e) {
            throw new RuntimeException("Error while copying the element", e);
        }
    }
}
