package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import org.hl7.fhir.r4.model.AuditEvent;

import java.util.List;

public interface AuditEventProducer {

    boolean accepts(AuditableEvent auditableEvent);

    List<AuditEvent> produce(AuditableEvent auditableEvent);
}


