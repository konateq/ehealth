package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditEventData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.ImmutableMetaData;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.ImmutableParticipantData;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class AbstractAuditEventProducer {

    protected List<AuditEventData.ParticipantData> createParticipants() {

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        AuditSecurityInfo auditSecurityInfo = (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();

        final AuditEventData.ParticipantData serviceConsumer = ImmutableParticipantData.builder()
                .id(usernamePasswordAuthenticationToken.getName())
                .roleCode(auditSecurityInfo.getSamlDetails().getHcpAssertionDetails()
                        .map(AssertionDetails::getElement)
                        .map(SoapElementHelper::getRoleID)
                        .orElse(StringUtils.EMPTY))
                .requestor(false)
                .network(auditSecurityInfo.getRequestIp())
                .build();

        final AuditEventData.ParticipantData serviceProvider = ImmutableParticipantData.builder()
                .id((String) usernamePasswordAuthenticationToken.getCredentials())
                .roleCode("provider role unknown")
                .requestor(true)
                .network(auditSecurityInfo.getHostIp())
                .build();

        return List.of(serviceConsumer, serviceProvider);
    }

    protected AuditEventData.MetaData createMetaData(final AuditableEvent auditableEvent) {
        return ImmutableMetaData.builder()
                .recordDateTime(auditableEvent.getTimestamp())
                .correlationId(LogContext.getCorrelationId())
                .build();
    }
}
