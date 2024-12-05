package eu.europa.ec.sante.openncp.core.common.fhir.audit.dispatcher;

import ca.uhn.fhir.context.FhirContext;
import eu.europa.ec.sante.openncp.common.audit.AuditService;
import eu.europa.ec.sante.openncp.common.audit.utils.SerializableMessage;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditMessageAdapter;
import net.RFC3881.dicom.AuditMessage;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AtnaAuditDispatcher implements AuditDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtnaAuditDispatcher.class);
    private final FhirContext fhirContext;
    private final AuditService atnaAuditService;
    private final AuditMessageAdapter auditMessageAdapter;

    public AtnaAuditDispatcher(final FhirContext fhirContext, final AuditService atnaAuditService, final AuditMessageAdapter auditMessageAdapter) {
        this.fhirContext = Validate.notNull(fhirContext, "fhirContext must not be null");
        this.atnaAuditService = Validate.notNull(atnaAuditService, "atnaAuditService must not be null");
        this.auditMessageAdapter = Validate.notNull(auditMessageAdapter, "auditMessageAdapter must not be null");
    }

    @Override
    public DispatchResult dispatch(final AuditEvent auditEvent, final String resourceType) {
        final AuditMessage iheAuditMessage = auditMessageAdapter.convertFhirAuditEventToIHEAuditMessage(auditEvent);
        final SerializableMessage serializableMessage = new SerializableMessage(iheAuditMessage, "13", "2");

        final DispatchMetadata dispatchingMetadata = ImmutableDispatchMetadata.builder()
                .dispatcherUsed(this.getClass())
                .dispatchingDestination("")
                .build();

        try {
            if (this.atnaAuditService.handleMessage(serializableMessage)) {
                return DispatchResult.success(dispatchingMetadata);
            } else {
                return DispatchResult.failure(dispatchingMetadata, "The handling of the message returned false");
            }
        } catch (final Exception e) {
            return DispatchResult.failure(dispatchingMetadata, e);
        }
    }
}
