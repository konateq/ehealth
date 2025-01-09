package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.ImmutableDispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.services.ValidationService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractResourceProvider {
    private final ServerContext serverContext;
    private final ValidationService validationService;

    public AbstractResourceProvider(final ServerContext serverContext, final ValidationService validationService) {
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null.");
        this.validationService = Validate.notNull(validationService, "validationService must not be null");
    }

    public void validate(final IBaseResource resource, final RestOperationTypeEnum restOperationTypeEnum) {
        validationService.validate(resource, restOperationTypeEnum);
    }

    protected ServerContext getServerContext() {
        return serverContext;
    }

    protected DispatchContext createDispatchContext(final HttpServletRequest theServletRequest,
                                                    final HttpServletResponse theServletResponse,
                                                    final RequestDetails theRequestDetails) {
        return ImmutableDispatchContext.builder()
                .ncpSide(serverContext.getNcpSide())
                .servletRequest(theServletRequest)
                .servletResponse(theServletResponse)
                .hapiRequestDetails(theRequestDetails)
                .build();
    }

    protected DispatchContext createDispatchContext(final HttpServletRequest theServletRequest,
                                                    final HttpServletResponse theServletResponse) {
        return ImmutableDispatchContext.builder()
                .ncpSide(serverContext.getNcpSide())
                .servletRequest(theServletRequest)
                .servletResponse(theServletResponse)
                .build();
    }
}
