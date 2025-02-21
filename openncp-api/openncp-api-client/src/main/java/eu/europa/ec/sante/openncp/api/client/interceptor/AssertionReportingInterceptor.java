package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.api.client.RequestContext;
import eu.europa.ec.sante.openncp.api.client.RequestContextProvider;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor that will report all the assertions present on the request if needed by the configuration.
 */
public class AssertionReportingInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionReportingInterceptor.class);

    public AssertionReportingInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    public void handleMessage(final Message message) {
        if (!GazelleValidation.isValidationEnable()) {
            LOGGER.info("OpenNCP validation is disabled, not reporting any assertions.");
            return;
        }

        LOGGER.info("Reporting assertions");
        final RequestContext requestContext = RequestContextProvider.getRequestContext().orElseThrow(() -> new RuntimeException("AssertionContext is null"));
        final SamlDetails samlDetails = requestContext.getSamlDetails();

        samlDetails.getAssertionDetails(AssertionType.HCP)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> GazelleValidation.logAndValidateHCPAssertion(assertion, NcpSide.NCP_B));
        samlDetails.getAssertionDetails(AssertionType.TRC)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> GazelleValidation.validateTRCAssertion(assertion, NcpSide.NCP_B));
        samlDetails.getAssertionDetails(AssertionType.NOK)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> GazelleValidation.validateNoKAssertion(assertion, NcpSide.NCP_B));
    }

    public void handleFault(final Message messageParam) {
        //empty
    }
}
