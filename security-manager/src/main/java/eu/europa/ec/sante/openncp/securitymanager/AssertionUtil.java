package eu.europa.ec.sante.openncp.securitymanager;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.saml2.core.*;

import javax.xml.namespace.QName;
import java.util.List;

public class AssertionUtil {

    private AssertionUtil() {
    }

    /**
     * @param subject
     * @return
     */
    public static NameID findProperNameID(Subject subject) {

        String format = subject.getNameID().getFormat();
        NameID nameID = create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        nameID.setFormat(format);
        nameID.setValue(subject.getNameID().getValue());

        return nameID;
    }

    /**
     * @param statements
     * @param attrName
     * @return
     */
    public static Attribute findStringInAttributeStatement(List<AttributeStatement> statements, String attrName) {

        for (AttributeStatement stmt : statements) {

            for (Attribute attribute : stmt.getAttributes()) {

                if (attribute.getName().equals(attrName)) {

                    Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
                    attr.setFriendlyName(attribute.getFriendlyName());
                    attr.setName(attribute.getName());
                    attr.setNameFormat(attribute.getNameFormat());

                    XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
                    XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
                    XSString attrVal = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);

                    if (!attribute.getAttributeValues().get(0).hasChildren()) {
                        attrVal.setValue(((XSString) attribute.getAttributeValues().get(0)).getValue());
                    } else {
                        if (attribute.getAttributeValues().get(0).getOrderedChildren().get(0).getClass().getName().equals("org.opensaml.core.xml.schema.impl.XSAnyImpl")) {
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
    public static Attribute findURIInAttributeStatement(List<AttributeStatement> statements, String attrName) {

        for (AttributeStatement stmt : statements) {
            for (Attribute attribute : stmt.getAttributes()) {
                if (attribute.getName().equals(attrName)) {

                    Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
                    attr.setFriendlyName(attribute.getFriendlyName());
                    attr.setName(attribute.getNameFormat());
                    attr.setNameFormat(attribute.getNameFormat());

                    XMLObjectBuilder uriBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSURI.TYPE_NAME);
                    XSURI attrVal = (XSURI) uriBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSURI.TYPE_NAME);
                    attrVal.setURI(((XSURI) attribute.getAttributeValues().get(0)).getURI());
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
    public static NameID getXspaSubjectFromAttributes(List<AttributeStatement> stmts) {

        var xspaSubjectAttribute = AssertionUtil.findStringInAttributeStatement(stmts, "urn:oasis:names:tc:xspa:1.0:subject:subject-id");
        NameID nameID = create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
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
    public static <T> T create(Class<T> cls, QName qname) {
        return (T) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(qname).buildObject(qname);
    }

    /**
     * @param value
     * @param friendlyName
     * @param nameFormat
     * @param name
     * @return
     */
    public static Attribute createAttribute(String value, String friendlyName, String nameFormat, String name) {

        Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attr.setFriendlyName(friendlyName);
        attr.setName(name);
        attr.setNameFormat(nameFormat);

        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
        XSString attrVal = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrVal.setValue(value);
        attr.getAttributeValues().add(attrVal);
        return attr;
    }

    public static Attribute createAttributePurposeOfUse(String value, String friendlyName, String nameFormat, String name) {

        Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attr.setFriendlyName(friendlyName);
        attr.setName(name);
        attr.setNameFormat(nameFormat);

        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);

        XMLObjectBuilder<XSAny> xsAnyBuilder = (XMLObjectBuilder<XSAny>) builderFactory.getBuilder(XSAny.TYPE_NAME);
        XSAny pou = xsAnyBuilder.buildObject("urn:hl7-org:v3", "PurposeOfUse", "");
        pou.getUnknownAttributes().put(new QName("code"), value);
        pou.getUnknownAttributes().put(new QName("codeSystem"), "3bc18518-d305-46c2-a8d6-94bd59856e9e");
        pou.getUnknownAttributes().put(new QName("codeSystemName"), "eHDSI PurposeofUse");
        pou.getUnknownAttributes().put(new QName("displayName"), value);
        //pou.setTextContent(value);
        XSAny pouAttributeValue = xsAnyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        pouAttributeValue.getUnknownXMLObjects().add(pou);
        attr.getAttributeValues().add(pouAttributeValue);

        XSString attrVal = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrVal.setValue(value);
        attr.getAttributeValues().add(attrVal);
        return attr;
    }
}
