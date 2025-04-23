package eu.europa.ec.sante.openncp.core.common.fhir.audit.dispatcher;

import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;

public interface AuditDispatcher {
    DispatchResult dispatch(AuditEventData auditEventData);
}
