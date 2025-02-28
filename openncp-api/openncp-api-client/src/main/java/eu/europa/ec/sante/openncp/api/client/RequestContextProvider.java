package eu.europa.ec.sante.openncp.api.client;

import org.apache.commons.lang3.Validate;

import java.util.Optional;

public class RequestContextProvider {

    private static final ThreadLocal<RequestContext> threadLocalRequestContext = new ThreadLocal<>();

    public static Optional<RequestContext> getRequestContext() {
        return Optional.ofNullable(threadLocalRequestContext.get());
    }

    public static void replaceRequestContext(final RequestContext requestContext) {
        Validate.notNull(requestContext, "request context must not be null");
        threadLocalRequestContext.set(requestContext);
    }

    public static void unload() {
        threadLocalRequestContext.remove();
    }
}
