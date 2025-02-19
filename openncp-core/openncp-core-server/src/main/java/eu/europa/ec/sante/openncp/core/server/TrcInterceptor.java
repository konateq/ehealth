package eu.europa.ec.sante.openncp.core.server;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.ImmutableDispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.interceptors.FhirCustomInterceptor;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML2Validator;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Interceptor
@Component
public class TrcInterceptor implements FhirCustomInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrcInterceptor.class);

    private final SAML2Validator saml2Validator;
    private final ServerContext serverContext;


    public TrcInterceptor(final SAML2Validator saml2Validator, final ServerContext serverContext) {
        this.saml2Validator = Validate.notNull(saml2Validator, "saml2Validator must not be null");
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void validateTrcAssertionIfApplicable(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails, final RestOperationTypeEnum restOperationTypeEnum) {
        final DispatchContext dispatchContext = ImmutableDispatchContext.builder()
                .ncpSide(serverContext.getNcpSide())
                .servletRequest(servletRequestDetails.getServletRequest())
                .servletResponse(servletRequestDetails.getServletResponse())
                .hapiRequestDetails(theRequestDetails)
                .build();
        if (dispatchContext.isPatient() || dispatchContext.getRestOperationType() == RestOperationTypeEnum.METADATA) {
            LOGGER.debug("The request was regarding a patient resource or a METADATA request, skipping TRC assertion verification");
            return; //no TRC check for patient resources or metadata requests
        }


        final EuRequestDetails euRequestDetails = EuRequestDetails.of(theRequestDetails);
        if (euRequestDetails.isPatient() || euRequestDetails.getRestOperationType() == RestOperationTypeEnum.METADATA) {
            LOGGER.debug("The request was regarding a patient resource or a METADATA request, skipping TRC assertion verification");
            return; //no TRC check for patient resources or metadata requests
        }

        LOGGER.info("Validating TRC assertion for resource: {}", euRequestDetails.getResourceType());
        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final AuditSecurityInfo auditSecurityInfo = (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();

        final Assertion trcAssertion = auditSecurityInfo.getSamlDetails().getAssertion(AssertionType.TRC)
                .map(AssertionDetails::getAssertion)
                .orElseThrow(() -> new AuthenticationException("No TRC assertion found"));

        try {
            saml2Validator.checkTRCAssertion(trcAssertion, ClassCode.LABORATORY_RESULT_REPORTS);
        } catch (final MissingFieldException | InvalidFieldException | InsufficientRightsException | SMgrException e) {
            throw new AuthenticationException(String.format("Invalid TRC assertion for server context [%s]", serverContext), e);
        }
    }
}
