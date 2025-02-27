package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.api.client.RequestContext;
import eu.europa.ec.sante.openncp.api.client.RequestContextProvider;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.*;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.interceptor.security.AuthenticationException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Abstract interception that will validate an assertion present on the request.
 */
public abstract class AbstractAssertionValidationInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAssertionValidationInterceptor.class);
    private final AssertionValidator assertionValidator;

    public AbstractAssertionValidationInterceptor(final AssertionValidator assertionValidator) {
        super(Phase.PRE_INVOKE);
        addAfter(AssertionReportingInterceptor.class.getName());

        this.assertionValidator = Validate.notNull(assertionValidator, "AssertionValidator must not be null");
    }

    public AssertionValidationResult validateAssertion(final Message message, final AssertionType assertionType, final boolean mandatory) {
        LOGGER.info("Validating [{}] Assertion", assertionType.name());

        //Special case
        if ("sayHello".equalsIgnoreCase(message.getExchange().getBindingOperationInfo().getOperationInfo().getName().getLocalPart())) {
            return ImmutableAssertionValidationResult
                    .builder()
                    .status(AssertionValidationStatus.IGNORED)
                    .assertionDetails(Optional.empty())
                    .addValidationDetail(ImmutableAssertionValidationDetail.builder()
                            .key(AssertionValidationKey.OPERATION_TYPE)
                            .status(AssertionValidationDetailStatus.IGNORED)
                            .message("No assertion validation needed for the sayHello operation")
                            .build())
                    .build();
        }

        final RequestContext requestContext = RequestContextProvider.getRequestContext().orElseThrow(() -> new RuntimeException("AssertionContext is null"));
        final SamlDetails samlDetails = requestContext.getSamlDetails();
        final List<AssertionDetails> allAssertions = samlDetails.getAssertions();
        final Optional<AssertionDetails> assertionDetails = samlDetails.getAssertionDetails(assertionType);

        if (assertionDetails.isEmpty()) {
            if (mandatory) {
                return ImmutableAssertionValidationResult
                        .builder()
                        .status(AssertionValidationStatus.FAILED)
                        .assertionDetails(assertionDetails)
                        .addValidationDetail(ImmutableAssertionValidationDetail.builder()
                                .key(AssertionValidationKey.MISSING_ASSERTION)
                                .status(AssertionValidationDetailStatus.FAILED)
                                .message(String.format("Missing [%s] assertion", assertionType.name()))
                                .build())
                        .build();
            } else {
                return ImmutableAssertionValidationResult
                        .builder()
                        .status(AssertionValidationStatus.IGNORED)
                        .assertionDetails(assertionDetails)
                        .addValidationDetail(ImmutableAssertionValidationDetail.builder()
                                .key(AssertionValidationKey.MISSING_ASSERTION)
                                .status(AssertionValidationDetailStatus.IGNORED)
                                .message(String.format("Missing [%s] assertion", assertionType.name()))
                                .build())
                        .build();
            }
        }

        return assertionValidator.validate(assertionDetails.get(), allAssertions)
                .orElseThrow(() -> new AuthenticationException(String.format("No valid validator found for [%s] assertion", assertionType.name())));
    }
}
