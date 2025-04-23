package eu.europa.ec.sante.openncp.core.common.fhir.audit.dispatcher;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.europa.ec.sante.openncp.common.context.ServerContext;
import eu.europa.ec.sante.openncp.common.util.FileSystemUtils;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventBuilder;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class FileSystemStorageAuditDispatcher implements AuditDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageAuditDispatcher.class);
    private final FhirContext fhirContext;

    private final ServerContext serverContext;

    private final AuditEventBuilder auditEventBuilder;


    @Value("${EPSOS_PROPS_PATH}")
    private String epsosPropsPath;

    public FileSystemStorageAuditDispatcher(final FhirContext fhirContext, final ServerContext serverContext, final AuditEventBuilder auditEventBuilder) {
        this.fhirContext = Validate.notNull(fhirContext, "fhirContext must not be null");
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");
        this.auditEventBuilder = Validate.notNull(auditEventBuilder, "auditEventBuilder must not be null");
    }

    @Override
    public DispatchResult dispatch(final AuditEventData auditEventData) {
        final AuditEvent hl7AuditEvent = this.auditEventBuilder.build(auditEventData);

        if (LOGGER.isDebugEnabled()) {
            final IParser jsonParser = fhirContext.newJsonParser();
            final String auditEventAsJsonString = jsonParser.encodeResourceToString(hl7AuditEvent);
            LOGGER.debug("Audit event dispatching using dispatcher [{}] for audit event [{}]", this.getClass().getSimpleName(), auditEventAsJsonString);
        }

        final String filename = String.format("fhir_audit_%s_%s%s.json", auditEventData.getAuditResourceType(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC).format(Instant.now()), RandomStringUtils.random(4, true, true));

        FileSystemUtils.createDirIfNotExists(Paths.get(this.epsosPropsPath, "validation", this.serverContext.getNcpSide().getName()).toString());
        final Path file = Paths.get(this.epsosPropsPath, "validation", this.serverContext.getNcpSide().getName(), filename);

            final DispatchMetadata dispatchingMetadata = ImmutableDispatchMetadata.builder()
                    .dispatcherUsed(this.getClass())
                    .dispatchingDestination(file.toString())
                    .build();

        final IParser jsonParser = this.fhirContext.newJsonParser().setPrettyPrint(true);
            final String jsonString = jsonParser.encodeResourceToString(hl7AuditEvent);

            try {
                Files.write(file, jsonString.getBytes());
            } catch (final IOException e) {
                return DispatchResult.failure(dispatchingMetadata, "There was an error writing the audit event to the filesystem.", e);
            }
            return DispatchResult.success(dispatchingMetadata, String.format("Dispatching audit event to filesystem [%s]", file));
    }
}
