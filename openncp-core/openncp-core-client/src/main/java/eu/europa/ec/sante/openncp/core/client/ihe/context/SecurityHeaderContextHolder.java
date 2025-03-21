package eu.europa.ec.sante.openncp.core.client.ihe.context;

import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import org.apache.cxf.headers.Header;
import org.opensaml.saml.saml2.core.Assertion;
import org.w3c.dom.Element;

import java.util.List;

public class SecurityHeaderContextHolder {
    private static final ThreadLocal<Element> securityHeaderThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Header> headerThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<List<Assertion>> assertionThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<SamlDetails> samlDetailsThreadLocal = new ThreadLocal<>();

    public static void setSecurityHeader(final Element securityHeader) {
        securityHeaderThreadLocal.set(securityHeader);
    }

    public static void setAllAssertions(final List<Assertion> assertions) {
        assertionThreadLocal.set(assertions);
    }

    public static void setSamlDetails(final SamlDetails samlDetails) {
        samlDetailsThreadLocal.set(samlDetails);
    }

    public static SamlDetails getSamlDetails() {
        return samlDetailsThreadLocal.get();
    }

    public static List<Assertion> getAllAssertions() {
        return assertionThreadLocal.get();
    }

    public static void setSecurityHeader(final Header securityHeader) {
        headerThreadLocal.set(securityHeader);
    }

    public static Element getSecurityHeader() {
        return securityHeaderThreadLocal.get();
    }

    public static Header getHeader() {
        return headerThreadLocal.get();
    }


    public static void clear() {
        securityHeaderThreadLocal.remove();
        headerThreadLocal.remove();
        assertionThreadLocal.remove();
        samlDetailsThreadLocal.remove();
    }
}
