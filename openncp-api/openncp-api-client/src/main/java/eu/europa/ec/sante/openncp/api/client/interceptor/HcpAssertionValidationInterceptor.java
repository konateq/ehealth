package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidationResult;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidator;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.interceptor.security.AuthenticationException;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Interceptor that will validate the specific HCP assertion present on the request.
 * This validation must happen for all requests.
 */
@Component
public class HcpAssertionValidationInterceptor extends AbstractAssertionValidationInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HcpAssertionValidationInterceptor.class);

    public HcpAssertionValidationInterceptor(final AssertionValidator assertionValidator) {
        super(Validate.notNull(assertionValidator, "AssertionValidator must not be null"));
    }

    @Override
    public void handleMessage(final Message message) {
        LOGGER.info("Validating HCP Assertion");

        final AssertionValidationResult assertionValidationResult = validateAssertion(message, AssertionType.HCP, true);
        final List<String> failedValidationMessages = assertionValidationResult.getFailedValidationMessages();
        if (!failedValidationMessages.isEmpty()) {
            throw new AuthenticationException(String.format("Invalid HCP assertion: [%s] ", failedValidationMessages));
        }

    }

    @Override
    public void handleFault(final Message messageParam) {
        //empty
    }
}
