package eu.europa.ec.sante.openncp.core.client.ihe;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.audit.ssl.ImmutableKeystoreDetails;
import eu.europa.ec.sante.openncp.common.audit.ssl.KeystoreDetails;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.core.client.ihe.interceptors.OutboundSecurityInterceptor;
import eu.europa.ec.sante.openncp.core.common.SslContextBuilder;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xca.XCAService;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.XCPDService;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xdr.DocumentRecipientPortType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xdr.XDRService;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.xml.ws.BindingProvider;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

@Component
public class DefaultIhePortTypeFactory implements IhePortTypeFactory {

    private final XCPDService xcpdService = new XCPDService();
    private final XCAService xcaService = new XCAService();
    private final XDRService xdrService = new XDRService();

    private List<Interceptor<? extends Message>> defaultOutInterceptors() {
        return List.of(new OutboundSecurityInterceptor());
    }

    private List<Interceptor<? extends Message>> defaultInInterceptors() {
        return List.of();
    }

    private List<Feature> defaultFeatures() {
        return List.of(new WSAddressingFeature());
    }


    @Override
    public RespondingGatewayPortType createXCPDPort(final ConfigurationManager configManager, final String endpointUrl) {
        final RespondingGatewayPortType port = xcpdService.getRespondingGatewayPortSoap12();
        return preparePort(port, endpointUrl, configManager);
    }

    @Override
    public eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xca.RespondingGatewayPortType createXCAPort(final ConfigurationManager configManager, final String endpointUrl) {
        final eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xca.RespondingGatewayPortType port = xcaService.getRespondingGatewayPortSoap12();
        return preparePort(port, endpointUrl, configManager);
    }

    @Override
    public DocumentRecipientPortType createXDRPort(final ConfigurationManager configManager, final String endpointUrl) {
        final DocumentRecipientPortType port = xdrService.getDocumentRecipientPortSoap12();
        return preparePort(port, endpointUrl, configManager);
    }

    private <T> T preparePort(final T port, final String endpointUrl, final ConfigurationManager configManager) {
        final BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
        final Client client = ClientProxy.getClient(port);
        try {
            addConduit(client, configManager);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to configure TLS for port: " + port.getClass().getSimpleName(), e);
        }

        client.getOutInterceptors().addAll(defaultOutInterceptors());
        client.getInInterceptors().addAll(defaultInInterceptors());
        client.getBus().getFeatures().addAll(defaultFeatures());

        return port;
    }

    private static <PORTTYPE> PORTTYPE createService(final Class<PORTTYPE> portTypeClass,
                                                     final ConfigurationManager configurationManager,
                                                     final String endpointUrl,
                                                     final List<Interceptor<? extends Message>> inInterceptors,
                                                     final List<Interceptor<? extends Message>> outInterceptors,
                                                     final List<Feature> features) {

        final JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(portTypeClass);
        factory.setAddress(endpointUrl);
        factory.getInInterceptors().addAll(inInterceptors);
        factory.getOutInterceptors().addAll(outInterceptors);
        factory.getFeatures().addAll(features);

        final PORTTYPE port = factory.create(portTypeClass);

        final Client client = ClientProxy.getClient(port);
        try {
            addConduit(client, configurationManager);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to configure TLS for port: " + portTypeClass.getSimpleName(), e);
        }

        return port;
    }

    private static void addConduit(final Client client, final ConfigurationManager configurationManager) throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, KeyManagementException {
        final KeystoreDetails serviceConsumerKeystoreDetails = ImmutableKeystoreDetails.builder()
                .keystoreLocation(configurationManager.getProperty(Constant.SC_KEYSTORE_PATH))
                .keystorePassword(configurationManager.getProperty(Constant.SC_KEYSTORE_PASSWORD))
                .alias(configurationManager.getProperty(Constant.SC_PRIVATEKEY_ALIAS))
                .keyPassword(configurationManager.getProperty(Constant.SC_PRIVATEKEY_PASSWORD))
                .build();
        final KeystoreDetails trustStoreKeystoreDetails = ImmutableKeystoreDetails.builder()
                .keystoreLocation(configurationManager.getProperty(Constant.TRUSTSTORE_PATH))
                .keystorePassword(configurationManager.getProperty(Constant.TRUSTSTORE_PASSWORD))
                .build();
        final SSLContext sslContext = SslContextBuilder.build(serviceConsumerKeystoreDetails, trustStoreKeystoreDetails);

        final HTTPConduit conduit = (HTTPConduit) client.getConduit();
        final TLSClientParameters tlsClientParameters = new TLSClientParameters();

        if (StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())) {
            tlsClientParameters.setDisableCNCheck(false);
        } else {
            tlsClientParameters.setDisableCNCheck(true);
            tlsClientParameters.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }


        tlsClientParameters.setSSLSocketFactory(sslContext.getSocketFactory());
        conduit.setTlsClientParameters(tlsClientParameters);

    }
}
