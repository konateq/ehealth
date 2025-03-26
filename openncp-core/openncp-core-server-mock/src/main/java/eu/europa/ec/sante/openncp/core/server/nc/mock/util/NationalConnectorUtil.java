package eu.europa.ec.sante.openncp.core.server.nc.mock.util;

import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.common.xml.SAMLSchemaBuilder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.impl.AssertionMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Validator;
import java.io.IOException;

public class NationalConnectorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(NationalConnectorUtil.class);

    private NationalConnectorUtil() {
    }

    private static Assertion getAssertionFromSOAPHeader(final Element soapHeader, final String type) {

        final NodeList securityList = soapHeader.getElementsByTagNameNS(
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
        if (securityList != null && securityList.getLength() > 0) {

            final Element security = (Element) securityList.item(0);
            final NodeList assertionList = security.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion");

            if (assertionList.getLength() > 0) {
                for (int i = 0; i < assertionList.getLength(); i++) {
                    try {
                        final Element assertionElement = (Element) assertionList.item(i);
                        // Validate Assertion according to SAML XSD
                        final SAMLSchemaBuilder schemaBuilder = new SAMLSchemaBuilder(SAMLSchemaBuilder.SAML1Version.SAML_11);
                        final Validator validator = schemaBuilder.getSAMLSchema().newValidator();
                        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                        validator.validate(new DOMSource(assertionElement));
                        final Assertion assertion = (Assertion) SAML.fromElement(assertionElement);
                        if (StringUtils.equals(type, assertion.getIssuer().getNameQualifier())) {
                            return assertion;
                        }
                    } catch (final SAXException | IOException | UnmarshallingException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public static String getDocumentAsXml(final Document document, final boolean header) {

        var response = "";
        try {
            final DOMSource domSource = new DOMSource(document);
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final Transformer transformer = transformerFactory.newTransformer();
            final String omit;
            if (header) {
                omit = "no";
            } else {
                omit = "yes";
            }
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omit);
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            final var stringWriter = new java.io.StringWriter();
            final StreamResult sr = new StreamResult(stringWriter);
            transformer.transform(domSource, sr);
            response = stringWriter.toString();
        } catch (final Exception e) {
            LOGGER.error(ExceptionUtils.getStackTrace(e));
        }
        return response;
    }

    /**
     * Returns HCP assertion received by NCP-A and passed to National Connector.
     *
     * @param soapHeader - SOAP Header message received by National Connector
     * @return HCP Assertion passed to National Connector.
     */
    public static Assertion getHCPAssertionFromSOAPHeader(final Element soapHeader) {
        return getAssertionFromSOAPHeader(soapHeader, "urn:ehdsi:assertions:hcp");
    }

    /**
     * Returns NoK assertion received by NCP-A and passed to National Connector.
     *
     * @param soapHeader - SOAP Header message received by National Connector
     * @return Next of Kin Assertion passed to National Connector.
     */
    public static Assertion getNoKAssertionFromSOAPHeader(final Element soapHeader) {
        return getAssertionFromSOAPHeader(soapHeader, "urn:ehdsi:assertions:nok");
    }

    /**
     * Returns TRC assertion received by NCP-A and passed to National Connector.
     *
     * @param soapHeader - SOAP Header message received by National Connector
     * @return TRC Assertion passed to National Connector.
     */
    public static Assertion getTRCAssertionFromSOAPHeader(final Element soapHeader) {
        return getAssertionFromSOAPHeader(soapHeader, "urn:ehdsi:assertions:trc");
    }

    public static void logAssertionAsXml(final Assertion assertion) {

        try {
            final var marshaller = new AssertionMarshaller();
            final Element element = marshaller.marshall(assertion);
            final Document document = element.getOwnerDocument();
            LOGGER.info("Assertion: '{}'\n'{}'", assertion.getID(), getDocumentAsXml(document, false));
        } catch (final MarshallingException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
