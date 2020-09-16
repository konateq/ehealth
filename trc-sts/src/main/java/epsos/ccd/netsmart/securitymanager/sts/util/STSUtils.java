package epsos.ccd.netsmart.securitymanager.sts.util;

import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tr.com.srdc.epsos.util.http.IPUtil;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 *
 */
public class STSUtils {

    public static final String NO_CLIENT_CERTIFICATE = "Unknown (No Client Certificate)";
    private static final Logger LOGGER = LoggerFactory.getLogger(STSUtils.class);
    // TRC Parameters Namespace
    private static final String TRC_NS = "http://epsos.eu/trc";
    private static final String SAML20_TOKEN_URN = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String WS_SEC_UTIL_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String WS_TRUST_NS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";

    private STSUtils() {
    }

    /**
     * @param body
     * @return
     */
    public static String getPurposeOfUse(SOAPElement body) {

        if (body.getElementsByTagNameNS(TRC_NS, "TRCParameters").getLength() < 1) {
            throw new WebServiceException("No TRC Parameters in RST");
        }

        SOAPElement trcDetails = (SOAPElement) body.getElementsByTagNameNS(TRC_NS, "TRCParameters").item(0);
        if (trcDetails.getElementsByTagNameNS(TRC_NS, "PurposeOfUse").item(0) == null) {
            return null;
        }

        String purposeOfUse = trcDetails.getElementsByTagNameNS(TRC_NS, "PurposeOfUse").item(0).getTextContent();
        if (purposeOfUse != null && (!StringUtils.equals("TREATMENT", purposeOfUse) && !StringUtils.equals("EMERGENCY", purposeOfUse))) {
            throw new WebServiceException("Purpose of Use MUST be either TREATMENT of EMERGENCY");
        }
        return purposeOfUse;
    }

    /**
     * @param assertion
     * @return
     */
    public static Document createRSTRC(Document assertion) {

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document respBody = builder.newDocument();

            Element rstrcElem = respBody.createElementNS(WS_TRUST_NS, "wst:RequestSecurityTokenResponseCollection");
            respBody.appendChild(rstrcElem);

            Element rstrElem = respBody.createElementNS(WS_TRUST_NS, "wst:RequestSecurityTokenResponse");
            rstrcElem.appendChild(rstrElem);

            Element rstElem = respBody.createElementNS(WS_TRUST_NS, "wst:RequestedSecurityToken");
            rstrElem.appendChild(rstElem);

            // add the Assertion
            rstElem.appendChild(respBody.importNode(assertion.getDocumentElement(), true));

            Element tokenTypeElem = respBody.createElementNS(WS_TRUST_NS, "wst:TokenType");
            tokenTypeElem.setTextContent(SAML20_TOKEN_URN);

            rstrElem.appendChild(tokenTypeElem);

            Element lifeTimeElem = respBody.createElementNS(WS_TRUST_NS, "wst:LifeTime");
            rstrElem.appendChild(lifeTimeElem);

            DateTime now = new DateTime();

            Element ltCreated = respBody.createElementNS(WS_SEC_UTIL_NS, "wsu:Created");
            ltCreated.setTextContent(now.toDateTime(DateTimeZone.UTC).toString());
            lifeTimeElem.appendChild(ltCreated);

            Element ltExpires = respBody.createElementNS(WS_SEC_UTIL_NS, "wsu:Expires");
            ltExpires.setTextContent(now.plusHours(2).toDateTime(DateTimeZone.UTC).toString());
            lifeTimeElem.appendChild(ltExpires);

            return respBody;

        } catch (Exception ex) {
            LOGGER.error(null, ex);
            throw new WebServiceException("Cannot create RSTSC Message");
        }
    }

    /**
     * @param element
     * @return
     */
    public static String domElementToString(Element element) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException ex) {
            LOGGER.error(null, ex);
            throw new WebServiceException("Error Creating audit message");
        }
    }

    /**
     * @return
     */
    public static String getSTSServerIP() {

        try {
            URL url = new URL(ConfigurationManagerFactory.getConfigurationManager().getProperty("secman.sts.url"));

            InetAddress inetAddress = InetAddress.getByName(url.getHost());
            if (!inetAddress.isLinkLocalAddress() && !inetAddress.isLoopbackAddress()
                    && (inetAddress instanceof Inet4Address)) {
                return inetAddress.getHostAddress();
            } else {
                return IPUtil.getPrivateServerIp();
            }
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            return "UNKNOWN_HOST";
        }
    }

    /**
     * Util method retrieving the Client certificate used (it may work only in a 2ways SSL context).
     *
     * @param messageContext SOAP Message context of the TRC Assertions request
     * @return TLS Common Name of the certificate used during the communication between client and TRC-STS Server
     */
    public static String getSSLCertPeer(MessageContext messageContext) {

        ServletRequest servletRequest = (ServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
        Enumeration<String> servletAttributes = servletRequest.getAttributeNames();
        while (servletAttributes.hasMoreElements()) {
            String attribute = servletAttributes.nextElement();
            LOGGER.info("Servlet Attribute: '{}'", attribute);
        }
        if (servletRequest instanceof HttpServletRequest && servletRequest.isSecure()) {

            LOGGER.info("Secured Channel used for ServletRequest");
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            X509Certificate[] peerCert = (X509Certificate[]) httpServletRequest.getAttribute("javax.servlet.request.X509Certificate");
            if (peerCert != null) {
                return peerCert[0].getSubjectDN().getName();
            }
        } else {

            LOGGER.info("Communication over a not secured channel");
        }
        return NO_CLIENT_CERTIFICATE;
    }
}
