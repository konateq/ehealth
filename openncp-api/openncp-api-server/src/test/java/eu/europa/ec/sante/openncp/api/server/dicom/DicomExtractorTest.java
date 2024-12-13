package eu.europa.ec.sante.openncp.api.server.dicom;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;


public class DicomExtractorTest {

    @Test
    public void testExtractDicomStudy() throws URISyntaxException, IOException {
        InputStream dicomKosInputStream = this.getClass().getResourceAsStream("/medicalimages/1000_1010_KOS.dcm");
        DicomExtractor dicomExtractor = new DicomExtractor();
        DicomStudy dicomStudy = dicomExtractor.extractDicomStudy(dicomKosInputStream);
        Assert.assertEquals("1.3.6.1.4.1.14519.5.2.1.7085.2626.192997540292073877946622133586", dicomStudy.getUID());
        List<DicomSeries> dicomSeries = dicomStudy.getSeries();
        Assert.assertEquals(1, dicomSeries.size());
        DicomSeries series = dicomSeries.get(0);
        Assert.assertEquals("1.3.6.1.4.1.14519.5.2.1.7085.2626.328191285537072639441393834220", series.getUID());
        List<DicomInstance> dicomInstances = series.getInstances();
        Assert.assertEquals(11, dicomInstances.size());
        System.out.println(dicomStudy);
    }
}
