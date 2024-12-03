package eu.europa.ec.sante.openncp.common.context;

import eu.europa.ec.sante.openncp.common.IpInformation;
import eu.europa.ec.sante.openncp.common.util.UUIDHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

/**
 * Higher level wrapper around the MDC. This wrapper makes sure to keep certain contexts in sync.
 */
public final class LogContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogContext.class);

    private LogContext() {
    }

    /**
     * Update the correlationId in the MDC.
     */
    public static void setCorrelationId() {
        final String correlationId = UUID.randomUUID().toString();
        setCorrelationId(correlationId);
    }

    public static String getCorrelationId() {
        return MDC.get("correlationId");
    }

    /**
     * Update the correlationId in the MDC. If the correlationId is not valid, an exception is thrown.
     *
     * @param correlationId the new correlation id
     */
    public static void setCorrelationId(final String correlationId) {
        if (UUIDHelper.isUUIDValid(correlationId)) {
            final String existingCorrelationId = getCorrelationId();
            if (existingCorrelationId != null) {
                LOGGER.info("Changing correlation id of log context from [{}] to [{}]", existingCorrelationId, correlationId);
            } else {
                LOGGER.info("Setting correlation id [{}]", correlationId);
            }
            MDC.put("correlationId", correlationId);
        } else {
            throw new IllegalArgumentException("Correlation Id must be a valid UUID. " +
                    StringUtils.length(correlationId));
        }
    }

    /**
     * Clear the correlationId in the MDC.
     */
    public static void clearCorrelationId() {
        MDC.remove("correlationId");
    }

    /**
     * Update the errorId in the MDC. If the errorId is not valid, an exception is thrown.
     *
     * @param errorId the new error id
     */
    public static void setErrorId(final String errorId) {
        if (StringUtils.isNotBlank(errorId) && errorId.length() <= 36) {
            MDC.put("errorId", errorId);
        } else {
            throw new IllegalArgumentException("Error Id does not respect the format limitations: length is " + StringUtils.length(errorId));
        }
    }

    /**
     * Clear the errorId in the MDC.
     */
    public static void clearErrorId() {
        MDC.remove("errorId");
    }

    // CHECKSTYLE.OFF: AbbreviationAsWordInNameCheck
    public static void clear() {
        // CHECKSTYLE.ON: AbbreviationAsWordInNameCheck
        MDC.remove("correlationId");
        MDC.remove("errorId");
        MDC.remove("CountryCode");
        MDC.remove("ipInformationHostIp");
        MDC.remove("ipInformationRequestIp");
        MDC.remove("authorization");
    }

    public static void setCountryCode(final String countryCode) {
        MDC.put("CountryCode", countryCode);
    }

    public static String getCountryCode() {
        return MDC.get("CountryCode");
    }

    public static void setAuthorization(final String authorization) {
        MDC.put("authorization", authorization);
    }

    public static String getAuthorization() {
        return MDC.get("authorization");
    }

    public static void setIpInformation(final IpInformation ipInformation) {
        ipInformation.getRequestIp().ifPresent(requestIp -> MDC.put("ipInformationRequestIp", requestIp));
        ipInformation.getHostIp().ifPresent(hostIp -> MDC.put("ipInformationHostIp", hostIp));
    }

    public static Optional<IpInformation> getIpInformation() {
        if (StringUtils.isNotBlank(MDC.get("ipInformationHostIp")) || StringUtils.isNotBlank(MDC.get("ipInformationRequestIp"))) {
            return Optional.of(IpInformation.from(MDC.get("ipInformationHostIp"), MDC.get("ipInformationRequestIp")));
        }

        return Optional.empty();
    }
}
