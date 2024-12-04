package eu.europa.ec.sante.openncp.api.common.handler;


import eu.europa.ec.sante.openncp.core.common.fhir.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.fhir.transformation.transcodings.TranscodingService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TranscodeBundleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscodeBundleHandler.class);

    private final TranscodingService transcodingService;

    public TranscodeBundleHandler(final TranscodingService transcodingService) {
        this.transcodingService = Validate.notNull(transcodingService);
    }

    public Bundle handle(final Bundle bundle) {
        LOGGER.info("Transcoding FHIR bundle from national format to MyHealth@EU format");
        final TMResponseStructure transcodedBundle = transcodingService.transcode(bundle);
        return Validate.notNull(transcodedBundle.getFhirDocument());
    }
}
