package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.api.client.AssertionContext;
import eu.europa.ec.sante.openncp.api.client.AssertionContextProvider;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.XSDValidationException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML2Validator;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.interceptor.security.AuthenticationException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Interceptor that will validate the specific HCP assertion present on the request.
 * This validation must happen for all requests.
 */
@Component
public class HcpAssertionValidationInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HcpAssertionValidationInterceptor.class);
    private final SAML2Validator saml2Validator;

    public HcpAssertionValidationInterceptor(final SAML2Validator saml2Validator) {
        super(Phase.PRE_INVOKE);
        addAfter(AssertionReportingInterceptor.class.getName());

        this.saml2Validator = Validate.notNull(saml2Validator, "saml2Validator must not be null");
    }

    public void handleMessage(final Message message) {
        LOGGER.info("Validating HCP Assertion");

        if ("sayHello".equalsIgnoreCase(message.getExchange().getBindingOperationInfo().getOperationInfo().getName().getLocalPart())) {
            LOGGER.info("The sayHello operation should not validate the HCP Assertion");
            return;
        }

        final AssertionContext assertionContext = AssertionContextProvider.getAssertionContext().orElseThrow(() -> new RuntimeException("AssertionContext is null"));
        final SamlDetails samlDetails = assertionContext.getSamlDetails();
        final AssertionDetails hcpAssertionDetails = samlDetails.getHcpAssertion().orElseThrow(() -> new AuthenticationException("A HCP assertion is mandatory."));

        try {
            saml2Validator.validateHCPHeader(hcpAssertionDetails.getAssertion());
        } catch (final MissingFieldException | InsufficientRightsException | InvalidFieldException | SMgrException |
                       XSDValidationException e) {
            throw new AuthenticationException(String.format("Invalid HCP assertion: Cause [%s] with message [%s]", e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    public void handleFault(final Message messageParam) {
        //empty
    }
}
