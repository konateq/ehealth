package eu.europa.ec.sante.openncp.common.security.util;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.saml2.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssertionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionUtil.class);

    private AssertionUtil() {
    }

    /**
     * @param subject
     * @return
     */
    public static NameID findProperNameID(final Subject subject) {

        final String format = subject.getNameID().getFormat();
        final NameID nameID = create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        nameID.setFormat(format);
        nameID.setValue(subject.getNameID().getValue());

        return nameID;
    }

    /**
     * @param statements
     * @param attrName
     * @return
     */
    public static Attribute findStringInAttributeStatement(final List<AttributeStatement> statements, final String attrName) {

        for (final AttributeStatement stmt : statements) {

            for (final Attribute attribute : stmt.getAttributes()) {

                if (attribute.getName().equals(attrName)) {

                    final Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
                    attr.setFriendlyName(attribute.getFriendlyName());
                    attr.setName(attribute.getName());
                    attr.setNameFormat(attribute.getNameFormat());

                    final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
                    final XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
                    final XSString attrVal = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);

                    if (!attribute.getAttributeValues().get(0).hasChildren()) {
                        attrVal.setValue(((XSString) attribute.getAttributeValues().get(0)).getValue());
                    } else {
                        if (attribute.getAttributeValues()
                                .get(0)
                                .getOrderedChildren()
                                .get(0)
                                .getClass()
                                .getName()
                                .equals("org.opensaml.core.xml.schema.impl.XSAnyImpl")) {
                            attrVal.setValue(((XSAny) attribute.getAttributeValues().get(0).getOrderedChildren().get(0)).getTextContent());
                        } else {
                            attrVal.setValue(((XSString) attribute.getAttributeValues().get(0).getOrderedChildren().get(0)).getValue());
                        }
                    }
                    attr.getAttributeValues().add(attrVal);

                    return attr;
                }
            }
        }
        return null;
    }

    /**
     * @param statements
     * @param attrName
     * @return
     */
    public static Attribute findURIInAttributeStatement(final List<AttributeStatement> statements, final String attrName) {

        for (final AttributeStatement stmt : statements) {
            for (final Attribute attribute : stmt.getAttributes()) {
                if (attribute.getName().equals(attrName)) {

                    final Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
                    attr.setFriendlyName(attribute.getFriendlyName());
                    attr.setName(attribute.getNameFormat());
                    attr.setNameFormat(attribute.getNameFormat());

                    final XMLObjectBuilder uriBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSURI.TYPE_NAME);
                    final XSURI attrVal = (XSURI) uriBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSURI.TYPE_NAME);
                    attrVal.setValue(((XSURI) attribute.getAttributeValues().get(0)).getValue());
                    attr.getAttributeValues().add(attrVal);

                    return attr;
                }
            }
        }
        return null;
    }

    /**
     * @param stmts
     * @return
     */
    public static NameID getXspaSubjectFromAttributes(final List<AttributeStatement> stmts) {

        final var xspaSubjectAttribute = AssertionUtil.findStringInAttributeStatement(stmts, "urn:oasis:names:tc:xspa:1.0:subject:subject-id");
        final NameID nameID = create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        nameID.setFormat(NameID.UNSPECIFIED);
        nameID.setValue(((XSString) xspaSubjectAttribute.getAttributeValues().get(0)).getValue());

        return nameID;
    }

    /**
     * Helper Function that makes it easy to create a new OpenSAML Object, using the default namespace prefixes.
     *
     * @param <T>   The Type of OpenSAML Class that will be created
     * @param cls   the openSAML Class
     * @param qname The Qname of the Represented XML element.
     * @return the new OpenSAML object of type T
     */
    public static <T> T create(final Class<T> cls, final QName qname) {
        return (T) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(qname).buildObject(qname);
    }

    /**
     * @param value
     * @param friendlyName
     * @param nameFormat
     * @param name
     * @return
     */
    public static Attribute createAttribute(final String value, final String friendlyName, final String nameFormat, final String name) {

        final Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attr.setFriendlyName(friendlyName);
        attr.setName(name);
        attr.setNameFormat(nameFormat);

        final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
        final XSString attrVal = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrVal.setValue(value);
        attr.getAttributeValues().add(attrVal);
        return attr;
    }

    public static Attribute createAttributePurposeOfUse(final String value, final String friendlyName, final String nameFormat, final String name) {

        final Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attr.setFriendlyName(friendlyName);
        attr.setName(name);
        attr.setNameFormat(nameFormat);

        final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);

        final XMLObjectBuilder<XSAny> xsAnyBuilder = (XMLObjectBuilder<XSAny>) builderFactory.getBuilder(XSAny.TYPE_NAME);
        final XSAny pou = xsAnyBuilder.buildObject("urn:hl7-org:v3", "PurposeOfUse", "");
        pou.getUnknownAttributes().put(new QName("code"), value);
        pou.getUnknownAttributes().put(new QName("codeSystem"), "3bc18518-d305-46c2-a8d6-94bd59856e9e");
        pou.getUnknownAttributes().put(new QName("codeSystemName"), "eHDSI XSPA PurposeOfUse");
        pou.getUnknownAttributes().put(new QName("displayName"), value);

        final XSAny pouAttributeValue = xsAnyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        pouAttributeValue.getUnknownXMLObjects().add(pou);
        attr.getAttributeValues().add(pouAttributeValue);

        final XSString attrVal = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrVal.setValue(value);
        attr.getAttributeValues().add(attrVal);
        return attr;
    }

    public static List<Assertion> toAssertions(final NodeList assertionList)  {
        final List<Assertion> assertions = new ArrayList<>();

        for (int i = 0; i < assertionList.getLength(); i++) {
            final Node node = assertionList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                final Element assertionElement = (Element) node;
                toAssertion(assertionElement).ifPresent(assertions::add);
            }
        }

        return assertions;
    }

    public static Optional<Assertion> toAssertion(final Element element) {
        // Unmarshalling using the document root element, an EntitiesDescriptor in this case
        try {

            final Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(element);
            if (unmarshaller == null) {
                LOGGER.error("SAML Unmarshalling is NULL");
                return Optional.empty();
            }
            return Optional.of((Assertion) unmarshaller.unmarshall(element));
        } catch (final UnmarshallingException e) {
            LOGGER.error("UnmarshallingException: '{}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public static Optional<Element> toElement(final Assertion assertion, final Document document) {
        try {
            final Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion);
            if (marshaller == null) {
                LOGGER.error("SAML marshaller is NULL");
                return Optional.empty();
            }
            marshaller.marshall(assertion, document);
            return Optional.of(document.getDocumentElement());
        } catch (final MarshallingException e) {
            LOGGER.error("UnmarshallingException: '{}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
