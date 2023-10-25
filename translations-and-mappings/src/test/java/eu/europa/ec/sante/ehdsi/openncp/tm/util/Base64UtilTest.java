package eu.europa.ec.sante.ehdsi.openncp.tm.util;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Base64UtilTest {

    @Test
    public void testEncodingDecoding() {
        var document = buildDom();
        var encodedDocument = Base64Util.encode(document);
        var decodedDocument = Base64Util.decode(encodedDocument);
        Assert.assertEquals(XmlUtil.xmlToString(document), XmlUtil.xmlToString(decodedDocument));
    }

    private Document buildDom() {
        var factory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            DocumentBuilder builder =
                    factory.newDocumentBuilder();
            document = builder.newDocument();
            Element root = document.createElement("rootElement");
            document.appendChild(root);
            root.appendChild(document.createTextNode("node 1"));
            root.appendChild(document.createTextNode("node 2"));
            root.appendChild(document.createTextNode("node 3"));
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }
        return document;
    }
}
