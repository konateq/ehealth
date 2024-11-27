package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.common.IpInformation;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import org.hl7.fhir.r4.model.AuditEvent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.*;

import java.util.List;

public interface AuditEventProducer {

    boolean accepts(AuditableEvent auditableEvent);

    List<AuditEvent> produce(AuditableEvent auditableEvent);

    default List<AuditEventData.ParticipantData> createParticipants() {
        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final AuditSecurityInfo auditSecurityInfo =
                (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();

        return List.of(
                createEventserviceConsumer(usernamePasswordAuthenticationToken, auditSecurityInfo),
                createEventserviceProvider(usernamePasswordAuthenticationToken)
        );
    }

    default AuditEventData.ParticipantData createEventserviceConsumer(
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken,
            AuditSecurityInfo auditSecurityInfo) {

        return ImmutableParticipantData.builder()
                .id(usernamePasswordAuthenticationToken.getName())
                .roleCode(SoapElementHelper.getRoleID(auditSecurityInfo.getSamlAsRoot()))
                .requestor(false)
                .network(LogContext.getIpInformation().flatMap(IpInformation::getRequestIp).orElse(null))
                .build();
    }

    default AuditEventData.ParticipantData createEventserviceProvider(
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {

        return ImmutableParticipantData.builder()
                .id((String) usernamePasswordAuthenticationToken.getCredentials())
                .roleCode("provider role unknown")
                .requestor(true)
                .network(LogContext.getIpInformation().flatMap(IpInformation::getHostIp).orElse(null))
                .build();
    }
}


