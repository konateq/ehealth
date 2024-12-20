package eu.europa.ec.sante.openncp.api.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.sante.openncp.api.common.handler.ResourceHandler;
import eu.europa.ec.sante.openncp.api.server.dicom.DicomExtractor;
import eu.europa.ec.sante.openncp.api.server.dicom.DicomStudy;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.ApplicationMediaType;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DocumentReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class DicomExtractionHandler implements ResourceHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DicomExtractionHandler.class);

    private final DicomExtractor dicomExtractor;

    private final ObjectMapper objectMapper;

    public DicomExtractionHandler(final DicomExtractor dicomExtractor, final ObjectMapper objectMapper) {
        this.dicomExtractor = Validate.notNull(dicomExtractor, "dicomExtractor cannot be null");
        this.objectMapper = Validate.notNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    public boolean accepts(final DispatchContext dispatchContext, final IBaseResource resource) {
        return dispatchContext != null && resource != null && dispatchContext.isDocumentReference();
    }

    @Override
    public void handle(final DispatchContext dispatchContext, final IBaseResource resource) {
        final DocumentReference documentReference = (DocumentReference) resource;
        Assert.notNull(documentReference, "documentReference cannot be null");
        List<DocumentReference.DocumentReferenceContentComponent> documentReferenceContentComponentsForDicomStudy = new ArrayList<>();
        for (final DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent : documentReference.getContent()) {
            if (documentReferenceContentComponent.getAttachment().getContentType().equals(ApplicationMediaType.APPLICATION_DICOM.value())) {
                final byte[] base64EncodedDicomFile = documentReferenceContentComponent.getAttachment().getDataElement().getValueAsString().getBytes(StandardCharsets.UTF_8);
                final byte[] data = Base64.getDecoder().decode(base64EncodedDicomFile);
                final InputStream inputStream = new ByteArrayInputStream(data);
                try {
                    final DicomStudy dicomStudy = this.dicomExtractor.extractDicomStudy(inputStream);
                    final String dicomStudyAsJson = this.objectMapper.writeValueAsString(dicomStudy);
                    final DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponentForDicomStudy = new DocumentReference.DocumentReferenceContentComponent();
                    final Attachment attachment = new Attachment();
                    attachment.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    attachment.setData(dicomStudyAsJson.getBytes(StandardCharsets.UTF_8));
                    documentReferenceContentComponentForDicomStudy.setAttachment(attachment);
                    documentReferenceContentComponentsForDicomStudy.add(documentReferenceContentComponentForDicomStudy);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        documentReferenceContentComponentsForDicomStudy.forEach(documentReference::addContent);
    }
}
