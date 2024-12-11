package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import eu.europa.ec.sante.openncp.api.common.StreamingResponseBodyUtils;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.dicom.DicomDispatchingService;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.services.ValidationService;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@RestController
@RequestMapping("/dicom")
public class DicomResourceProvider extends AbstractResourceProvider {
    private final DicomDispatchingService dicomDispatchingService;

    public DicomResourceProvider(final DicomDispatchingService dicomDispatchingService, final ServerContext serverContext, final ValidationService validationService) {
        super(serverContext, validationService);
        this.dicomDispatchingService = Validate.notNull(dicomDispatchingService, "dicomDispatchingService must not be null");
    }

    @GetMapping(value = "/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}", produces = {"application/dicom"})
    public byte[] getDicomFile(@PathVariable("studyUID") final String studyUID,
                               @PathVariable("seriesUID") final String seriesUID,
                               @PathVariable("instanceUID") final String instanceUID,
                               final HttpServletRequest servletRequest,
                               final HttpServletResponse servletResponse) {
        final DispatchContext dispatchContext = createDispatchContext(servletRequest, servletResponse);
        return dicomDispatchingService.dispatchDicomFile(dispatchContext, studyUID, seriesUID, instanceUID);
    }

    @GetMapping(value = "/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}/metadata", produces = {"application/dicom+json"})
    public String getDicomMetadata(@PathVariable("studyUID") final String studyUID,
                                   @PathVariable("seriesUID") final String seriesUID,
                                   @PathVariable("instanceUID") final String instanceUID,
                                   final HttpServletRequest servletRequest,
                                   final HttpServletResponse servletResponse) {
        final DispatchContext dispatchContext = createDispatchContext(servletRequest, servletResponse);
        return dicomDispatchingService.dispatchDicomMetadata(dispatchContext, studyUID, seriesUID, instanceUID);
    }

    @GetMapping(value = "/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}/frames/{frameNumber}",
            consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE},
            produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public byte[] getDicomRenderedImage(@PathVariable("studyUID") final String studyUID,
                                        @PathVariable("seriesUID") final String seriesUID,
                                        @PathVariable("instanceUID") final String instanceUID,
                                        @PathVariable("frameNumber") final String frameNumber,
                                        final HttpServletRequest servletRequest,
                                        final HttpServletResponse servletResponse) {
        final DispatchContext dispatchContext = createDispatchContext(servletRequest, servletResponse);
        return dicomDispatchingService.dispatchDicomRenderedImage(dispatchContext, studyUID, seriesUID, instanceUID, frameNumber);
    }

    @GetMapping(value = "/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}/frames/{frameNumber}",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> getPixelData(
            @PathVariable("studyUID") final String studyUID,
            @PathVariable("seriesUID") final String seriesUID,
            @PathVariable("instanceUID") final String instanceUID,
            @PathVariable("frameNumber") final String frameNumber,
            final HttpServletRequest servletRequest,
            final HttpServletResponse servletResponse) {
        final DispatchContext dispatchContext = createDispatchContext(servletRequest, servletResponse);
        final InputStream pixelData = dicomDispatchingService.dispatchDicomPixelData(dispatchContext, studyUID, seriesUID, instanceUID, frameNumber);

        try {
            final StreamingResponseBody responseBody = StreamingResponseBodyUtils.createFromInputStream(pixelData);
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", String.format("frame_%s_%s.raw", instanceUID, frameNumber));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(responseBody);
        } catch (final Exception ex) {
            final StreamingResponseBody errorResponseBody = StreamingResponseBodyUtils
                    .createErrorResponse("Error while streaming the dicom pixel data", ex);
            return ResponseEntity.internalServerError()
                    .body(errorResponseBody);
        }
    }

    @GetMapping(value = "/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}/bulkdata/{bulkDataID}",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> getBulkData(
            @PathVariable("studyUID") final String studyUID,
            @PathVariable("seriesUID") final String seriesUID,
            @PathVariable("instanceUID") final String instanceUID,
            @PathVariable("bulkDataID") final String bulkDataID,
            final HttpServletRequest servletRequest,
            final HttpServletResponse servletResponse) {
        final DispatchContext dispatchContext = createDispatchContext(servletRequest, servletResponse);
        final InputStream bulkDataStream = dicomDispatchingService.dispatchDicomBulkData(dispatchContext, studyUID, seriesUID, instanceUID, bulkDataID);

        try {
            final StreamingResponseBody responseBody = StreamingResponseBodyUtils.createFromInputStream(bulkDataStream);
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", String.format("bulkdata-%s.bin", bulkDataID));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(responseBody);
        } catch (final Exception ex) {
            final StreamingResponseBody errorResponseBody = StreamingResponseBodyUtils
                    .createErrorResponse("Error while streaming the dicom bulkdata", ex);
            return ResponseEntity.internalServerError()
                    .body(errorResponseBody);
        }
    }
}
