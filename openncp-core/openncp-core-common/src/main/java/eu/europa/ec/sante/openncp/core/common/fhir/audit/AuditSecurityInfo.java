package eu.europa.ec.sante.openncp.core.common.fhir.audit;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.common.fhir.security.SamlDetails;

@Domain
public interface AuditSecurityInfo {

    SamlDetails getSamlDetails();

    String getRequestIp();

    String getHostIp();

    static AuditSecurityInfo from(final SamlDetails samlDetails, final String requestIp, final String hostIp) {
        return ImmutableAuditSecurityInfo.builder()
                .samlDetails(samlDetails)
                .requestIp(requestIp)
                .hostIp(hostIp)
                .build();
    }

}
