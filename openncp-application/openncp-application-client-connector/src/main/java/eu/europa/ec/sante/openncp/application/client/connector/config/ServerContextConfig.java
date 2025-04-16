package eu.europa.ec.sante.openncp.application.client.connector.config;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.context.ServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration("clientConnectorServerContext")
public class ServerContextConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServerContextConfig.class);

    @Bean
    @Primary
    public ServerContext serverContext() {
        return ServerContext.of(NcpSide.CLIENT_CONNECTOR,
                ServerMode.fromValue(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE)));
    }
}
