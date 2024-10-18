package eu.europa.ec.sante.openncp.core.common.fhir.audit.dispatcher;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AtnaAuditDispatcher implements AuditDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtnaAuditDispatcher.class);
    private final FhirContext fhirContext;

    public AtnaAuditDispatcher(final FhirContext fhirContext) {
        this.fhirContext = Validate.notNull(fhirContext, "fhirContext must not be null");
    }

    @Override
    public DispatchResult dispatch(final AuditEvent auditEvent, String resourceType) {
        final DispatchMetadata dispatchingMetadata = ImmutableDispatchMetadata.builder()
                .dispatcherUsed(this.getClass())
                .dispatchingDestination("Atna server url goes here")
                .build();
        return DispatchResult.failure(dispatchingMetadata, "ATNA dispatching not implemented yet.");
    }
}
