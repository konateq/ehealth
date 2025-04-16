package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.SecurityContext;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidationResult;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.interceptor.security.AuthenticationException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract interception that will validate an assertion present on the request.
 */
@Component
public class AssertionValidationInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionValidationInterceptor.class);
    private final AssertionValidator assertionValidator;

    public AssertionValidationInterceptor(final AssertionValidator assertionValidator) {
        super(Phase.PRE_INVOKE);
        addAfter(AssertionReportingInterceptor.class.getName());

        this.assertionValidator = Validate.notNull(assertionValidator, "AssertionValidator must not be null");
    }

    @Override
    public void handleMessage(final Message message) {
        LOGGER.info("Validating Assertions");

        //Special case
        if ("sayHello".equalsIgnoreCase(message.getExchange().getBindingOperationInfo().getOperationInfo().getName().getLocalPart())) {
            LOGGER.info("No assertion validation needed for the sayHello operation");
            return;
        }

        final SecurityContext securityContext = SecurityContextProvider.getSecurityContext().orElseThrow(() -> new RuntimeException("AssertionContext is null"));
        final SamlDetails samlDetails = securityContext.getSamlDetails().orElseThrow(() -> new RuntimeException("SamlDetails is null"));
        samlDetails.getAssertionDetails(AssertionType.HCP).orElseThrow(() -> new AuthenticationException("No HCP assertion found"));

        final List<AssertionValidationResult> validationResults = assertionValidator.validate(samlDetails.getAssertions());
        final List<String> failedValidationMessages = validationResults.stream().flatMap(assertionValidationResult -> assertionValidationResult.getFailedValidationMessages().stream()).collect(Collectors.toList());
        if (!failedValidationMessages.isEmpty()) {
            throw new AuthenticationException(String.format("Validation failed for assertions with the following details: %s", StringUtils.join(failedValidationMessages, "\n")));
        }
    }
}
