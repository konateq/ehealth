package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.ImmutableDispatchContext;
import org.apache.commons.lang3.Validate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractResourceProvider {
    private final ServerContext serverContext;

    public AbstractResourceProvider(ServerContext serverContext) {
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null.");
    }

    protected ServerContext getServerContext() {
        return serverContext;
    }

    protected DispatchContext createDispatchContext(final HttpServletRequest theServletRequest,
                                                    final HttpServletResponse theServletResponse,
                                                    final RequestDetails theRequestDetails) {
        return ImmutableDispatchContext.builder()
                .ncpSide(serverContext.getNcpSide())
                .hapiRequestDetails(theRequestDetails)
                .servletRequest(theServletRequest)
                .servletResponse(theServletResponse)
                .build();
    }
}
