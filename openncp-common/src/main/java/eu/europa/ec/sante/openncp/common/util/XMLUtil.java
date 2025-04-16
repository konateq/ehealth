package eu.europa.ec.sante.openncp.common.util;

import eu.europa.ec.sante.openncp.common.validation.util.security.CryptographicConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class XMLUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtil.class);
    private static final String HTTP_APACHE_ORG_XML_FEATURES_DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

    private XMLUtil() {
    }

    /**
     * returns null if Node is null
     */
    public static Node extractFromDOMTree(final Node node) throws ParserConfigurationException {

        if (node == null) {
            return null;
        }
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(HTTP_APACHE_ORG_XML_FEATURES_DISALLOW_DOCTYPE_DECL, true);
        documentBuilderFactory.setXIncludeAware(false);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();
        document.appendChild(document.importNode(node, true));
        return document.getDocumentElement();
    }

    /**
     * Gets a DOM document and canonicalize it using OMIT_COMMENTS.
     * <p>
     * Add by massi - 29/12/2016
     *
     * @param document The document to be canonicalized
     * @return the canonicalized document
     * @throws Exception either the document is null, there is no available DOM factory, or a generic c14n error
     */
    public static Document canonicalize(final Document document)
            throws SAXException, IOException, ParserConfigurationException,
                    InvalidCanonicalizerException, CanonicalizationException {

        final Canonicalizer canonicalizer = Canonicalizer.getInstance(CryptographicConstant.ALGO_ID_C14N_INCL_OMIT_COMMENTS);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        canonicalizer.canonicalizeSubtree(document, outputStream);
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(HTTP_APACHE_ORG_XML_FEATURES_DISALLOW_DOCTYPE_DECL, true);
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setNamespaceAware(true);

        return documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(outputStream.toByteArray()));
    }

    public static Document parseContent(final byte[] byteContent)
            throws ParserConfigurationException, SAXException, IOException {

        final String content = new String(byteContent, StandardCharsets.UTF_8);
        return parseContent(content);
    }

    public static Document parseContent(final String content)
            throws ParserConfigurationException, SAXException, IOException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("XMLUtil: parse byte[] content: \n'{}'", content);
        }
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(HTTP_APACHE_ORG_XML_FEATURES_DISALLOW_DOCTYPE_DECL, true);
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setNamespaceAware(true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final StringReader lReader = new StringReader(content);
        final InputSource inputSource = new InputSource(lReader);
        return documentBuilder.parse(inputSource);
    }

    public static byte[] documentToByteArray(final Document doc) throws Exception {
        final TransformerFactory factory = getTransformerFactory();
        final Transformer transformer = factory.newTransformer();
        // Optionally set output properties
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    public static String documentToString(final Document doc) throws TransformerException {
       return documentToString(doc, true);
    }

    public static String documentToString(final Document doc, final boolean omitXmlDeclaration) throws TransformerException {
        final TransformerFactory factory = getTransformerFactory();
        final Transformer transformer = factory.newTransformer();
        final StringWriter writer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString().replaceAll("\n|\r", "");
    }

    public static String prettyPrintForValidation(final Node node) throws TransformerException, XPathExpressionException {

        final XPath xPath = XPathFactory.newInstance().newXPath();
        final NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", node, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); ++i) {
            final Node item = nodeList.item(i);
            item.getParentNode().removeChild(item);
        }

        final StringWriter stringWriter = new StringWriter();

        final TransformerFactory factory = getTransformerFactory();
        final Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());

        transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    private static String unPrettyPrint(final String xml) throws TransformerException {

        if (StringUtils.isBlank(xml)) {
            throw new TransformerException("xml was null or blank in unPrettyPrint()");
        }

        final StringWriter sw;

        try {
            final OutputFormat format = OutputFormat.createCompactFormat();
            final org.dom4j.Document document = DocumentHelper.parseText(xml);
            sw = new StringWriter();
            final XMLWriter writer = new XMLWriter(sw, format);
            writer.write(document);
        } catch (final Exception e) {
            throw new TransformerException("Error un-pretty printing xml:\n" + xml, e);
        }
        return sw.toString();
    }

    /**
     * @param node
     * @return
     * @throws TransformerException
     */
    public static String prettyPrint(final Node node) throws TransformerException {

        final StringWriter stringWriter = new StringWriter();

        final TransformerFactory factory = getTransformerFactory();
        final Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());

        transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    /**
     * @param source
     * @param result
     */
    public static void transformDocument(final DOMSource source, final Result result) throws TransformerException {

        final TransformerFactory factory = getTransformerFactory();
        final Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.transform(source, result);
    }

    /**
     * @param namespaceBindings
     * @return
     */
    public static Map<String, String> parseNamespaceBindings(String namespaceBindings) {

        if (namespaceBindings == null) {
            return Collections.emptyMap();
        }
        namespaceBindings = namespaceBindings.substring(1, namespaceBindings.length() - 1);
        final String[] bindings = namespaceBindings.split(",");
        final Map<String, String> namespaces = new HashMap<>();
        for (final String binding : bindings) {
            final String[] pair = binding.trim().split("=");
            final String prefix = pair[0].trim();
            final String namespace = pair[1].trim();
            namespaces.put(prefix, namespace);
        }
        return namespaces;
    }

    /**
     * @param object
     * @param context
     * @param schemaLocation
     * @return
     */
    public static Document marshall(final Object object, final String context, final String schemaLocation) {

        final Locale oldLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en"));
        try {
            final JAXBContext jc = JAXBContext.newInstance(context);
            final Marshaller marshaller = jc.createMarshaller();
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = schemaFactory.newSchema(new File(schemaLocation));
            marshaller.setSchema(schema);

            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(HTTP_APACHE_ORG_XML_FEATURES_DISALLOW_DOCTYPE_DECL, true);
            dbf.setXIncludeAware(false);
            dbf.setNamespaceAware(true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.newDocument();
            marshaller.marshal(object, doc);
            Locale.setDefault(oldLocale);
            return doc;
        } catch (final Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        Locale.setDefault(oldLocale);
        return null;
    }

    /**
     * @param context
     * @param schemaLocation
     * @param content
     * @return
     */
    public static Object unmarshall(final String context, final String schemaLocation, final String content) {

        final Locale oldLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en"));
        try {
            final JAXBContext jc = JAXBContext.newInstance(context);
            final Unmarshaller unmarshaller = jc.createUnmarshaller();
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = schemaFactory.newSchema(new File(schemaLocation));
            unmarshaller.setSchema(schema);

            final Object obj = unmarshaller.unmarshal(new StringReader(content));
            Locale.setDefault(oldLocale);
            return obj;
        } catch (final Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        Locale.setDefault(oldLocale);
        return null;
    }

    /**
     * @param context
     * @param schemaLocation
     * @param content
     * @return
     */
    public static Object unmarshallWithoutValidation(final String context, final String schemaLocation, final String content) {

        final Locale oldLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en"));
        try {
            final JAXBContext jc = JAXBContext.newInstance(context);
            final Unmarshaller unmarshaller = jc.createUnmarshaller();
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.newSchema(new File(schemaLocation));
            final Object obj = unmarshaller.unmarshal(new StringReader(content));
            Locale.setDefault(oldLocale);
            return obj;
        } catch (final Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        Locale.setDefault(oldLocale);
        return null;
    }

    /**
     * @param in
     * @return
     */
    public static Document newDocumentFromInputStream(final InputStream in) {

        final DocumentBuilderFactory factory;
        final DocumentBuilder builder;
        Document ret = null;

        try {
            factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(HTTP_APACHE_ORG_XML_FEATURES_DISALLOW_DOCTYPE_DECL, true);
            factory.setXIncludeAware(false);
            builder = factory.newDocumentBuilder();
            ret = builder.parse(new InputSource(in));
        } catch (final ParserConfigurationException e) {
            LOGGER.error("ParserConfigurationException: '{}'", e.getMessage(), e);
        } catch (final SAXException | IOException e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }
        return ret;
    }

    public static Node stringToNode(final String xml) throws IOException {

        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
        } catch (final SAXException | ParserConfigurationException e) {
            return null;
        }
    }

    public static List<Node> getNodeList(final Node node, final String xpathexpression) {

        final List<Node> result;
        try {
            final NoNsXpath xpath = new NoNsXpath(xpathexpression);
            result = xpath.selectNodes(node);
        } catch (final JaxenException e) {
            LOGGER.error("xpath: " + xpathexpression + ", node: " + node, e);
            return new ArrayList<>();
        }
        return result;
    }

    private static TransformerFactory getTransformerFactory() throws TransformerException {
        final TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }
}
