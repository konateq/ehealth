package eu.europa.ec.sante.openncp.core.server.nc.mock.dicom;

import eu.europa.ec.sante.openncp.core.common.dicom.DicomDispatchingService;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class MockDicomDispatchingService implements DicomDispatchingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockDicomDispatchingService.class);

    public MockDicomDispatchingService() {
    }

    @Override
    public byte[] dispatchDicomFile(DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID) {
        //FIXME call the rest interface of the dicom server
        return "dicom file goes here".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String dispatchDicomMetadata(DispatchContext dispatchContext, final String studyUID, final String seriesUID) {
        //FIXME call the rest interface of the dicom server
        //call the rest interface of NCP-A
        return "dicom metadata goes here";
    }

    @Override
    public byte[] dispatchDicomRenderedImage(DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID, final String frameNumber) {
        //FIXME call the rest interface of the dicom server
        return "dicom rendered image goes here".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public InputStream dispatchDicomPixelData(DispatchContext dispatchContext, final String studyId, final String seriesId, final String instanceId, final String frameNumber) {
        //FIXME call the rest interface of the dicom server
        return new ByteArrayInputStream("dicom pixel data image goes here".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream dispatchDicomBulkData(DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID, final String bulkDataID) {
        //FIXME call the rest interface of the dicom server
        return new ByteArrayInputStream("dicom bulk data goes here".getBytes(StandardCharsets.UTF_8));
    }
}
