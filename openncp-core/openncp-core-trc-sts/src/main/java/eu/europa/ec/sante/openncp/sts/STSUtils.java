package eu.europa.ec.sante.openncp.sts;

import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerFactory;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 *
 */
public class STSUtils {
    public static final String NO_CLIENT_CERTIFICATE = "Unknown (No Client Certificate)";
    public static final String NOK_NS = "https://ehdsi.eu/assertion/nok";
    public static final String TRC_NS = "https://ehdsi.eu/assertion/trc";

    private static final Logger LOGGER = LoggerFactory.getLogger(STSUtils.class);
    private static final String SAML20_TOKEN_URN = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String WS_SEC_UTIL_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String WS_TRUST_NS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";

    private STSUtils() {
    }

    public static String getDispensationPinCode(final SOAPElement body) {

        if (body.getElementsByTagNameNS(TRC_NS, "TRCParameters").getLength() < 1) {
            throw new WebServiceException("No TRC Parameters in RST");
        }

        final SOAPElement trcDetails = (SOAPElement) body.getElementsByTagNameNS(TRC_NS, "TRCParameters").item(0);
        if (trcDetails.getElementsByTagNameNS(TRC_NS, "DispensationPinCode").item(0) == null) {
            return StringUtils.EMPTY;
        }

        return trcDetails.getElementsByTagNameNS(TRC_NS, "DispensationPinCode").item(0).getTextContent();
    }

