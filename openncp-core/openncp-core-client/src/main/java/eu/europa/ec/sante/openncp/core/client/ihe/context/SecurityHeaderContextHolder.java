package eu.europa.ec.sante.openncp.core.client.ihe.context;

import org.apache.cxf.headers.Header;
import org.w3c.dom.Element;

public class SecurityHeaderContextHolder {
    private static final ThreadLocal<Element> securityHeaderThreadLocal = new ThreadLocal<>();

    public static void setSecurityHeader(final Element securityHeader) {
        securityHeaderThreadLocal.set(securityHeader);
    }

    public static Element getSecurityHeader() {
        return securityHeaderThreadLocal.get();
    }

    public static void clear() {
        securityHeaderThreadLocal.remove();
    }
}
