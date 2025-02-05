package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.common.IpInformation;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.ImmutableParticipantData;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.AuditEvent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
                createEventServiceConsumer(usernamePasswordAuthenticationToken, auditSecurityInfo),
                createEventServiceProvider(usernamePasswordAuthenticationToken)
        );
    }

    default AuditEventData.ParticipantData createEventServiceConsumer(
            final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken,
            final AuditSecurityInfo auditSecurityInfo) {

        return ImmutableParticipantData.builder()
                .id(usernamePasswordAuthenticationToken.getName())
                .roleCode(auditSecurityInfo.getSamlDetails().getHcpAssertion()
                        .map(AssertionDetails::getElement)
                        .map(SoapElementHelper::getRoleID)
                        .orElse(StringUtils.EMPTY))
                .requestor(false)
                .network(LogContext.getIpInformation().flatMap(IpInformation::getRequestIp))
                .build();
    }

    default AuditEventData.ParticipantData createEventServiceProvider(
            final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {

        return ImmutableParticipantData.builder()
                .id((String) usernamePasswordAuthenticationToken.getCredentials())
                .roleCode("provider role unknown")
                .requestor(true)
                .network(LogContext.getIpInformation().flatMap(IpInformation::getHostIp))
                .build();
    }
}


