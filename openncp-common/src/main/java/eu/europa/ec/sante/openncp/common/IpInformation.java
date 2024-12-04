package eu.europa.ec.sante.openncp.common;

import eu.europa.ec.sante.openncp.common.immutables.Domain;

import java.util.Optional;

@Domain
public interface IpInformation {
    Optional<String> getRequestIp();

    Optional<String> getHostIp();

    static IpInformation from(final String requestIp, final String hostIp) {
        return ImmutableIpInformation.builder()
                .requestIp(requestIp)
                .hostIp(hostIp)
                .build();
    }

}
