package eu.europa.ec.sante.openncp.core.common;

import eu.europa.ec.sante.openncp.common.NcpSide;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerContextConfig {

    @Bean
    public ServerContext defaultServerContext() {
        return ImmutableServerContext.of(NcpSide.OFFICER);
    }
}
