package eu.europa.ec.sante.openncp.core.server.nc.mock.dicom;

import eu.europa.ec.sante.openncp.core.common.dicom.DicomDispatchingService;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class MockDicomDispatchingService implements DicomDispatchingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockDicomDispatchingService.class);
    private final OrthancRestClient orthancRestClient;

    public MockDicomDispatchingService(final OrthancRestClient orthancRestClient) {
        this.orthancRestClient = Validate.notNull(orthancRestClient, "orthancRestClient must not be null");
    }

    @Override
    public byte[] dispatchDicomFile(final DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID) {
        return orthancRestClient.downloadDicomFile(studyUID, seriesUID, instanceUID);
    }

    @Override
    public String dispatchDicomMetadata(final DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID) {
        return orthancRestClient.downloadDicomMetadata(studyUID, seriesUID, instanceUID);
    }

    @Override
    public byte[] dispatchDicomRenderedImage(final DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID, final String frameNumber) {
        return orthancRestClient.downloadDicomRenderedImage(studyUID, seriesUID, instanceUID, frameNumber);
    }

    @Override
    public InputStream dispatchDicomPixelData(final DispatchContext dispatchContext, final String studyId, final String seriesId, final String instanceId, final String frameNumber) {
        //FIXME call the rest interface of the dicom server
        return new ByteArrayInputStream("dicom pixel data image goes here".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream dispatchDicomBulkData(final DispatchContext dispatchContext, final String studyUID, final String seriesUID, final String instanceUID, final String bulkDataID) {
        //FIXME call the rest interface of the dicom server
        return new ByteArrayInputStream("dicom bulk data goes here".getBytes(StandardCharsets.UTF_8));
    }
}