    public static NextOfKinDetail getNextOfKinDetails(final SOAPElement body) throws ParseException {

        LOGGER.info("Processing Next Of Kin details from STS SOAP Request");
        if (body.getElementsByTagNameNS(NOK_NS, "NoKParameters").getLength() < 1) {
            throw new WebServiceException("No NoK Parameters in RST");
        }
        final var nextOfKinDetail = new NextOfKinDetail();
        final SOAPElement trcDetails = (SOAPElement) body.getElementsByTagNameNS(NOK_NS, "NoKParameters").item(0);

        if (trcDetails.getElementsByTagNameNS(NOK_NS, "PatientId").item(0) != null) {
            nextOfKinDetail.setPatientId(trcDetails.getElementsByTagNameNS(NOK_NS, "PatientId").item(0).getTextContent());
        }


        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinId").item(0) != null) {
            nextOfKinDetail.setLivingSubjectId(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinId").item(0).getTextContent());
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinFamilyName").item(0) != null) {
            nextOfKinDetail.setFamilyName(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinFamilyName").item(0).getTextContent());
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinFirstName").item(0) != null) {
            nextOfKinDetail.setFirstName(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinFirstName").item(0).getTextContent());
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinFamilyName").item(0) != null) {
            nextOfKinDetail.setFamilyName(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinFamilyName").item(0).getTextContent());
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinGender").item(0) != null) {
            nextOfKinDetail.setGender(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinGender").item(0).getTextContent());
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinBirthDate").item(0) != null) {
            final var formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            final var birthDate = formatter.parse(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinBirthDate").item(0).getTextContent());
            nextOfKinDetail.setBirthDate(birthDate);
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinAddressStreet").item(0) != null) {
            nextOfKinDetail.setAddressStreet(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinAddressStreet").item(0).getTextContent());
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinAddressCity").item(0) != null) {
            nextOfKinDetail.setAddressCity(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinAddressCity").item(0).getTextContent());
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinPostalCode").item(0) != null) {
            nextOfKinDetail.setAddressPostalCode(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinPostalCode").item(0).getTextContent());
        }
        if (trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinAddressCountry").item(0) != null) {
            nextOfKinDetail.setAddressCountry(trcDetails.getElementsByTagNameNS(NOK_NS, "NextOfKinAddressCountry").item(0).getTextContent());
        }
        return nextOfKinDetail;
    }

    public static String getPrescriptionId(final SOAPElement body) {

        if (body.getElementsByTagNameNS(TRC_NS, "TRCParameters").getLength() < 1) {
            throw new WebServiceException("No TRC Parameters in RST");
        }

        final SOAPElement trcDetails = (SOAPElement) body.getElementsByTagNameNS(TRC_NS, "TRCParameters").item(0);
        if (trcDetails.getElementsByTagNameNS(TRC_NS, "PrescriptionId").item(0) == null) {
            return StringUtils.EMPTY;
        }

        return trcDetails.getElementsByTagNameNS(TRC_NS, "PrescriptionId").item(0).getTextContent();
    }

    public static String getPurposeOfUse(final SOAPElement body, final String namespace, final String localParametersName) {

        if (body.getElementsByTagNameNS(namespace, localParametersName).getLength() < 1) {
            throw new WebServiceException(String.format("No local parameters name [%s] found in request", localParametersName));
        }

        final SOAPElement trcDetails = (SOAPElement) body.getElementsByTagNameNS(namespace, localParametersName).item(0);
        if (trcDetails.getElementsByTagNameNS(namespace, "PurposeOfUse").item(0) == null) {
            return null;
        }

        final String purposeOfUse = trcDetails.getElementsByTagNameNS(namespace, "PurposeOfUse").item(0).getTextContent();
        if (purposeOfUse != null && (!StringUtils.equals("TREATMENT", purposeOfUse) && !StringUtils.equals("EMERGENCY", purposeOfUse))) {
            throw new WebServiceException("Purpose of Use MUST be either TREATMENT of EMERGENCY");
        }
        return purposeOfUse;
    }

    public static Document createRSTRC(final Document assertion) {

        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document respBody = builder.newDocument();

            final Element rstrcElem = respBody.createElementNS(WS_TRUST_NS, "wst:RequestSecurityTokenResponseCollection");
            respBody.appendChild(rstrcElem);

            final Element rstrElem = respBody.createElementNS(WS_TRUST_NS, "wst:RequestSecurityTokenResponse");
            rstrcElem.appendChild(rstrElem);

            final Element rstElem = respBody.createElementNS(WS_TRUST_NS, "wst:RequestedSecurityToken");
            rstrElem.appendChild(rstElem);

            // add the Assertion
            rstElem.appendChild(respBody.importNode(assertion.getDocumentElement(), true));

            final Element tokenTypeElem = respBody.createElementNS(WS_TRUST_NS, "wst:TokenType");
            tokenTypeElem.setTextContent(SAML20_TOKEN_URN);

            rstrElem.appendChild(tokenTypeElem);

            final Element lifeTimeElem = respBody.createElementNS(WS_TRUST_NS, "wst:LifeTime");
            rstrElem.appendChild(lifeTimeElem);

            final var now = new DateTime();

            final Element ltCreated = respBody.createElementNS(WS_SEC_UTIL_NS, "wsu:Created");
            ltCreated.setTextContent(now.toDateTime(DateTimeZone.UTC).toString());
            lifeTimeElem.appendChild(ltCreated);

            final Element ltExpires = respBody.createElementNS(WS_SEC_UTIL_NS, "wsu:Expires");
            ltExpires.setTextContent(now.plusHours(2).toDateTime(DateTimeZone.UTC).toString());
            lifeTimeElem.appendChild(ltExpires);

            return respBody;

        } catch (final Exception ex) {
            LOGGER.error(null, ex);
            throw new WebServiceException("Cannot create RSTSC Message");
        }
    }

    public static String domElementToString(final Element element) {
        try {
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final Transformer transformer = transformerFactory.newTransformer();
            final var stringWriter = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (final TransformerException ex) {
            LOGGER.error(null, ex);
            throw new WebServiceException("Error Creating audit message");
        }
    }

    public static String getSTSServerIP() {

        try {
            final var url = new URL(ConfigurationManagerFactory.getConfigurationManager().getProperty("secman.sts.url"));
            final var inetAddress = InetAddress.getByName(url.getHost());
            if (!inetAddress.isLinkLocalAddress() && !inetAddress.isLoopbackAddress()
                    && (inetAddress instanceof Inet4Address)) {
                return inetAddress.getHostAddress();
            } else {
                return IPUtil.getPrivateServerIp();
            }
        } catch (final Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            return "UNKNOWN_HOST";
        }
    }
}
