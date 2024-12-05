package eu.europa.ec.sante.openncp.core.common.fhir.audit;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.dispatcher.AuditDispatcher;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.dispatcher.DispatchResult;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler.AuditEventProducer;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler.AuditableEvent;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler.FallbackAuditEventProducer;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler.ImmutableAuditableEvent;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.ImmutableDispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.interceptors.FhirCustomInterceptor;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Interceptor(order = 5)
@Component
@Order(Integer.MAX_VALUE)
public class AuditInterceptor implements FhirCustomInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditInterceptor.class);
    private final FhirContext fhirContext;
    private final List<AuditEventProducer> auditEventProducers;
    private final FallbackAuditEventProducer fallbackAuditEventProducer;
    private final List<AuditDispatcher> auditDispatchers;
    private final ServerContext serverContext;

    public AuditInterceptor(final FhirContext fhirContext, final List<AuditEventProducer> auditEventProducers, final FallbackAuditEventProducer fallbackAuditEventProducer, final List<AuditDispatcher> auditDispatchers, final ServerContext serverContext) {
        this.fhirContext = Validate.notNull(fhirContext, "fhirContext cannot be null.");
        this.auditEventProducers = Validate.notNull(auditEventProducers, "auditEventProducers cannot be null.");
        this.fallbackAuditEventProducer = Validate.notNull(fallbackAuditEventProducer, "fallbackAuditEventProducer cannot be null.");
        this.auditDispatchers = Validate.notNull(auditDispatchers, "auditDispatchers cannot be null.");
        this.serverContext = Validate.notNull(serverContext, "serverContext cannot be null.");
    }

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public void auditOutgoingResponse(final RequestDetails requestDetails,
                                      final ServletRequestDetails servletRequestDetails,
                                      final IBaseResource baseResource,
                                      final ResponseDetails responseDetails,
                                      final HttpServletRequest httpServletRequest,
                                      final HttpServletResponse httpServletResponse) {
        final DispatchContext dispatchContext = ImmutableDispatchContext.builder()
                .ncpSide(serverContext.getNcpSide())
                .hapiRequestDetails(requestDetails)
                .servletRequest(httpServletRequest)
                .servletResponse(httpServletResponse).build();
        final AuditableEvent auditableEvent = ImmutableAuditableEvent.builder()
                .pointcut(Pointcut.SERVER_OUTGOING_RESPONSE)
                .fhirContext(fhirContext)
                .dispatchContext(dispatchContext)
                .resource(baseResource)
                .build();
        final List<AuditEvent> auditEvents = auditEventProducers.stream()
                .filter(auditEventProducer -> auditEventProducer.accepts(auditableEvent))
                .findFirst()
                .map(auditEventProducer -> auditEventProducer.produce(auditableEvent))
                .orElseGet(() -> fallbackAuditEventProducer.produce(auditableEvent));

        auditEvents.forEach(auditEvent -> auditDispatchers.forEach(auditDispatcher -> {
            if (LOGGER.isDebugEnabled()) {
                final IParser jsonParser = fhirContext.newJsonParser();
                final String auditEventAsJsonString = jsonParser.encodeResourceToString(auditEvent);
                LOGGER.debug("Audit event dispatching using dispatcher [{}] for audit event [{}]", auditDispatcher.getClass().getSimpleName(), auditEventAsJsonString);
            }

            final DispatchResult dispatchResult = auditDispatcher.dispatch(auditEvent, dispatchContext.getSpecificResourceType());
            LOGGER.debug("Audit event dispatched with result [{}]", dispatchResult);
            if (dispatchResult.isSuccess()) {
                LOGGER.info("Audit event successfully dispatched: [{}]", dispatchResult.getMessage());
            } else {
                final DispatchResult.DispatchError dispatchError = dispatchResult.getError();
                final String errorMessage = String.format("Dispatching the audit event FAILED: [%s]", dispatchError.getErrorMessage());
                dispatchError.getThrowable().ifPresentOrElse(throwable -> LOGGER.error(errorMessage, throwable), () -> LOGGER.error(errorMessage));
            }
        }));
    }
}
