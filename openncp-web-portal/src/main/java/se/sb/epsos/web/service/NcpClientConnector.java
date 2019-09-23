package se.sb.epsos.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sb.epsos.shelob.ws.client.jaxws.ClientConnectorServiceService;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

public class NcpClientConnector {

    public static final Logger LOGGER = LoggerFactory.getLogger(NcpClientConnector.class);

    private NcpClientConnector() {
    }

    public static synchronized ClientConnectorServiceService createClientConnector() {

        String namespaceUrl = "http://cc.pt.epsos.eu";
        String clientConnectorWsdlUrl = System.getProperty("client-connector-wsdl-url");
        LOGGER.info("[Portal] Initializing client-connector-wsdl-url: '{}'", clientConnectorWsdlUrl);
        if (clientConnectorWsdlUrl == null) {
            throw new IllegalArgumentException("client-connector-wsdl-url is not set");
        }
        ClientConnectorServiceService service;
        try {
            service = new ClientConnectorServiceService(new URL(clientConnectorWsdlUrl), new QName(namespaceUrl, "ClientConnectorService"));
        } catch (MalformedURLException ex) {
            throw new RuntimeException("System property 'client-connector-wsdl-url' is malformed: " + clientConnectorWsdlUrl);
        }
        return service;
    }
}
