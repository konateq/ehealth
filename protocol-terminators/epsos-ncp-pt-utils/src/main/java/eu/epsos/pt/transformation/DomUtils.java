package eu.epsos.pt.transformation;

import eu.epsos.exceptions.DocumentTransformationException;
import eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DomUtils {

    /**
     * @param document CDA Document passed as byte[]
     * @return CDA Document in XML
     * @throws DocumentTransformationException If the transformation to Document failed.
     */
    public static Document byteToDocument(byte[] document) throws DocumentTransformationException {

        try {
            //Convert document byte array into a String.
            String docString = new String(document, StandardCharsets.UTF_8);
            //Parse the String into a Document object.
            return XMLUtil.parseContent(docString);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new DocumentTransformationException(OpenNCPErrorCode.ERROR_GENERIC, ex.getMessage(), ex.getMessage());
        }
    }
}
