package eu.europa.ec.sante.openncp.api.server.dicom;

import eu.europa.ec.sante.openncp.api.server.dicom.DicomParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import org.dcm4che3.data.Tag;


public class DicomParserTest {

    @Test
    public void testExtractAttributeValues() throws URISyntaxException, IOException {
        URL dicomKosUrl = this.getClass().getResource("/medicalimages/1000_1010_KOS.dcm");
        File dicomKosFile = new File(dicomKosUrl.toURI());
        Map<Integer, String> returnedAttributes = DicomParser.extractAttributeValues(dicomKosFile);
        Assert.assertEquals("1.3.6.1.4.1.14519.5.2.1.7085.2626.192997540292073877946622133586", returnedAttributes.get(Tag.StudyInstanceUID));
        Assert.assertEquals("1.3.6.1.4.1.14519.5.2.1.7085.2525.328191285537072638743947497494", returnedAttributes.get(Tag.SeriesInstanceUID));
    }
}
