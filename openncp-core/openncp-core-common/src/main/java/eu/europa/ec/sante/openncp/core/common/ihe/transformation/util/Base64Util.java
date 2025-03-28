package eu.europa.ec.sante.openncp.core.common.ihe.transformation.util;

import org.w3c.dom.Document;

import java.util.Base64;

public class Base64Util {

    public static String encode(final Document document) {
            return document != null ? Base64.getEncoder().encodeToString(XmlUtil.doc2bytes(document)) : null;
    }

    public static Document decode(final String base64EncodedString) {
        return base64EncodedString != null ? XmlUtil.bytesToXml(Base64.getDecoder().decode(base64EncodedString), Boolean.TRUE) : null;
    }
}
