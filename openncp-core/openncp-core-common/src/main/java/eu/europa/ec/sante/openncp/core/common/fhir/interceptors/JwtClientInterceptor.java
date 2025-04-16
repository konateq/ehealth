package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.common.context.ServerContext;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Sets the jwt token on an outgoing request
 */
@Interceptor
@Component
public class JwtClientInterceptor implements IClientInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtClientInterceptor.class);

    private final ServerContext serverContext;

    public JwtClientInterceptor(final ServerContext serverContext) {
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");

    }

    @Override
    public void interceptRequest(final IHttpRequest theRequest) {
        if (serverContext.getNcpSide() == NcpSide.NCP_B) {
            theRequest.addHeader("Authorization", StringEscapeUtils.escapeJava(LogContext.getAuthorization()));
        }
    }

    @Override
    public void interceptResponse(final IHttpResponse theResponse) throws IOException {
    }
}
