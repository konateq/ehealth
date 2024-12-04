package eu.europa.ec.sante.openncp.application.client.connector.fhir.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.apache.commons.lang.time.DateUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenGenerator {

    ConfigurationManager configurationManager;

    public JwtTokenGenerator(final ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }


    public String generate(final Map<AssertionType, Assertion> assertions) {

        Assertion hcpAssertion = assertions.get(AssertionType.HCP);
        Assertion trcAssertion = assertions.get(AssertionType.TRC);
        Assertion nokAssertion = assertions.get(AssertionType.NOK);

        final String hcp = SerializeSupport.prettyPrintXML(hcpAssertion.getDOM());
        final Base64.Encoder encoder = Base64.getEncoder();

        JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject("user")
                .withExpiresAt(DateUtils.addHours(new Date(), 24))
                .withClaim(AssertionType.HCP.name(), encoder.encodeToString(hcp.getBytes()));

        if (trcAssertion != null) {
            final String trc = SerializeSupport.prettyPrintXML(trcAssertion.getDOM());
            jwtBuilder.withClaim(AssertionType.TRC.name(), encoder.encodeToString(trc.getBytes()));
        }

        if (nokAssertion != null) {
            final String nok = SerializeSupport.prettyPrintXML(nokAssertion.getDOM());
            jwtBuilder.withClaim(AssertionType.NOK.name(), encoder.encodeToString(nok.getBytes()));
        }
        return jwtBuilder.sign(Algorithm.HMAC512(configurationManager.getProperty(Constant.JWT_SECRET)));
    }
}
