package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.api.client.AssertionContext;
import eu.europa.ec.sante.openncp.api.client.AssertionContextProvider;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.assertion.AssertionValidationResult;
import eu.europa.ec.sante.openncp.core.common.assertion.AssertionValidator;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.interceptor.security.AuthenticationException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Interceptor that will validate the specific HCP assertion present on the request.
 * This validation must happen for all requests.
 */
@Component
public class HcpAssertionValidationInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HcpAssertionValidationInterceptor.class);
    private final AssertionValidator assertionValidator;

    public HcpAssertionValidationInterceptor(final AssertionValidator assertionValidator) {
        super(Phase.PRE_INVOKE);
        addAfter(AssertionReportingInterceptor.class.getName());

        this.assertionValidator = Validate.notNull(assertionValidator, "AssertionValidator must not be null");
    }

    public void handleMessage(final Message message) {
        LOGGER.info("Validating HCP Assertion");

        if ("sayHello".equalsIgnoreCase(message.getExchange().getBindingOperationInfo().getOperationInfo().getName().getLocalPart())) {
            LOGGER.info("The sayHello operation should not validate the HCP Assertion");
            return;
        }

        final AssertionContext assertionContext = AssertionContextProvider.getAssertionContext().orElseThrow(() -> new RuntimeException("AssertionContext is null"));
        final SamlDetails samlDetails = assertionContext.getSamlDetails();
        final AssertionDetails hcpAssertionDetails = samlDetails.getHcpAssertion()
                .orElseThrow(() -> new AuthenticationException("A HCP assertion is mandatory."));

        final AssertionValidationResult assertionValidationResult = assertionValidator.validate(hcpAssertionDetails)
                .orElseThrow(() -> new AuthenticationException("No valid validator found for HCP assertion"));

        final List<String> failedValidationMessages = assertionValidationResult.getFailedValidationMessages();
        if (!failedValidationMessages.isEmpty()) {
            throw new AuthenticationException(String.format("Invalid HCP assertion: [%s] ", failedValidationMessages));
        }
    }

    public void handleFault(final Message messageParam) {
        //empty
    }
}
