package eu.europa.ec.sante.openncp.core.common;

import org.apache.commons.lang3.Validate;

import java.util.Optional;

public class SecurityContextProvider {

    private static final ThreadLocal<SecurityContext> threadLocalSecurityContext = new ThreadLocal<>();

    public static Optional<SecurityContext> getSecurityContext() {
        return Optional.ofNullable(threadLocalSecurityContext.get());
    }

    public static void setAssertionContext(final SecurityContext securityContext) {
        Validate.notNull(securityContext, "assertionContext must not be null");
        threadLocalSecurityContext.set(securityContext);
    }

    public void clear() {
        threadLocalSecurityContext.remove();
    }
}
