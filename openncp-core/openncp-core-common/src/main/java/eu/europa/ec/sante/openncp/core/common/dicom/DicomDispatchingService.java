package eu.europa.ec.sante.openncp.core.common.dicom;

import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;

import java.io.InputStream;

public interface DicomDispatchingService {

    byte[] dispatchDicomFile(DispatchContext dispatchContext, String studyUID, String seriesUID, String instanceUID);

    String dispatchDicomMetadata(DispatchContext dispatchContext, String studyUID, String seriesUID, String instanceUID);

    byte[] dispatchDicomRenderedImage(DispatchContext dispatchContext, String studyUID, String seriesUID, String instanceUID, String frameNumber);

    InputStream dispatchDicomPixelData(DispatchContext dispatchContext, String studyId, String seriesId, String instanceId, String frameNumber);

    InputStream dispatchDicomBulkData(DispatchContext dispatchContext, String studyUID, String seriesUID, String instanceUID, String bulkDataID);
}
