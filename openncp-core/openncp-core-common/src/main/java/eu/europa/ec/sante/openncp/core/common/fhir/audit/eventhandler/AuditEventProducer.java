package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;

import java.util.List;

public interface AuditEventProducer {
    boolean accepts(AuditableEvent auditableEvent);

    List<AuditEventData> produce(AuditableEvent auditableEvent);
}
