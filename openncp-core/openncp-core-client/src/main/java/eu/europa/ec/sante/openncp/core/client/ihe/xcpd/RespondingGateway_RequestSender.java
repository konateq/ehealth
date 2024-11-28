package eu.europa.ec.sante.openncp.core.client.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerException;
import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201306UV02;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogClientUtil;
import eu.europa.ec.sante.openncp.core.common.util.OidUtil;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

/**
 * RespondingGateway_RequestSender class.
 * <p>
 * Contains the necessary operations to build a XCPD request and to send it to the NCP-A.
 */
public final class RespondingGateway_RequestSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespondingGateway_RequestSender.class);

    private RespondingGateway_RequestSender() {
    }

    /**
     * Builds and sends a PRPA_IN201305UV02 HL7 message, representing an XCPD Request process.
     */
    public static PRPAIN201306UV02 respondingGateway_PRPA_IN201305UV02(final PatientDemographics patientDemographics,
                                                                       final Map<AssertionType, Assertion> assertionMap, final String countryCode)
            throws NoPatientIdDiscoveredException {

        final var dynamicDiscoveryService = new DynamicDiscoveryService();
        String endpointUrl = null;
        try {
            endpointUrl = dynamicDiscoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH),
                                                                 RegisteredService.PATIENT_IDENTIFICATION_SERVICE);
        } catch (final ConfigurationManagerException e) {
            throw new NoPatientIdDiscoveredException(OpenNCPErrorCode.ERROR_PI_NO_MATCH, e);
        }

        final String dstHomeCommunityId = OidUtil.getHomeCommunityId(countryCode.toLowerCase(Locale.ENGLISH));
        final var hl7Request = PRPAIN201305UV022DTS.newInstance(patientDemographics, dstHomeCommunityId);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ClientConnector is trying to contact remote NCP-A:\nEndpoint: '{}'\nHomeCommunityId: '{}'", endpointUrl,
                         dstHomeCommunityId);
        }
        return sendRequest(endpointUrl, hl7Request, assertionMap, countryCode, dstHomeCommunityId);
    }

    private static PRPAIN201306UV02 sendRequest(final String endpointUrl, final PRPAIN201305UV02 pRPAIN201305UV022, final Map<AssertionType, Assertion> assertionMap,
                                                final String countryCode,
                                                final String dstHomeCommunityId) throws NoPatientIdDiscoveredException {

        final var respondingGatewayServiceStub = new RespondingGateway_ServiceStub(endpointUrl);
        // Dummy handler for any mustUnderstand
        EventLogClientUtil.createDummyMustUnderstandHandler(respondingGatewayServiceStub);
        respondingGatewayServiceStub.setCountryCode(countryCode);

        return respondingGatewayServiceStub.respondingGateway_PRPA_IN201305UV02(pRPAIN201305UV022, assertionMap, dstHomeCommunityId);
    }
}
