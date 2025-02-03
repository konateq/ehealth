package eu.europa.ec.sante.openncp.application.client.connector.fhir.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.apache.commons.lang.time.DateUtils;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenGenerator {
    private final MarshallerFactory marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();

    private final ConfigurationManager configurationManager;

    public JwtTokenGenerator(final ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }


    public String generate(final Map<AssertionType, Assertion> assertions) {

        final Assertion hcpAssertion = assertions.get(AssertionType.HCP);
        final Assertion trcAssertion = assertions.get(AssertionType.TRC);
        final Assertion nokAssertion = assertions.get(AssertionType.NOK);

        final String hcp = serializeAssertionToXML(hcpAssertion);
        final Base64.Encoder encoder = Base64.getEncoder();

        final JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject("user")
                .withExpiresAt(DateUtils.addHours(new Date(), 1))
                .withClaim(AssertionType.HCP.name(), encoder.encodeToString(hcp.getBytes()));

        if (trcAssertion != null) {
            final String trc = serializeAssertionToXML(trcAssertion);
            jwtBuilder.withClaim(AssertionType.TRC.name(), encoder.encodeToString(trc.getBytes()));
        }

        if (nokAssertion != null) {
            final String nok = serializeAssertionToXML(nokAssertion);
            jwtBuilder.withClaim(AssertionType.NOK.name(), encoder.encodeToString(nok.getBytes()));
        }
        return jwtBuilder.sign(Algorithm.HMAC512(configurationManager.getProperty(Constant.JWT_SECRET)));
    }

    public String serializeAssertionToXML(final Assertion assertion) {
        final Marshaller marshaller = this.marshallerFactory.getMarshaller(assertion);

        // Marshall the assertion into a DOM Element
        final Element element;
        try {
            element = marshaller.marshall(assertion);
        } catch (final MarshallingException e) {
            throw new RuntimeException("Exception when marshalling the assertion", e);
        }

        return SerializeSupport.nodeToString(element);
    }
}
