package eu.europa.ec.sante.openncp.common.context;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.immutables.Domain;

@Domain
public interface ServerContext {
    NcpSide getNcpSide();

    ServerMode getServerMode();

    default boolean isProduction() {
        return getServerMode() == ServerMode.PRODUCTION;
    }

    static ServerContext of(NcpSide ncpSide, ServerMode serverMode) {
        return ImmutableServerContext.of(ncpSide, serverMode);
    }
}
