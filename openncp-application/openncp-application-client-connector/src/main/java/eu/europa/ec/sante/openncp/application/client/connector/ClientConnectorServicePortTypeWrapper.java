package eu.europa.ec.sante.openncp.application.client.connector;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.ClientServicePortType;
import org.opensaml.saml.saml2.core.Assertion;

import javax.xml.ws.BindingProvider;
import java.util.Map;

public class ClientConnectorServicePortTypeWrapper {

    public static final String REQUESTCONTEXT_ASSERTIONS_KEY = "oncp.assertions";
    private final ClientServicePortType clientConnectorServicePortType;

    public ClientConnectorServicePortTypeWrapper(final ClientServicePortType clientServicePortType) {
        this.clientConnectorServicePortType = clientServicePortType;
    }

    public void setAssertions(final Map<AssertionType, Assertion> assertions) {
        getRequestContext().put(REQUESTCONTEXT_ASSERTIONS_KEY, assertions);
    }

    public ClientServicePortType getClientConnectorServicePortType() {
        return clientConnectorServicePortType;
    }

    public BindingProvider getBindingProvider() {
        return (BindingProvider) getClientConnectorServicePortType();
    }

    public Map<String, Object> getRequestContext() {
        return getBindingProvider().getRequestContext();
    }
}
