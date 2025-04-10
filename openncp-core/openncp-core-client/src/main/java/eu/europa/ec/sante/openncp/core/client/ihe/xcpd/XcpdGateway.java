package eu.europa.ec.sante.openncp.core.client.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerException;
import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.ihe.IhePortTypeFactory;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.OpenNCPException;
import eu.europa.ec.sante.openncp.core.common.util.OidUtil;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201306UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * XCPD Gateway
 * This is an implementation of a IHE XCPD Gateway. This class provides the necessary operations to perform
 * PatientDiscovery.
 */
@Service
public class XcpdGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(XcpdGateway.class);

    private final DynamicDiscoveryService discoveryService;
    private final ConfigurationManager configurationManager;
    private final IhePortTypeFactory ihePortTypeFactory;

    public XcpdGateway(final DynamicDiscoveryService discoveryService, final ConfigurationManager configurationManager, final IhePortTypeFactory ihePortTypeFactory) {
        this.discoveryService = Validate.notNull(discoveryService, "discoveryService must not be null");
        this.configurationManager = Validate.notNull(configurationManager, "configurationManager must not be null");
        this.ihePortTypeFactory = Validate.notNull(ihePortTypeFactory, "ihePortTypeFactory must not be null");
    }

    /**
     * Performs a Patient Discovery for the given Patient Demographics.
     *
     * @param patientDemographics the Patient Demographics set to be used in the request.
     * @param assertionMap        HCP identity assertion.
     * @param countryCode         country code - ISO 3166-1 alpha-2
     * @return a List of matching Patient Demographics, each representing a patient person.
     * @throws OpenNCPException contains the error message
     */
    public List<PatientDemographics> patientDiscovery(final PatientDemographics patientDemographics,
                                                      final Map<AssertionType, Assertion> assertionMap,
                                                      final String countryCode) throws OpenNCPException {


        String endpointUrl = null;
        try {
            endpointUrl = discoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.PATIENT_IDENTIFICATION_SERVICE);
        } catch (final ConfigurationManagerException e) {
            throw new OpenNCPException(OpenNCPErrorCode.ERROR_PI_NO_MATCH, e);
        }
        final RespondingGatewayPortType xcpdPortType = ihePortTypeFactory.createXCPDPort(configurationManager, endpointUrl);

        final String dstHomeCommunityId = OidUtil.getHomeCommunityId(countryCode.toLowerCase(Locale.ENGLISH));
        final PRPAIN201305UV02 xcpdRequest = XcpdMessageBuilder.build(patientDemographics, dstHomeCommunityId);
        final PRPAIN201306UV02 xcpdResponse = xcpdPortType.respondingGatewayPRPAIN201305UV02(xcpdRequest);

        return XcpdResponseExtractor.extract(xcpdResponse);
    }
}
