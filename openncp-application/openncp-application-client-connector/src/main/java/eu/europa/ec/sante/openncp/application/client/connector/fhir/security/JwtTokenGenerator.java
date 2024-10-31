package eu.europa.ec.sante.openncp.application.client.connector.fhir.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import eu.europa.ec.sante.openncp.common.security.JwtClaimType;
import eu.europa.ec.sante.openncp.core.client.api.AssertionEnum;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.apache.commons.lang.time.DateUtils;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.Base64;
import java.util.Date;
import java.util.Map;

public class JwtTokenGenerator {

    public static String generate(Map<AssertionEnum, Assertion> assertions) {

        Assertion hcpAssertion = assertions.get(AssertionEnum.CLINICIAN);
        Assertion trcAssertion = assertions.get(AssertionEnum.TREATMENT);
        Assertion nokAssertion = assertions.get(AssertionEnum.NEXT_OF_KIN);

        final String hcp = SerializeSupport.prettyPrintXML(hcpAssertion.getDOM());
        final Base64.Encoder encoder = Base64.getEncoder();

        JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject("user")
                .withExpiresAt(DateUtils.addHours(new Date(), 24))
                .withClaim(JwtClaimType.HCP.name(), encoder.encodeToString(hcp.getBytes()));

        if (trcAssertion != null) {
            final String trc = SerializeSupport.prettyPrintXML(trcAssertion.getDOM());
            jwtBuilder.withClaim(JwtClaimType.TRC.name(), encoder.encodeToString(trc.getBytes()));
        }

        if (nokAssertion != null) {
            final String nok = SerializeSupport.prettyPrintXML(nokAssertion.getDOM());
            jwtBuilder.withClaim(JwtClaimType.NOK.name(), encoder.encodeToString(nok.getBytes()));
        }
        return jwtBuilder.sign(Algorithm.HMAC512("cO8IGFVZa60tBFScaDotntDucC4FphuA"));
    }
}
