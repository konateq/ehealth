package eu.europa.ec.sante.openncp.common.context;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("officerServerContextConfig")
public class ServerContextConfig {

    @Bean
    public ServerContext defaultServerContext() {
        return ServerContext.of(NcpSide.OFFICER,
                ServerMode.fromValue(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE)));
    }
}
