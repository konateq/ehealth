package eu.europa.ec.sante.openncp.api.server.dicom;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;


public class DicomExtractorTest {

    @Test
    public void testExtractDicomStudy() throws IOException {
        InputStream dicomKosInputStream = this.getClass().getResourceAsStream("/medicalimages/dicomKos.dcm");
        DicomExtractor dicomExtractor = new DicomExtractor();
        DicomStudy dicomStudy = dicomExtractor.extractDicomStudy(dicomKosInputStream);
        Assert.assertEquals("1.2.276.0.7230010.3.1.2.296485376.1.1521713414.1800996", dicomStudy.getUID());
        Assert.assertEquals("KO", dicomStudy.getModality());
        Assert.assertEquals("CT ABDOMEN/PELVIS W AND W/O IV CONTRAST", dicomStudy.getStudyDescription());
        Assert.assertEquals("2794663908550664", dicomStudy.getAccessionNumber());
        List<DicomSeries> dicomSeries = dicomStudy.getSeries();
        Assert.assertEquals(1, dicomSeries.size());
        DicomSeries series = dicomSeries.get(0);
        Assert.assertEquals("1.2.276.0.7230010.3.1.3.296485376.1.1521713419.1802493", series.getUID());
        Assert.assertEquals("ABD ROUTINE  3.0  B31f", series.getDescription());
        Assert.assertEquals("https://imageStorage.vna.org/wadoRS/studies/1.3.6.1.4.1.14519.5.2.1.7085.2626.192997540292073877946622133586/series/1.3.6.1.4.1.14519.5.2.1.7085.2626.328191285537072639441393834220", series.getEndpoint());
        Assert.assertEquals("CT", series.getModality());
        List<DicomInstance> dicomInstances = series.getInstances();
        Assert.assertEquals(11, dicomInstances.size());
    }
}
