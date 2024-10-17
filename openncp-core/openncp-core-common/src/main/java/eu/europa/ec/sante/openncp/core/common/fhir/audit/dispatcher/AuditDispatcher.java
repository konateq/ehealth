package eu.europa.ec.sante.openncp.core.common.fhir.audit.dispatcher;

import org.hl7.fhir.r4.model.AuditEvent;

public interface AuditDispatcher {
    DispatchResult dispatch(AuditEvent auditEvent, String resourceType);
}
