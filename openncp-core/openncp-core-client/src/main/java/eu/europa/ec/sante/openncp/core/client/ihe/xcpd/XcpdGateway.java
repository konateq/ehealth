package eu.europa.ec.sante.openncp.core.client.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerException;
import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.ihe.xcpd.generated.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.client.ihe.xcpd.generated.PRPAIN201306UV02;
import eu.europa.ec.sante.openncp.core.client.ihe.xcpd.generated.RespondingGatewayPortType;
import eu.europa.ec.sante.openncp.core.client.ihe.xcpd.generated.XCPDService;
import eu.europa.ec.sante.openncp.core.common.HttpsClientConfiguration;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;
import eu.europa.ec.sante.openncp.core.common.util.OidUtil;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.BindingProvider;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
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
    private final RespondingGatewayPortType xcpdPort;

    public XcpdGateway(final DynamicDiscoveryService discoveryService, final ConfigurationManager configurationManager) throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        this.discoveryService = Validate.notNull(discoveryService, " discoveryService must not be null");
        this.configurationManager = Validate.notNull(configurationManager, "configurationManager must not be null");
        final XCPDService xcpdService = new XCPDService();
        xcpdPort = xcpdService.getRespondingGatewayPortSoap12();

        final Client client = ClientProxy.getClient(xcpdPort);
        client.getBus().getFeatures().add(new WSAddressingFeature());

        final HTTPConduit conduit = (HTTPConduit) client.getConduit();

        final TLSClientParameters tlsClientParameters = new TLSClientParameters();
        // This should be configurable, you don't want to disable the CN check in production!!
        tlsClientParameters.setDisableCNCheck(true);
        tlsClientParameters.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        tlsClientParameters.setSSLSocketFactory(HttpsClientConfiguration.buildSSLContext().getSocketFactory());

        conduit.setTlsClientParameters(tlsClientParameters);
    }

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


        String endpointUrl = null;
        try {
            endpointUrl = discoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.PATIENT_IDENTIFICATION_SERVICE);
        } catch (final ConfigurationManagerException e) {
            throw new NoPatientIdDiscoveredException(OpenNCPErrorCode.ERROR_PI_NO_MATCH, e);
        }

        // Override the endpoint address dynamically.
        final BindingProvider bindingProvider = (BindingProvider) xcpdPort;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        final String dstHomeCommunityId = OidUtil.getHomeCommunityId(countryCode.toLowerCase(Locale.ENGLISH));
        final PRPAIN201305UV02 xcpdRequest = XcpdMessageBuilder.build(patientDemographics, dstHomeCommunityId);
        final PRPAIN201306UV02 xcpdResponse = xcpdPort.respondingGatewayPRPAIN201305UV02(xcpdRequest);

        return XcpdResponseExtractor.extract(xcpdResponse);
    }
}
