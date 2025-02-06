package eu.europa.ec.sante.openncp.core.client.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201306UV02;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * XCPD Initiating Gateway
 * This is an implementation of a IHE XCPD Initiation Gateway. This class provides the necessary operations to perform
 * PatientDiscovery.
 *
 */
@Service
public class XcpdInitGateway {


    /**
     * Performs a Patient Discovery for the given Patient Demographics.
     *
     * @param patientDemographics the Patient Demographics set to be used in the request.
     * @param assertionMap        HCP identity assertion.
     * @param countryCode         country code - ISO 3166-1 alpha-2
     * @return a List of matching Patient Demographics, each representing a patient person.
     * @throws NoPatientIdDiscoveredException contains the error message
     */
    public List<PatientDemographics> patientDiscovery(final PatientDemographics patientDemographics,
                                                             final Map<AssertionType, Assertion> assertionMap,
                                                             final String countryCode) throws NoPatientIdDiscoveredException {

        final PRPAIN201306UV02 response = RespondingGateway_RequestSender.respondingGateway_PRPA_IN201305UV02(patientDemographics, assertionMap, countryCode);
        return RespondingGateway_RequestReceiver.respondingGateway_PRPA_IN201306UV02(response);
    }
}
