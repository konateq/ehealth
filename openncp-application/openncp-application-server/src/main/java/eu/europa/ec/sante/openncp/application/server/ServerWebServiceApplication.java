package eu.europa.ec.sante.openncp.application.server;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.core.common.ImmutableServerContext;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;


@SpringBootApplication(scanBasePackages = {"eu.europa.ec.sante.openncp"})
public class ServerWebServiceApplication extends SpringBootServletInitializer {

    @Bean
    @Primary
    public ServerContext serverContext() {
        return ImmutableServerContext.of(NcpSide.NCP_A);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerWebServiceApplication.class);


    public static void main(final String... args) {
        SpringApplication.run(eu.europa.ec.sante.openncp.application.server.ServerWebServiceApplication.class, args);

        final String serverMode = System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE);
        LOGGER.info("Server Mode: '{}'", StringUtils.isNotBlank(serverMode) ? serverMode : "N/A");
    }
}