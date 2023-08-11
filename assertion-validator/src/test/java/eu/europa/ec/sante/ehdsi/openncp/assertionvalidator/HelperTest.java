package eu.europa.ec.sante.ehdsi.openncp.assertionvalidator;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class HelperTest {

    @Test
    public void testGetRoleId() throws ParserConfigurationException, IOException, SAXException {
        var classLoader = getClass().getClassLoader();
        var samlAssertion = new File(classLoader.getResource("samlAssertion.xml").getFile());
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        var db = dbf.newDocumentBuilder();
        var doc = db.parse(samlAssertion);
        var roleId = Helper.getRoleID(doc.getDocumentElement());
        Assert.assertEquals("Medical doctors", roleId);
    }
}
