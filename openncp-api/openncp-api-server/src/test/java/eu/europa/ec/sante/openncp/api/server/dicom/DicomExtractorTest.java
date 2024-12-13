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
        List<DicomSeries> dicomSeries = dicomStudy.getSeries();
        Assert.assertEquals(1, dicomSeries.size());
        DicomSeries series = dicomSeries.get(0);
        Assert.assertEquals("1.2.276.0.7230010.3.1.3.296485376.1.1521713419.1802493", series.getUID());
        List<DicomInstance> dicomInstances = series.getInstances();
        Assert.assertEquals(11, dicomInstances.size());
    }
}
