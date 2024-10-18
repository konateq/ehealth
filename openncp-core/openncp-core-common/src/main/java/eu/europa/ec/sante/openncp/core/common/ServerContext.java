package eu.europa.ec.sante.openncp.core.common;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.immutables.Domain;

@Domain
public interface ServerContext {
    NcpSide getNcpSide();
}