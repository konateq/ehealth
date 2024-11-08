package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractResourceProvider {
//    private final ServerContext serverContext;
//
//    public AbstractResourceProvider(ServerContext serverContext) {
//        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null.");
//    }

    private final ValidationService validationService;

    public AbstractResourceProvider(ValidationService validationService) {
        this.validationService = Validate.notNull(validationService);
    }

    public void validate(IBaseResource resource, RestOperationTypeEnum restOperationTypeEnum) {
        validationService.validate(resource, restOperationTypeEnum);
    }

    public String getJwtFromRequest(final HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header;
        }
        throw new RuntimeException("JWT Token is missing");
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
