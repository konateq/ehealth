package eu.europa.ec.sante.openncp.core.server.config;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.core.common.ImmutableServerContext;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerContextConfiguration {

    @Bean
    public ServerContext ncpSide() {
        return ImmutableServerContext.builder()
                .ncpSide(NcpSide.NCP_A)
                .build();
    }
}
